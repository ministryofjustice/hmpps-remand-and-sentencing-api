package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import kotlinx.coroutines.runBlocking
import org.awaitility.core.ConditionTimeoutException
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.slf4j.LoggerFactory
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
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateRecall
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateSentence
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateUploadedDocument
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.DeleteRecallResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.DraftCourtAppearanceCreatedResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.DraftCourtCaseCreatedResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.DraftCreateCourtAppearance
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.DraftCreateCourtCase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.Recall
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.SaveRecallResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.UploadedDocument
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.event.HmppsMessage
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.util.DataCreator
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.wiremock.DocumentManagementApiExtension
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.wiremock.OAuthExtension
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.wiremock.PrisonApiExtension
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.CourtAppearanceRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.CourtCaseRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.PeriodLengthRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.SentenceRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.UploadedDocumentRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.audit.AppearanceChargeHistoryRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.audit.CourtAppearanceHistoryRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.audit.CourtCaseHistoryRepository
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
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.MigrationCreateCourtCases
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.MigrationCreateCourtCasesResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.DpsDataCreator
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.DraftDataCreator
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.numberOfMessagesCurrentlyOnQueue
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.MissingQueueException
import uk.gov.justice.hmpps.sqs.PurgeQueueRequest
import uk.gov.justice.hmpps.sqs.countAllMessagesOnQueue
import uk.gov.justice.hmpps.test.kotlin.auth.JwtAuthorisationHelper
import java.util.*

@Sql("classpath:test_data/reset-database.sql")
@ExtendWith(OAuthExtension::class, PrisonApiExtension::class, DocumentManagementApiExtension::class)
@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
abstract class IntegrationTestBase {

  @Suppress("SpringJavaInjectionPointsAutowiringInspection")
  @Autowired
  lateinit var webTestClient: WebTestClient

  @Autowired
  protected lateinit var jwtAuthHelper: JwtAuthorisationHelper

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

  private val prisonerListenerQueue by lazy {
    hmppsQueueService.findByQueueId("prisonerlistener")
      ?: throw MissingQueueException("HmppsQueue prisonerlistener not found")
  }

  private val prisonerListenerQueueSqsClient by lazy { hmppsDomainQueue.sqsClient }
  private val prisonerListenerQueueSqsDlqClient by lazy { hmppsDomainQueue.sqsDlqClient!! }

  @Autowired
  protected lateinit var courtAppearanceHistoryRepository: CourtAppearanceHistoryRepository

  @Autowired
  protected lateinit var appearanceChargeHistoryRepository: AppearanceChargeHistoryRepository

  @Autowired
  protected lateinit var courtAppearanceRepository: CourtAppearanceRepository

  @Autowired
  protected lateinit var sentenceRepository: SentenceRepository

  @Autowired
  protected lateinit var courtCaseRepository: CourtCaseRepository

  @Autowired
  protected lateinit var uploadedDocumentRepository: UploadedDocumentRepository

  @Autowired
  protected lateinit var courtCaseHistoryRepository: CourtCaseHistoryRepository

  @Autowired
  protected lateinit var periodLengthRepository: PeriodLengthRepository

  @BeforeEach
  fun clearDependencies() {
    purgeQueues()
  }

  internal fun HttpHeaders.authToken(roles: List<String> = emptyList(), user: String = "SOME_USER") {
    this.setBearerAuth(
      jwtAuthHelper.createJwtAccessToken(
        roles = roles,
        clientId = "some-client",
        username = user,
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
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
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
    createAppearance: Boolean = true,
  ): Pair<UUID, LegacyCreateCharge> {
    var toCreateCharge = legacyCharge
    if (createAppearance) {
      val courtAppearance = createLegacyCourtAppearance(legacyCreateCourtCase, legacyCreateCourtAppearance)
      toCreateCharge = legacyCharge.copy(appearanceLifetimeUuid = courtAppearance.first)
    }
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
    val (chargeLifetimeUuid, toCreateLegacyCharge) = createLegacyCharge(legacyCreateCourtCase, legacyCreateCourtAppearance, legacyCharge)
    val toCreateSentence = legacySentence.copy(chargeUuids = listOf(chargeLifetimeUuid), appearanceUuid = toCreateLegacyCharge.appearanceLifetimeUuid)
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

  protected fun addBookingIdToDpsSentence(
    sentenceUuid: UUID,
    chargeUuid: UUID,
    appearanceUuid: UUID,
  ): Long {
    val toUpdateSentence = DataCreator.legacyCreateSentence(chargeUuids = listOf(chargeUuid), appearanceUuid = appearanceUuid, fine = null)

    webTestClient
      .put()
      .uri("/legacy/sentence/$sentenceUuid")
      .bodyValue(toUpdateSentence)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_SENTENCE_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isNoContent
    return toUpdateSentence.legacyData.bookingId!!
  }

  protected fun createLegacySentenceWithManyCharges(
    legacyCreateCourtCase: LegacyCreateCourtCase = DataCreator.legacyCreateCourtCase(),
    legacyCreateCourtAppearance: LegacyCreateCourtAppearance = DataCreator.legacyCreateCourtAppearance(
      legacyData = DataCreator.courtAppearanceLegacyData(
        outcomeConvictionFlag = true,
        outcomeDispositionCode = "F",
      ),
    ),
    firstLegacyCharge: LegacyCreateCharge = DataCreator.legacyCreateCharge(),
    secondLegacyCharge: LegacyCreateCharge = DataCreator.legacyCreateCharge(),
    legacySentence: LegacyCreateSentence =
      DataCreator.legacyCreateSentence(),
  ): Pair<UUID, LegacyCreateSentence> {
    val (chargeUuidOne, toCreateLegacyCharge) = createLegacyCharge(legacyCreateCourtCase, legacyCreateCourtAppearance, firstLegacyCharge)
    val toCreateSecond = secondLegacyCharge.copy(appearanceLifetimeUuid = toCreateLegacyCharge.appearanceLifetimeUuid)
    val (chargeUuidTwo) = createLegacyCharge(legacyCreateCourtCase, legacyCreateCourtAppearance, toCreateSecond, false)
    val toCreateSentence = legacySentence.copy(chargeUuids = listOf(chargeUuidOne, chargeUuidTwo), appearanceUuid = toCreateLegacyCharge.appearanceLifetimeUuid)
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
    draftCourtCase: DraftCreateCourtCase = DraftDataCreator.draftCreateCourtCase(),
  ): DraftCourtCaseCreatedResponse = webTestClient
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

  protected fun getRecallByUUID(recallUuid: UUID): Recall = webTestClient
    .get()
    .uri("/recall/$recallUuid")
    .headers {
      it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING"))
    }
    .exchange()
    .expectStatus()
    .isOk
    .expectBody(Recall::class.java)
    .returnResult().responseBody!!

  protected fun createRecall(recall: CreateRecall) = webTestClient
    .post()
    .uri("/recall")
    .bodyValue(recall)
    .headers {
      it.authToken(roles = listOf("ROLE_REMAND_SENTENCING__RECORD_RECALL_RW"))
      it.contentType = MediaType.APPLICATION_JSON
    }
    .exchange()
    .expectStatus()
    .isCreated
    .expectBody(SaveRecallResponse::class.java)
    .returnResult().responseBody!!

  protected fun updateRecall(recall: CreateRecall, uuid: UUID) = webTestClient
    .put()
    .uri("/recall/$uuid")
    .bodyValue(recall)
    .headers {
      it.authToken(roles = listOf("ROLE_REMAND_SENTENCING__RECORD_RECALL_RW"))
      it.contentType = MediaType.APPLICATION_JSON
    }
    .exchange()
    .expectStatus()
    .isOk
    .expectBody(SaveRecallResponse::class.java)
    .returnResult().responseBody!!

  protected fun deleteRecall(uuid: UUID) = webTestClient
    .delete()
    .uri("/recall/$uuid")
    .headers {
      it.authToken(roles = listOf("ROLE_REMAND_SENTENCING__RECORD_RECALL_RW"))
      it.contentType = MediaType.APPLICATION_JSON
    }
    .exchange()
    .expectStatus()
    .isOk
    .expectBody(DeleteRecallResponse::class.java)
    .returnResult().responseBody!!

  fun purgeQueues() {
    val totalAttempts = 5
    var currentAttempt = 0
    while (currentAttempt < totalAttempts && hmppsDomainQueueSqsClient.countAllMessagesOnQueue(hmppsDomainQueue.queueUrl).get() != 0) {
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
        hmppsQueueService.purgeQueue(
          PurgeQueueRequest(
            "prisonerlistener-queue",
            prisonerListenerQueueSqsClient,
            prisonerListenerQueue.queueUrl,
          ),
        )
        hmppsQueueService.purgeQueue(
          PurgeQueueRequest(
            "prisonerlistener-dlq",
            prisonerListenerQueueSqsDlqClient,
            prisonerListenerQueue.dlqUrl!!,
          ),
        )
      }
      currentAttempt++
    }
    val messagesOnQueue = getAllDomainMessages()
    log.info("message types on queue: {}", messagesOnQueue.joinToString { it.eventType })
    val prisonerMessagesOnQueue = getAllPrisonerMessages()
    log.info("messages on queue: {}", prisonerMessagesOnQueue.joinToString { it.eventType })
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
    try {
      numberOfMessagesCurrentlyOnQueue(hmppsDomainQueueSqsClient, hmppsDomainQueue.queueUrl, expectedNumberOfMessages)
    } catch (e: ConditionTimeoutException) {
      val messagesOnQueue = getAllDomainMessages()
      log.info("message types on queue: {}", messagesOnQueue.joinToString { it.eventType })
      throw e
    }
    return getAllDomainMessages()
  }

  fun createCourtCaseTwoSentences(): Pair<CreateSentence, CreateSentence> {
    val firstCharge = DpsDataCreator.dpsCreateCharge(sentence = DpsDataCreator.dpsCreateSentence())
    val secondCharge = DpsDataCreator.dpsCreateCharge(sentence = DpsDataCreator.dpsCreateSentence())
    val appearance = DpsDataCreator.dpsCreateCourtAppearance(charges = listOf(firstCharge, secondCharge))
    val (_, courtCase) = createCourtCase(DpsDataCreator.dpsCreateCourtCase(appearances = listOf(appearance)))
    val sentenceOne = courtCase.appearances.first().charges.first().sentence!!
    val sentenceTwo = courtCase.appearances.first().charges[1].sentence!!
    return sentenceOne to sentenceTwo
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

  private fun getAllPrisonerMessages(): List<HmppsMessage<ObjectNode>> {
    val messages = ArrayList<HmppsMessage<ObjectNode>>()
    while (prisonerListenerQueueSqsClient.countAllMessagesOnQueue(prisonerListenerQueue.queueUrl).get() != 0) {
      val message = prisonerListenerQueueSqsClient.receiveMessage(
        ReceiveMessageRequest.builder().queueUrl(prisonerListenerQueue.queueUrl).build(),
      )
      messages.addAll(
        message.get().messages().map {
          prisonerListenerQueueSqsClient.deleteMessage(
            DeleteMessageRequest.builder().queueUrl(prisonerListenerQueue.queueUrl).receiptHandle(it.receiptHandle())
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

  protected fun legacyUpdateSentence(sentenceUuid: UUID, sentence: LegacyCreateSentence) {
    webTestClient
      .put()
      .uri("/legacy/sentence/$sentenceUuid")
      .bodyValue(sentence)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_SENTENCE_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus().isNoContent
  }

  protected fun legacyCreatePeriodLength(periodLength: LegacyCreatePeriodLength) {
    webTestClient
      .post()
      .uri("/legacy/period-length")
      .bodyValue(periodLength)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_PERIOD_LENGTH_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus().isCreated
  }

  protected fun legacyCreateSentence(legacySentence: LegacyCreateSentence): LegacySentenceCreatedResponse = webTestClient
    .post()
    .uri("/legacy/sentence")
    .bodyValue(legacySentence)
    .headers {
      it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_SENTENCE_RW"))
      it.contentType = MediaType.APPLICATION_JSON
    }
    .exchange()
    .expectStatus()
    .isCreated.returnResult(LegacySentenceCreatedResponse::class.java)
    .responseBody.blockFirst()!!

  protected fun uploadDocument(document: UploadedDocument) {
    webTestClient.post()
      .uri("/uploaded-documents")
      .bodyValue(CreateUploadedDocument(appearanceUUID = null, documents = listOf(document)))
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isCreated
  }

  protected fun migrateCases(courtCases: MigrationCreateCourtCases) = webTestClient
    .post()
    .uri("/legacy/court-case/migration")
    .bodyValue(courtCases)
    .headers {
      it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_COURT_CASE_RW"))
      it.contentType = MediaType.APPLICATION_JSON
    }
    .exchange()
    .expectStatus()
    .isCreated
    .returnResult(MigrationCreateCourtCasesResponse::class.java)
    .responseBody.blockFirst()!!

  protected fun linkCases(sourceCourtCaseUuid: String, targetCourtCaseUuid: String) = webTestClient
    .put()
    .uri("/legacy/court-case/$sourceCourtCaseUuid/link/$targetCourtCaseUuid")
    .headers {
      it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_COURT_CASE_RW"))
      it.contentType = MediaType.APPLICATION_JSON
    }
    .exchange()
    .expectStatus()
    .isNoContent

  protected fun uuid(i: Long) = UUID(0L, i)

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}

data class SQSMessage(
  val Message: String,
  val MessageId: String,
)
