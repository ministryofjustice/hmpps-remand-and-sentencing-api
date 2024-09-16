package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.reactive.server.WebTestClient
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateCharge
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateCourtAppearance
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateCourtCase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateCourtCaseResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateNextCourtAppearance
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreatePeriodLength
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateSentence
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.event.HmppsCourtCaseMessage
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.event.HmppsMessage
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.wiremock.DocumentManagementApiExtension
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.wiremock.OAuthExtension
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.wiremock.PrisonApiExtension
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.numberOfMessagesCurrentlyOnQueue
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.MissingQueueException
import uk.gov.justice.hmpps.sqs.PurgeQueueRequest
import uk.gov.justice.hmpps.sqs.countAllMessagesOnQueue
import java.time.LocalDate
import java.time.LocalTime
import java.util.ArrayList
import java.util.UUID

@Sql("classpath:test_data/reset-database.sql")
@ExtendWith(OAuthExtension::class, PrisonApiExtension::class, DocumentManagementApiExtension::class)
@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
abstract class IntegrationTestBase {

  @Suppress("SpringJavaInjectionPointsAutowiringInspection")
  @Autowired
  lateinit var webTestClient: WebTestClient

  @Autowired
  protected lateinit var jwtAuthHelper: JwtAuthHelper

  @Autowired
  protected lateinit var hmppsQueueService: HmppsQueueService

  @Autowired
  private lateinit var objectMapper: ObjectMapper

  private val hmppsDomainQueue by lazy {
    hmppsQueueService.findByQueueId("hmppsdomainqueue") ?: throw MissingQueueException("HmppsQueue hmppsdomainqueue not found")
  }

  private val hmppsDomainQueueSqsClient by lazy { hmppsDomainQueue.sqsClient }
  private val hmppsDomainQueueSqsDlqClient by lazy { hmppsDomainQueue.sqsDlqClient!! }

  @BeforeEach
  fun clearDependencies() {
    runBlocking {
      hmppsQueueService.purgeQueue(PurgeQueueRequest("hmpps_domain_queue", hmppsDomainQueueSqsClient, hmppsDomainQueue.queueUrl))
      hmppsQueueService.purgeQueue(PurgeQueueRequest("hmpps_domain_dlq", hmppsDomainQueueSqsDlqClient, hmppsDomainQueue.dlqUrl!!))
    }
  }

  internal fun HttpHeaders.authToken(roles: List<String> = emptyList()) {
    this.setBearerAuth(
      jwtAuthHelper.createJwt(
        subject = "SOME_USER",
        roles = roles,
        client = "some-client",
        user = "SOME_USER",
      ),
    )
  }

  protected fun createCourtCase(prisonerId: String = "PRI123", minusDaysFromAppearanceDate: Long = 0): Pair<String, CreateCourtCase> {
    val sentence = CreateSentence(UUID.randomUUID(), "1", CreatePeriodLength(1, null, null, null, periodOrder = "years"), null, "FORTHWITH", null, UUID.fromString("1104e683-5467-4340-b961-ff53672c4f39"), LocalDate.now().minusDays(7))
    val charge = CreateCharge(UUID.randomUUID(), "OFF123", LocalDate.now(), null, "OUT123", true, sentence)
    val appearance = CreateCourtAppearance(
      null, UUID.randomUUID(), "OUT123", "COURT1", "GH123456789", LocalDate.now().minusDays(minusDaysFromAppearanceDate), "123", "REMAND", 1,
      CreatePeriodLength(1, null, null, null, periodOrder = "years"),
      CreateNextCourtAppearance(
        LocalDate.now(),
        LocalTime.now(),
        "COURT1",
        "Court Appearance",
      ),
      listOf(charge),
      LocalDate.now().minusDays(7),
    )
    val courtCase = CreateCourtCase(prisonerId, listOf(appearance))
    val response = webTestClient
      .post()
      .uri("/court-case")
      .bodyValue(courtCase)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isCreated.returnResult(CreateCourtCaseResponse::class.java)
      .responseBody.blockFirst()!!
    return response.courtCaseUuid to courtCase
  }

  fun expectCourtCaseInsertedMessage(prisonerId: String) {
    numberOfMessagesCurrentlyOnQueue(hmppsDomainQueueSqsClient, hmppsDomainQueue.queueUrl, 1)
    val messages = getAllDomainMessages()
    Assertions.assertEquals(1, messages.size)
    val courtCaseInsertedMessage = messages.first()
    Assertions.assertEquals(prisonerId, courtCaseInsertedMessage.personReference.identifiers.first { it.type == "NOMS" }.value)
    Assertions.assertEquals("DPS", courtCaseInsertedMessage.additionalInformation.source)
  }

  private fun getAllDomainMessages(): List<HmppsMessage<HmppsCourtCaseMessage>> {
    val messages = ArrayList<HmppsMessage<HmppsCourtCaseMessage>>()
    while (hmppsDomainQueueSqsClient.countAllMessagesOnQueue(hmppsDomainQueue.queueUrl).get() != 0) {
      val message = hmppsDomainQueueSqsClient.receiveMessage(ReceiveMessageRequest.builder().queueUrl(hmppsDomainQueue.queueUrl).build())
      messages.addAll(
        message.get().messages().map {
          hmppsDomainQueueSqsClient.deleteMessage(DeleteMessageRequest.builder().queueUrl(hmppsDomainQueue.queueUrl).receiptHandle(it.receiptHandle()).build()).get()
          val sqsMessage = objectMapper.readValue(it.body(), SQSMessage::class.java)
          val courtCaseInsertedMessageType = object : TypeReference<HmppsMessage<HmppsCourtCaseMessage>>() {}
          objectMapper.readValue(sqsMessage.Message, courtCaseInsertedMessageType)
        },
      )
    }
    return messages
  }
}

data class SQSMessage(
  val Message: String,
  val MessageId: String,
)
