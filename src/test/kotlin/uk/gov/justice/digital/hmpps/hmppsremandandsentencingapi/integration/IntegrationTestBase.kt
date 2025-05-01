package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
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
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateCourtCase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateCourtCaseResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.DraftCourtAppearanceCreatedResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.DraftCourtCaseCreatedResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.DraftCreateCourtAppearance
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.DraftCreateCourtCase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.Recall
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.event.HmppsMessage
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.util.DataCreator
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.wiremock.DocumentManagementApiExtension
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.wiremock.OAuthExtension
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.wiremock.PrisonApiExtension
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.CourtAppearanceRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.audit.AppearanceChargeHistoryRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.audit.CourtAppearanceHistoryRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacyChargeCreatedResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacyCourtAppearanceCreatedResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacyCourtCaseCreatedResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacyCreateCharge
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacyCreateCourtAppearance
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacyCreateCourtCase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacyCreatePeriodLength
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacyCreateSentence
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacyPeriodLengthCreatedResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacySentenceCreatedResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.DpsDataCreator
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.DraftDataCreator
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.numberOfMessagesCurrentlyOnQueue
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.MissingQueueException
import uk.gov.justice.hmpps.sqs.PurgeQueueRequest
import uk.gov.justice.hmpps.sqs.countAllMessagesOnQueue
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
  protected lateinit var objectMapper: ObjectMapper

  private val hmppsDomainQueue by lazy {
    hmppsQueueService.findByQueueId("hmppsdomainqueue")
      ?: throw MissingQueueException("HmppsQueue hmppsdomainqueue not found")
  }

  private val hmppsDomainQueueSqsClient by lazy { hmppsDomainQueue.sqsClient }
  private val hmppsDomainQueueSqsDlqClient by lazy { hmppsDomainQueue.sqsDlqClient!! }

  @Autowired
  protected lateinit var courtAppearanceHistoryRepository: CourtAppearanceHistoryRepository

  @Autowired
  protected lateinit var appearanceChargeHistoryRepository: AppearanceChargeHistoryRepository

  @Autowired
  protected lateinit var courtAppearanceRepository: CourtAppearanceRepository

  @BeforeEach
  fun clearDependencies() {
    purgeQueues()
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

  protected fun createCourtCase(
    createCourtCase: CreateCourtCase = DpsDataCreator.dpsCreateCourtCase(),
    purgeQueues: Boolean = true,
  ): Pair<String, CreateCourtCase> {
    val response = webTestClient
      .post()
      .uri("/court-case")
      .bodyValue(createCourtCase)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isCreated.returnResult(CreateCourtCaseResponse::class.java)
      .responseBody.blockFirst()!!
    if (purgeQueues) {
      purgeQueues()
    }
    return response.courtCaseUuid to createCourtCase
  }

  protected fun createLegacyCourtCase(legacyCreateCourtCase: LegacyCreateCourtCase = DataCreator.legacyCreateCourtCase()): Pair<String, LegacyCreateCourtCase> {
    val response = webTestClient
      .post()
      .uri("/legacy/court-case")
      .bodyValue(legacyCreateCourtCase)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_COURT_CASE_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isCreated.returnResult(LegacyCourtCaseCreatedResponse::class.java)
      .responseBody.blockFirst()!!
    purgeQueues()
    return response.courtCaseUuid to legacyCreateCourtCase
  }

  protected fun createLegacyCourtAppearance(
    legacyCreateCourtCase: LegacyCreateCourtCase = DataCreator.legacyCreateCourtCase(),
    legacyCreateCourtAppearance: LegacyCreateCourtAppearance = DataCreator.legacyCreateCourtAppearance(),
  ): Pair<UUID, LegacyCreateCourtAppearance> {
    val courtCase = createLegacyCourtCase(legacyCreateCourtCase)
    val toCreateAppearance = legacyCreateCourtAppearance.copy(courtCaseUuid = courtCase.first)
    val response = webTestClient
      .post()
      .uri("/legacy/court-appearance")
      .bodyValue(toCreateAppearance)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_APPEARANCE_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isCreated.returnResult(LegacyCourtAppearanceCreatedResponse::class.java)
      .responseBody.blockFirst()!!
    purgeQueues()
    return response.lifetimeUuid to toCreateAppearance
  }

  protected fun createLegacyCharge(
    legacyCreateCourtCase: LegacyCreateCourtCase = DataCreator.legacyCreateCourtCase(),
    legacyCreateCourtAppearance: LegacyCreateCourtAppearance = DataCreator.legacyCreateCourtAppearance(),
    legacyCharge: LegacyCreateCharge = DataCreator.legacyCreateCharge(),
  ): Pair<UUID, LegacyCreateCharge> {
    val courtAppearance = createLegacyCourtAppearance(legacyCreateCourtCase, legacyCreateCourtAppearance)
    val toCreateCharge = legacyCharge.copy(appearanceLifetimeUuid = courtAppearance.first)
    val response = webTestClient
      .post()
      .uri("/legacy/charge")
      .bodyValue(toCreateCharge)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_CHARGE_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isCreated.returnResult(LegacyChargeCreatedResponse::class.java)
      .responseBody.blockFirst()!!
    purgeQueues()
    return response.lifetimeUuid to toCreateCharge
  }

  protected fun createLegacySentence(
    legacyCreateCourtCase: LegacyCreateCourtCase = DataCreator.legacyCreateCourtCase(),
    legacyCreateCourtAppearance: LegacyCreateCourtAppearance = DataCreator.legacyCreateCourtAppearance(
      legacyData = DataCreator.courtAppearanceLegacyData(
        outcomeConvictionFlag = true,
        outcomeDispositionCode = "F",
      ),
    ),
    legacyCharge: LegacyCreateCharge = DataCreator.legacyCreateCharge(),
    legacySentence: LegacyCreateSentence =
      DataCreator.legacyCreateSentence(),
  ): Pair<UUID, LegacyCreateSentence> {
    val (chargeLifetimeUuid) = createLegacyCharge(legacyCreateCourtCase, legacyCreateCourtAppearance, legacyCharge)
    val toCreateSentence = legacySentence.copy(chargeUuids = listOf(chargeLifetimeUuid))
    val response = webTestClient
      .post()
      .uri("/legacy/sentence")
      .bodyValue(toCreateSentence)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_SENTENCE_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isCreated.returnResult(LegacySentenceCreatedResponse::class.java)
      .responseBody.blockFirst()!!
    purgeQueues()
    return response.lifetimeUuid to toCreateSentence
  }

  protected fun createPeriodLength(
    legacyCreateCourtCase: LegacyCreateCourtCase = DataCreator.legacyCreateCourtCase(),
    legacyCreateCourtAppearance: LegacyCreateCourtAppearance = DataCreator.legacyCreateCourtAppearance(
      legacyData = DataCreator.courtAppearanceLegacyData(
        outcomeConvictionFlag = true,
        outcomeDispositionCode = "F",
      ),
    ),
    legacyPeriodLength: LegacyCreatePeriodLength = DataCreator.legacyCreatePeriodLength(),
  ): Pair<UUID, LegacyCreatePeriodLength> {
    val (sentenceLifetimeUuid) = createLegacySentence(legacyCreateCourtCase, legacyCreateCourtAppearance)
    val toCreatePeriodLength = legacyPeriodLength.copy(sentenceUuid = sentenceLifetimeUuid)
    val response = webTestClient
      .post()
      .uri("/legacy/period-length")
      .bodyValue(toCreatePeriodLength)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_PERIOD_LENGTH_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isCreated.returnResult(LegacyPeriodLengthCreatedResponse::class.java)
      .responseBody.blockFirst()!!
    purgeQueues()
    return response.periodLengthUuid to toCreatePeriodLength
  }

  protected fun createDraftCourtCase(
    draftCourtCase: DraftCreateCourtCase = DraftDataCreator.draftCreateCourtCase()):
    DraftCourtCaseCreatedResponse = webTestClient
    .post()
    .uri("/draft/court-case")
    .bodyValue(draftCourtCase)
    .headers {
      it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_REMAND_AND_SENTENCING_UI"))
      it.contentType = MediaType.APPLICATION_JSON
    }
    .exchange()
    .expectStatus()
    .isCreated.returnResult(DraftCourtCaseCreatedResponse::class.java)
    .responseBody.blockFirst()!!

  protected fun createDraftAppearance(
    courtCaseUuid: String,
    draftAppearance: DraftCreateCourtAppearance = DraftDataCreator.draftCreateCourtAppearance(),
  ): DraftCourtAppearanceCreatedResponse = webTestClient
    .post()
    .uri("/draft/court-case/$courtCaseUuid/appearance")
    .bodyValue(draftAppearance)
    .headers {
      it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_REMAND_AND_SENTENCING_UI"))
      it.contentType = MediaType.APPLICATION_JSON
    }
    .exchange()
    .expectStatus()
    .isCreated.returnResult(DraftCourtAppearanceCreatedResponse::class.java)
    .responseBody.blockFirst()!!

  protected fun getRecallsByPrisonerId(prisonerId: String): List<Recall> = webTestClient
    .get()
    .uri("/recall/person/$prisonerId")
    .headers {
      it.authToken(roles = listOf("ROLE_REMAND_SENTENCING__RECORD_RECALL_RW"))
    }
    .exchange()
    .expectStatus()
    .isOk
    .expectBodyList(Recall::class.java)
    .returnResult().responseBody!!

  fun purgeQueues() {
    runBlocking {
      hmppsQueueService.purgeQueue(
        PurgeQueueRequest(
          "hmpps_domain_queue",
          hmppsDomainQueueSqsClient,
          hmppsDomainQueue.queueUrl,
        ),
      )
      hmppsQueueService.purgeQueue(
        PurgeQueueRequest(
          "hmpps_domain_dlq",
          hmppsDomainQueueSqsDlqClient,
          hmppsDomainQueue.dlqUrl!!,
        ),
      )
    }
  }

  fun expectInsertedMessages(prisonerId: String) {
    numberOfMessagesCurrentlyOnQueue(hmppsDomainQueueSqsClient, hmppsDomainQueue.queueUrl, 7)
    val messages = getAllDomainMessages()
    Assertions.assertEquals(7, messages.size)
    messages.forEach { message ->
      Assertions.assertEquals(prisonerId, message.personReference.identifiers.first { it.type == "NOMS" }.value)
      Assertions.assertEquals("DPS", message.additionalInformation.get("source").asText())
    }
  }

  fun getMessages(expectedNumberOfMessages: Int): List<HmppsMessage<ObjectNode>> {
    numberOfMessagesCurrentlyOnQueue(hmppsDomainQueueSqsClient, hmppsDomainQueue.queueUrl, expectedNumberOfMessages)
    return getAllDomainMessages()
  }

  private fun getAllDomainMessages(): List<HmppsMessage<ObjectNode>> {
    val messages = ArrayList<HmppsMessage<ObjectNode>>()
    while (hmppsDomainQueueSqsClient.countAllMessagesOnQueue(hmppsDomainQueue.queueUrl).get() != 0) {
      val message = hmppsDomainQueueSqsClient.receiveMessage(
        ReceiveMessageRequest.builder().queueUrl(hmppsDomainQueue.queueUrl).build(),
      )
      messages.addAll(
        message.get().messages().map {
          hmppsDomainQueueSqsClient.deleteMessage(
            DeleteMessageRequest.builder().queueUrl(hmppsDomainQueue.queueUrl).receiptHandle(it.receiptHandle())
              .build(),
          ).get()
          val sqsMessage = objectMapper.readValue(it.body(), SQSMessage::class.java)
          val courtCaseInsertedMessageType = object : TypeReference<HmppsMessage<ObjectNode>>() {}
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
