package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.recall

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.expectBody
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.dto.AdjustmentDto
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.dto.UnlawfullyAtLargeDto
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateRecall
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.IsRecallPossible
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.IsRecallPossibleRequest
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.PeriodLength
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.recall.Recall
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.recall.RecallCourtCaseDetails
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.recall.RecallUALAdjustment
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.recall.RecalledSentence
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.event.EventSource
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.util.DataCreator
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.wiremock.AdjustmentsApiExtension.Companion.adjustmentsApi
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.PeriodLengthType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.RecallEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.RecallType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.RecallType.CUR_HDC
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.RecallType.FTR_14
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.RecallType.FTR_28
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.RecallType.FTR_56
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.RecallType.FTR_HDC_14
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.RecallType.FTR_HDC_28
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.RecallType.IN_HDC
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.RecallType.LR
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.SentenceTypeClassification
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.SentenceTypeRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacySentence
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacySentenceCreatedResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.DpsDataCreator
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.*
import java.util.stream.Stream

class RecallIntTests : IntegrationTestBase() {

  @Autowired
  private lateinit var sentenceTypeRepository: SentenceTypeRepository

  @BeforeEach
  fun setUp() {
    adjustmentsApi.stubAllowCreateAdjustments()
    adjustmentsApi.stubGetAdjustmentsDefaultToNone()
  }

  @Test
  fun `Create recall with UAL and fetch it based on returned UUID`() {
    val recall = CreateRecall(
      prisonerId = "A12345B",
      revocationDate = LocalDate.of(2024, 1, 2),
      returnToCustodyDate = LocalDate.of(2024, 1, 13),
      recallTypeCode = FTR_14,
      createdByUsername = "user001",
      createdByPrison = "PRI",
    )

    val createRecall = createRecall(recall)

    val ualAdjustment = AdjustmentDto(
      id = UUID.randomUUID().toString(),
      person = "A12345B",
      adjustmentType = "UNLAWFULLY_AT_LARGE",
      fromDate = LocalDate.of(2024, 1, 14),
      toDate = LocalDate.of(2024, 1, 22),
      days = 9,
      recallId = createRecall.toString(),
      unlawfullyAtLarge = UnlawfullyAtLargeDto(),
    )
    adjustmentsApi.stubGetRecallAdjustments(
      "A12345B",
      createRecall.recallUuid.toString(),
      listOf(
        ualAdjustment,
      ),
    )

    val actualRecall = getRecallByUUID(createRecall.recallUuid)
    assertThat(actualRecall)
      .usingRecursiveComparison()
      .ignoringFields("createdAt")
      .isEqualTo(
        Recall(
          recallUuid = createRecall.recallUuid,
          prisonerId = "A12345B",
          revocationDate = LocalDate.of(2024, 1, 2),
          returnToCustodyDate = LocalDate.of(2024, 1, 13),
          inPrisonOnRevocationDate = null,
          recallType = FTR_14,
          createdByUsername = "user001",
          createdAt = ZonedDateTime.now(),
          createdByPrison = "PRI",
          source = EventSource.DPS,
          ual = RecallUALAdjustment(ualAdjustment.id!!, 9),
        ),
      )
    val messages = getMessages(1)
    assertThat(messages).hasSize(1).extracting<String> { it.eventType }.contains("recall.inserted")

    adjustmentsApi.verifyAdjustmentCreated(
      AdjustmentDto(
        id = null,
        person = "A12345B",
        adjustmentType = "UNLAWFULLY_AT_LARGE",
        fromDate = LocalDate.of(2024, 1, 3),
        toDate = LocalDate.of(2024, 1, 12),
        days = null,
        recallId = createRecall.recallUuid.toString(),
        unlawfullyAtLarge = UnlawfullyAtLargeDto(),
      ),
    )
  }

  @Test
  fun `Create recall with no UAL`() {
    val recall = CreateRecall(
      prisonerId = "A12345B",
      revocationDate = LocalDate.of(2024, 1, 2),
      returnToCustodyDate = null,
      recallTypeCode = FTR_14,
      createdByUsername = "user001",
      createdByPrison = "PRI",
    )

    val createRecall = createRecall(recall)
    val actualRecall = getRecallByUUID(createRecall.recallUuid)

    assertThat(actualRecall)
      .usingRecursiveComparison()
      .ignoringFields("createdAt")
      .isEqualTo(
        Recall(
          recallUuid = createRecall.recallUuid,
          prisonerId = "A12345B",
          revocationDate = LocalDate.of(2024, 1, 2),
          null,
          inPrisonOnRevocationDate = null,
          recallType = FTR_14,
          createdByUsername = "user001",
          createdAt = ZonedDateTime.now(),
          createdByPrison = "PRI",
          source = EventSource.DPS,
        ),
      )
    val messages = getMessages(1)
    assertThat(messages).hasSize(1).extracting<String> { it.eventType }.contains("recall.inserted")

    adjustmentsApi.verifyNoAdjustmentsCreated()
  }

  @Test
  fun `Create recall with a uuid via edit endpoint`() {
    val recall = CreateRecall(
      prisonerId = "A12345B",
      revocationDate = LocalDate.of(2024, 1, 2),
      returnToCustodyDate = LocalDate.of(2024, 2, 3),
      recallTypeCode = FTR_28,
      createdByUsername = "user001",
      createdByPrison = "PRI",
    )

    val uuid = UUID.randomUUID()

    val createdRecall = updateRecall(recall, uuid)

    assertThat(uuid).isEqualTo(createdRecall.recallUuid)
    val actualRecall = getRecallByUUID(createdRecall.recallUuid)

    assertThat(actualRecall)
      .usingRecursiveComparison()
      .ignoringFields("createdAt")
      .isEqualTo(
        Recall(
          recallUuid = createdRecall.recallUuid,
          prisonerId = "A12345B",
          revocationDate = LocalDate.of(2024, 1, 2),
          returnToCustodyDate = LocalDate.of(2024, 2, 3),
          inPrisonOnRevocationDate = null,
          recallType = FTR_28,
          createdByUsername = "user001",
          createdAt = ZonedDateTime.now(),
          createdByPrison = "PRI",
          source = EventSource.DPS,
        ),
      )
    val messages = getMessages(1)
    assertThat(messages).hasSize(1).extracting<String> { it.eventType }.contains("recall.inserted")
  }

  @Test
  fun `Get all recalls for a prisoner`() {
    val (sentenceOne, _) = createCourtCaseTwoSentences()
    val recallOne = DpsDataCreator.dpsCreateRecall(
      revocationDate = LocalDate.of(2024, 7, 1),
      returnToCustodyDate = null,
      recallTypeCode = LR,
      sentenceIds = listOf(
        sentenceOne.sentenceUuid,
      ),
    )
    val uuidOne = createRecall(recallOne).recallUuid
    val recallTwo = DpsDataCreator.dpsCreateRecall(
      revocationDate = LocalDate.of(2024, 9, 1),
      returnToCustodyDate = LocalDate.of(2024, 9, 1),
      recallTypeCode = FTR_14,
      sentenceIds = listOf(
        sentenceOne.sentenceUuid,
      ),
      calculationRequestId = 9991,
    )
    val uuidTwo = createRecall(recallTwo).recallUuid

    val adjustmentForRecall2 = AdjustmentDto(
      id = UUID.randomUUID().toString(),
      person = DpsDataCreator.DEFAULT_PRISONER_ID,
      adjustmentType = "UNLAWFULLY_AT_LARGE",
      fromDate = LocalDate.of(2024, 1, 14),
      toDate = LocalDate.of(2024, 1, 22),
      days = 9,
      recallId = uuidTwo.toString(),
      unlawfullyAtLarge = UnlawfullyAtLargeDto(),
    )
    val randomAdjustment = AdjustmentDto(
      id = UUID.randomUUID().toString(),
      person = DpsDataCreator.DEFAULT_PRISONER_ID,
      adjustmentType = "ADDITIONAL_DAYS_AWARDED",
      fromDate = LocalDate.of(2024, 1, 1),
      toDate = LocalDate.of(2024, 1, 21),
      days = 20,
      recallId = null,
      unlawfullyAtLarge = null,
    )

    adjustmentsApi.stubGetPrisonerAdjustments(DpsDataCreator.DEFAULT_PRISONER_ID, listOf(randomAdjustment, adjustmentForRecall2))

    val recalls = getRecallsByPrisonerId(DpsDataCreator.DEFAULT_PRISONER_ID)

    assertThat(recalls)
      .usingRecursiveComparison()
      .ignoringFields("createdAt", "sentences", "courtCaseIds", "courtCases") // courtCases tested separately
      .ignoringCollectionOrder()
      .isEqualTo(
        listOf(
          Recall(
            recallUuid = uuidOne,
            prisonerId = DpsDataCreator.DEFAULT_PRISONER_ID,
            revocationDate = LocalDate.of(2024, 7, 1),
            returnToCustodyDate = null,
            inPrisonOnRevocationDate = null,
            recallType = LR,
            createdByUsername = "user001",
            createdAt = ZonedDateTime.now(),
            createdByPrison = "PRISON1",
            source = EventSource.DPS,
            ual = null,
            calculationRequestId = null,
          ),
          Recall(
            recallUuid = uuidTwo,
            prisonerId = DpsDataCreator.DEFAULT_PRISONER_ID,
            revocationDate = LocalDate.of(2024, 9, 1),
            returnToCustodyDate = LocalDate.of(2024, 9, 1),
            inPrisonOnRevocationDate = null,
            recallType = FTR_14,
            createdByUsername = "user001",
            createdAt = ZonedDateTime.now(),
            createdByPrison = "PRISON1",
            source = EventSource.DPS,
            ual = RecallUALAdjustment(adjustmentForRecall2.id!!, 9),
            calculationRequestId = 9991,
          ),
        ),
      )

    val recall1 = recalls.first { it.recallUuid == uuidOne }
    val recall2 = recalls.first { it.recallUuid == uuidTwo }

    assertThat(recall1.isManual).isTrue()
    assertThat(recall2.isManual).isFalse()

    assertThat(recalls).allMatch { it.courtCases[0].sentences.size == 1 && it.courtCases.size == 1 }
  }

  @Test
  fun `Get recalls builds correct court case and sentence groups for DPS recall`() {
    val appearanceDateOne = LocalDate.now().minusDays(30)
    val firstChargeCourtCaseOne = DpsDataCreator.dpsCreateCharge(
      sentence = DpsDataCreator.dpsCreateSentence(convictionDate = appearanceDateOne),
      offenceStartDate = LocalDate.of(2025, 2, 3),
    )
    val secondChargeCourtCaseOne = DpsDataCreator.dpsCreateCharge(
      sentence = DpsDataCreator.dpsCreateSentence(convictionDate = appearanceDateOne),
      offenceStartDate = LocalDate.of(2025, 3, 4),
      offenceEndDate = LocalDate.of(2025, 4, 5),
    )
    val appearanceCourtCaseOne = DpsDataCreator.dpsCreateCourtAppearance(
      charges = listOf(firstChargeCourtCaseOne, secondChargeCourtCaseOne),
      courtCaseReference = "CC1",
      appearanceDate = appearanceDateOne,
    )
    val (courtCaseOneUuid, courtCaseOne) = createCourtCase(
      DpsDataCreator.dpsCreateCourtCase(
        prisonerId = DpsDataCreator.DEFAULT_PRISONER_ID,
        appearances = listOf(appearanceCourtCaseOne),
      ),
    )
    val sentenceOneOnCourtCaseOne = courtCaseOne.appearances.first().charges.first().sentence!!
    val sentenceTwoOnCourtCaseOne = courtCaseOne.appearances.first().charges[1].sentence!!

    val appearanceDateTwo = LocalDate.now().minusDays(20)
    val firstChargeCourtCaseTwo = DpsDataCreator.dpsCreateCharge(
      sentence = DpsDataCreator.dpsCreateSentence(convictionDate = appearanceDateTwo),
      offenceStartDate = LocalDate.of(2025, 6, 7),
    )
    val appearanceCourtCaseTwo = DpsDataCreator.dpsCreateCourtAppearance(
      charges = listOf(firstChargeCourtCaseTwo),
      courtCaseReference = "CC2",
      appearanceDate = appearanceDateTwo,
    )
    val (courtCaseTwoUuid, courtCaseTwo) = createCourtCase(
      DpsDataCreator.dpsCreateCourtCase(
        prisonerId = DpsDataCreator.DEFAULT_PRISONER_ID,
        appearances = listOf(appearanceCourtCaseTwo),
      ),
    )
    val sentenceThreeOnCourtCaseTwo = courtCaseTwo.appearances.first().charges.first().sentence!!

    val recallOne = DpsDataCreator.dpsCreateRecall(
      revocationDate = LocalDate.of(2024, 7, 1),
      returnToCustodyDate = LocalDate.of(2024, 7, 20),
      recallTypeCode = LR,
      sentenceIds = listOf(
        sentenceOneOnCourtCaseOne.sentenceUuid,
        sentenceTwoOnCourtCaseOne.sentenceUuid,
        sentenceThreeOnCourtCaseTwo.sentenceUuid,
      ),
    )
    val uuidOne = createRecall(recallOne).recallUuid

    val recallByUuid = getRecallByUUID(uuidOne)
    val allRecallsForPrisoner = getRecallsByPrisonerId(DpsDataCreator.DEFAULT_PRISONER_ID)
    assertThat(allRecallsForPrisoner).containsOnly(recallByUuid)

    assertThat(recallByUuid)
      .usingRecursiveComparison()
      .ignoringCollectionOrderInFields(
        "sentences",
        "courtCases",
        "courtCases.sentences",
        "courtCases[*].sentences",
        "sentences[*].periodLengths",
        "courtCases[*].sentences[*].periodLengths",
      )
      .ignoringCollectionOrder()
      .isEqualTo(
        Recall(
          recallUuid = uuidOne,
          prisonerId = DpsDataCreator.DEFAULT_PRISONER_ID,
          revocationDate = LocalDate.of(2024, 7, 1),
          returnToCustodyDate = LocalDate.of(2024, 7, 20),
          inPrisonOnRevocationDate = null,
          recallType = LR,
          createdByUsername = "user001",
          createdAt = recallByUuid.createdAt,
          createdByPrison = "PRISON1",
          source = EventSource.DPS,
          courtCases = listOf(
            RecallCourtCaseDetails(
              courtCaseReference = "CC1",
              courtCaseUuid = courtCaseOneUuid,
              courtCode = "COURT1",
              sentencingAppearanceDate = appearanceDateOne,
              sentences = listOf(
                RecalledSentence(
                  sentenceUuid = sentenceOneOnCourtCaseOne.sentenceUuid,
                  offenceCode = "OFF123",
                  offenceStartDate = LocalDate.of(2025, 2, 3),
                  offenceEndDate = null,
                  sentenceDate = appearanceDateOne,
                  lineNumber = null,
                  countNumber = "1",
                  periodLengths = listOf(
                    PeriodLength(
                      years = 1,
                      months = null,
                      weeks = null,
                      days = null,
                      periodOrder = "years",
                      periodLengthType = PeriodLengthType.SENTENCE_LENGTH,
                      legacyData = null,
                      periodLengthUuid = recallByUuid.courtCases[0].sentences[0].periodLengths[0].periodLengthUuid,
                    ),
                  ),
                  sentenceServeType = "FORTHWITH",
                  sentenceTypeDescription = "Serious Offence Sec 250 Sentencing Code (U18)",
                ),
                RecalledSentence(
                  sentenceUuid = sentenceTwoOnCourtCaseOne.sentenceUuid,
                  offenceCode = "OFF123",
                  offenceStartDate = LocalDate.of(2025, 3, 4),
                  offenceEndDate = LocalDate.of(2025, 4, 5),
                  sentenceDate = appearanceDateOne,
                  lineNumber = null,
                  countNumber = "1",
                  periodLengths = listOf(
                    PeriodLength(
                      years = 1,
                      months = null,
                      weeks = null,
                      days = null,
                      periodOrder = "years",
                      periodLengthType = PeriodLengthType.SENTENCE_LENGTH,
                      legacyData = null,
                      periodLengthUuid = recallByUuid.courtCases[0].sentences[1].periodLengths[0].periodLengthUuid,
                    ),
                  ),
                  sentenceServeType = "FORTHWITH",
                  sentenceTypeDescription = "Serious Offence Sec 250 Sentencing Code (U18)",
                ),
              ),
            ),
            RecallCourtCaseDetails(
              courtCaseReference = "CC2",
              courtCaseUuid = courtCaseTwoUuid,
              courtCode = "COURT1",
              sentencingAppearanceDate = appearanceDateTwo,
              sentences = listOf(
                RecalledSentence(
                  sentenceUuid = sentenceThreeOnCourtCaseTwo.sentenceUuid,
                  offenceCode = "OFF123",
                  offenceStartDate = LocalDate.of(2025, 6, 7),
                  offenceEndDate = null,
                  sentenceDate = appearanceDateTwo,
                  lineNumber = null,
                  countNumber = "1",
                  periodLengths = listOf(
                    PeriodLength(
                      years = 1,
                      months = null,
                      weeks = null,
                      days = null,
                      periodOrder = "years",
                      periodLengthType = PeriodLengthType.SENTENCE_LENGTH,
                      legacyData = null,
                      periodLengthUuid = recallByUuid.courtCases[1].sentences[0].periodLengths[0].periodLengthUuid,
                    ),
                  ),
                  sentenceServeType = "FORTHWITH",
                  sentenceTypeDescription = "Serious Offence Sec 250 Sentencing Code (U18)",
                ),
              ),
            ),
          ),
        ),
      )
  }

  @Test
  fun `Get recalls builds correct court case and sentence groups for NOMIS recall`() {
    // Create a legacy sentence so that the legacy recall is also created.
    val appearanceDate = LocalDate.now().minusDays(30)
    val (chargeLifetimeUuid, toCreateCharge) = createLegacyCharge(
      legacyCreateCourtAppearance = DataCreator.legacyCreateCourtAppearance(appearanceDate = appearanceDate),
      legacyCharge = DataCreator.legacyCreateCharge(offenceStartDate = LocalDate.of(2025, 6, 7)),
    )
    val legacySentence = DataCreator.legacyCreateSentence(
      chargeUuids = listOf(chargeLifetimeUuid),
      appearanceUuid = toCreateCharge.appearanceLifetimeUuid,
      sentenceLegacyData = DataCreator.sentenceLegacyData(sentenceCalcType = "FTR_ORA", sentenceCategory = "2020"),
      returnToCustodyDate = LocalDate.of(2023, 1, 1),
    )
    val response = webTestClient
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

    val recalls = getRecallsByPrisonerId(response.prisonerId)
    assertThat(recalls).hasSize(1)

    val theRecall = recalls[0]
    assertThat(theRecall)
      .usingRecursiveComparison()
      .ignoringCollectionOrder()
      .isEqualTo(
        Recall(
          recallUuid = theRecall.recallUuid,
          prisonerId = DpsDataCreator.DEFAULT_PRISONER_ID,
          revocationDate = null,
          returnToCustodyDate = LocalDate.of(2023, 1, 1),
          inPrisonOnRevocationDate = null,
          recallType = FTR_28,
          createdByUsername = "SOME_USER",
          createdAt = theRecall.createdAt,
          createdByPrison = null,
          source = EventSource.NOMIS,
          courtCases = listOf(
            RecallCourtCaseDetails(
              courtCaseReference = null,
              courtCaseUuid = null,
              courtCode = null,
              sentencingAppearanceDate = null,
              sentences = listOf(
                RecalledSentence(
                  sentenceUuid = response.lifetimeUuid,
                  offenceCode = "OFF1",
                  offenceStartDate = LocalDate.of(2025, 6, 7),
                  offenceEndDate = null,
                  sentenceDate = null,
                  lineNumber = "4",
                  countNumber = null,
                  periodLengths = emptyList(),
                  sentenceServeType = "CONCURRENT",
                  sentenceTypeDescription = "Unknown pre-recall sentence",
                ),
              ),
            ),
          ),
        ),
      )
  }

  @Test
  fun `Updating a recall with the same UAL does not make any changes to adjustments`() {
    val (sentenceOne, sentenceTwo) = createCourtCaseTwoSentences()
    val originalRecall = DpsDataCreator.dpsCreateRecall(
      sentenceIds = listOf(
        sentenceOne.sentenceUuid,
        sentenceTwo.sentenceUuid,
      ),
    )
    val uuid = createRecall(originalRecall).recallUuid
    purgeQueues()
    adjustmentsApi.resetRequests()

    updateRecall(
      CreateRecall(
        prisonerId = "A12345B",
        recallTypeCode = FTR_14,
        revocationDate = originalRecall.revocationDate,
        returnToCustodyDate = originalRecall.returnToCustodyDate,
        createdByUsername = "user001",
        createdByPrison = "New prison",
        sentenceIds = listOf(sentenceOne.sentenceUuid),
        calculationRequestId = 9993,
      ),
      uuid,
    )

    val savedRecall = getRecallByUUID(uuid)

    assertThat(savedRecall)
      .usingRecursiveComparison()
      .ignoringFields("createdAt", "sentences", "courtCaseIds", "courtCases")
      .ignoringCollectionOrder()
      .isEqualTo(
        Recall(
          recallUuid = uuid,
          prisonerId = originalRecall.prisonerId,
          revocationDate = originalRecall.revocationDate,
          returnToCustodyDate = originalRecall.returnToCustodyDate,
          inPrisonOnRevocationDate = originalRecall.inPrisonOnRevocationDate,
          recallType = FTR_14,
          createdByUsername = originalRecall.createdByUsername,
          createdByPrison = originalRecall.createdByPrison,
          createdAt = ZonedDateTime.now(),
          source = EventSource.DPS,
          calculationRequestId = 9993,
        ),
      )

    assertThat(savedRecall.courtCases).hasSize(1)
    assertThat(savedRecall.courtCases[0].sentences).hasSize(1)
    assertThat(savedRecall.courtCases[0].sentences).extracting<UUID> { it.sentenceUuid }.contains(sentenceOne.sentenceUuid)
    assertThat(savedRecall.isManual).isFalse

    val messages = getMessages(1)
    assertThat(messages).hasSize(1).extracting<String> { it.eventType }.contains("recall.updated")
    val message = messages[0]
    val sentenceIds = message.additionalInformation.get("sentenceIds").toList().map { arr -> arr.asText() }
    val previousSentenceIds =
      message.additionalInformation.get("previousSentenceIds").toList().map { arr -> arr.asText() }
    assertThat(sentenceIds)
      .contains(sentenceOne.sentenceUuid.toString())
    assertThat(previousSentenceIds)
      .contains(sentenceOne.sentenceUuid.toString())
      .contains(sentenceTwo.sentenceUuid.toString())

    val historicalRecalls = recallHistoryRepository.findByRecallUuid(uuid)
    assertThat(historicalRecalls).hasSize(1)
    assertThat(historicalRecalls[0].historyStatusId).isEqualTo(RecallEntityStatus.EDITED)
    assertThat(historicalRecalls[0].historyCreatedAt).isNotNull()

    val historicalRecallSentences = recallSentenceHistoryRepository.findByRecallHistoryId(historicalRecalls[0].id)
    assertThat(historicalRecallSentences!!).hasSize(2)
    assertThat(historicalRecallSentences.map { it.sentence.sentenceUuid }).containsExactlyInAnyOrder(
      sentenceOne.sentenceUuid,
      sentenceTwo.sentenceUuid,
    )
    adjustmentsApi.verifyNoAdjustmentsCreated()
    adjustmentsApi.verifyNoAdjustmentsUpdated()
    adjustmentsApi.verifyNoAdjustmentsDeleted()
  }

  @Test
  fun `Updating a recall to require UAL when it previously had none creates the adjustment`() {
    val (sentenceOne, sentenceTwo) = createCourtCaseTwoSentences()
    val originalRecall = DpsDataCreator.dpsCreateRecall(
      sentenceIds = listOf(
        sentenceOne.sentenceUuid,
        sentenceTwo.sentenceUuid,
      ),
      revocationDate = LocalDate.of(2024, 1, 2),
      returnToCustodyDate = null,
      inPrisonOnRevocationDate = true,
    )
    val uuid = createRecall(originalRecall).recallUuid
    purgeQueues()
    adjustmentsApi.resetRequests()

    updateRecall(
      CreateRecall(
        prisonerId = "A12345B",
        recallTypeCode = FTR_14,
        revocationDate = LocalDate.of(2024, 1, 1),
        returnToCustodyDate = LocalDate.of(2024, 1, 13),
        createdByUsername = "user001",
        createdByPrison = "New prison",
        sentenceIds = listOf(sentenceOne.sentenceUuid),
        inPrisonOnRevocationDate = false,
      ),
      uuid,
    )

    val savedRecall = getRecallByUUID(uuid)

    assertThat(savedRecall)
      .usingRecursiveComparison()
      .ignoringFields("createdAt", "sentences", "courtCaseIds", "courtCases")
      .ignoringCollectionOrder()
      .isEqualTo(
        Recall(
          recallUuid = uuid,
          prisonerId = originalRecall.prisonerId,
          revocationDate = LocalDate.of(2024, 1, 1),
          returnToCustodyDate = LocalDate.of(2024, 1, 13),
          inPrisonOnRevocationDate = false,
          recallType = FTR_14,
          createdByUsername = originalRecall.createdByUsername,
          createdByPrison = originalRecall.createdByPrison,
          createdAt = ZonedDateTime.now(),
          source = EventSource.DPS,
        ),
      )

    adjustmentsApi.verifyAdjustmentCreated(
      AdjustmentDto(
        id = null,
        person = originalRecall.prisonerId,
        adjustmentType = "UNLAWFULLY_AT_LARGE",
        fromDate = LocalDate.of(2024, 1, 2),
        toDate = LocalDate.of(2024, 1, 12),
        days = null,
        recallId = uuid.toString(),
        unlawfullyAtLarge = UnlawfullyAtLargeDto(),
      ),
    )
    adjustmentsApi.verifyNoAdjustmentsUpdated()
    adjustmentsApi.verifyNoAdjustmentsDeleted()
  }

  @Test
  fun `Updating a recall to no longer require UAL when it previously had some deletes the adjustment`() {
    val (sentenceOne, sentenceTwo) = createCourtCaseTwoSentences()
    val originalRecall = DpsDataCreator.dpsCreateRecall(
      sentenceIds = listOf(
        sentenceOne.sentenceUuid,
        sentenceTwo.sentenceUuid,
      ),
      revocationDate = LocalDate.of(2024, 1, 13),
      returnToCustodyDate = LocalDate.of(2024, 1, 23),
    )
    val uuid = createRecall(originalRecall).recallUuid
    purgeQueues()
    adjustmentsApi.resetRequests()
    val originalAdjustment = AdjustmentDto(
      id = UUID.randomUUID().toString(),
      person = originalRecall.prisonerId,
      adjustmentType = "UNLAWFULLY_AT_LARGE",
      fromDate = LocalDate.of(2024, 1, 14),
      toDate = LocalDate.of(2024, 1, 22),
      days = null,
      recallId = uuid.toString(),
      unlawfullyAtLarge = UnlawfullyAtLargeDto(),
    )

    adjustmentsApi.stubDeleteAdjustment(originalAdjustment.id!!)
    adjustmentsApi.stubGetRecallAdjustments(originalRecall.prisonerId, uuid.toString(), listOf(originalAdjustment))
    updateRecall(
      CreateRecall(
        prisonerId = "A12345B",
        recallTypeCode = FTR_14,
        revocationDate = LocalDate.of(2024, 1, 1),
        returnToCustodyDate = null,
        createdByUsername = "user001",
        createdByPrison = "New prison",
        sentenceIds = listOf(sentenceOne.sentenceUuid),
      ),
      uuid,
    )

    val savedRecall = getRecallByUUID(uuid)

    assertThat(savedRecall)
      .usingRecursiveComparison()
      .ignoringFields("createdAt", "sentences", "courtCaseIds", "courtCases")
      .ignoringCollectionOrder()
      .isEqualTo(
        Recall(
          recallUuid = uuid,
          prisonerId = originalRecall.prisonerId,
          revocationDate = LocalDate.of(2024, 1, 1),
          returnToCustodyDate = null,
          inPrisonOnRevocationDate = originalRecall.inPrisonOnRevocationDate,
          recallType = FTR_14,
          createdByUsername = originalRecall.createdByUsername,
          createdByPrison = originalRecall.createdByPrison,
          createdAt = ZonedDateTime.now(),
          source = EventSource.DPS,
        ),
      )

    adjustmentsApi.verifyNoAdjustmentsCreated()
    adjustmentsApi.verifyNoAdjustmentsUpdated()
    adjustmentsApi.verifyAdjustmentDeleted(originalAdjustment.id)
  }

  @Test
  fun `Updating a recall to require different UAL updates the adjustment`() {
    val (sentenceOne, sentenceTwo) = createCourtCaseTwoSentences()
    val originalRecall = DpsDataCreator.dpsCreateRecall(
      sentenceIds = listOf(
        sentenceOne.sentenceUuid,
        sentenceTwo.sentenceUuid,
      ),
      revocationDate = LocalDate.of(2024, 1, 13),
      returnToCustodyDate = LocalDate.of(2024, 1, 23),
    )
    val uuid = createRecall(originalRecall).recallUuid
    purgeQueues()
    adjustmentsApi.resetRequests()
    val originalAdjustment = AdjustmentDto(
      id = UUID.randomUUID().toString(),
      person = originalRecall.prisonerId,
      adjustmentType = "UNLAWFULLY_AT_LARGE",
      fromDate = LocalDate.of(2024, 1, 14),
      toDate = LocalDate.of(2024, 1, 22),
      days = 9,
      recallId = uuid.toString(),
      unlawfullyAtLarge = UnlawfullyAtLargeDto(),
    )

    adjustmentsApi.stubAllowUpdateAdjustments()
    adjustmentsApi.stubGetRecallAdjustments(originalRecall.prisonerId, uuid.toString(), listOf(originalAdjustment))
    updateRecall(
      CreateRecall(
        prisonerId = "A12345B",
        recallTypeCode = FTR_14,
        revocationDate = LocalDate.of(2024, 1, 1),
        returnToCustodyDate = LocalDate.of(2024, 1, 13),
        createdByUsername = "user001",
        createdByPrison = "New prison",
        sentenceIds = listOf(sentenceOne.sentenceUuid),
      ),
      uuid,
    )

    val savedRecall = getRecallByUUID(uuid)

    assertThat(savedRecall)
      .usingRecursiveComparison()
      .ignoringFields("createdAt", "sentences", "courtCaseIds", "courtCases")
      .ignoringCollectionOrder()
      .isEqualTo(
        Recall(
          recallUuid = uuid,
          prisonerId = originalRecall.prisonerId,
          revocationDate = LocalDate.of(2024, 1, 1),
          returnToCustodyDate = LocalDate.of(2024, 1, 13),
          inPrisonOnRevocationDate = originalRecall.inPrisonOnRevocationDate,
          recallType = FTR_14,
          createdByUsername = originalRecall.createdByUsername,
          createdByPrison = originalRecall.createdByPrison,
          createdAt = ZonedDateTime.now(),
          source = EventSource.DPS,
          ual = RecallUALAdjustment(originalAdjustment.id!!, 9),
        ),
      )

    adjustmentsApi.verifyNoAdjustmentsCreated()
    adjustmentsApi.verifyAdjustmentUpdated(
      originalAdjustment.id,
      AdjustmentDto(
        id = originalAdjustment.id,
        person = originalRecall.prisonerId,
        adjustmentType = "UNLAWFULLY_AT_LARGE",
        fromDate = LocalDate.of(2024, 1, 2),
        toDate = LocalDate.of(2024, 1, 12),
        days = null,
        recallId = uuid.toString(),
        unlawfullyAtLarge = UnlawfullyAtLargeDto(),
      ),
    )
    adjustmentsApi.verifyNoAdjustmentsDeleted()
  }

  @Test
  fun `Create recall with a sentence and fetch it based on returned UUID`() {
    val (sentenceOne, _) = createCourtCaseTwoSentences()
    val recall = DpsDataCreator.dpsCreateRecall(
      sentenceIds = listOf(
        sentenceOne.sentenceUuid,
      ),
    )

    val createRecall = createRecall(recall)
    val actualRecall = getRecallByUUID(createRecall.recallUuid)
    assertThat(actualRecall)
      .usingRecursiveComparison()
      .ignoringFields("createdAt", "sentences", "courtCaseIds", "courtCases")
      .isEqualTo(
        Recall(
          recallUuid = createRecall.recallUuid,
          prisonerId = DpsDataCreator.DEFAULT_PRISONER_ID,
          revocationDate = LocalDate.of(2024, 1, 2),
          returnToCustodyDate = LocalDate.of(2024, 2, 3),
          inPrisonOnRevocationDate = null,
          recallType = FTR_14,
          createdByUsername = "user001",
          createdAt = ZonedDateTime.now(),
          createdByPrison = "PRISON1",
          source = EventSource.DPS,
        ),
      )
    val messages = getMessages(1)
    assertThat(messages).hasSize(1).extracting<String> { it.eventType }.contains("recall.inserted")
  }

  @Test
  fun `Create recall with two sentences and fetch it based on returned UUID`() {
    val (sentenceOne, sentenceTwo) = createCourtCaseTwoSentences()
    val recall = DpsDataCreator.dpsCreateRecall(
      sentenceIds = listOf(
        sentenceOne.sentenceUuid,
        sentenceTwo.sentenceUuid,
      ),
    )

    val createRecall = createRecall(recall)
    val actualRecall = getRecallByUUID(createRecall.recallUuid)
    assertThat(actualRecall)
      .usingRecursiveComparison()
      .ignoringFields("createdAt", "sentences", "courtCaseIds", "courtCases")
      .isEqualTo(
        Recall(
          recallUuid = createRecall.recallUuid,
          prisonerId = DpsDataCreator.DEFAULT_PRISONER_ID,
          revocationDate = LocalDate.of(2024, 1, 2),
          returnToCustodyDate = LocalDate.of(2024, 2, 3),
          inPrisonOnRevocationDate = null,
          recallType = FTR_14,
          createdByUsername = "user001",
          createdAt = ZonedDateTime.now(),
          createdByPrison = "PRISON1",
          source = EventSource.DPS,
        ),
      )
    val messages = getMessages(1)
    assertThat(messages).hasSize(1).extracting<String> { it.eventType }.contains("recall.inserted")
  }

  @Test
  fun `Fetch created recall by UUID ignoring unrelated recall_sentences`() {
    val recall = CreateRecall(
      prisonerId = "A12345B",
      revocationDate = LocalDate.of(2024, 1, 2),
      returnToCustodyDate = LocalDate.of(2024, 2, 3),
      recallTypeCode = FTR_14,
      createdByUsername = "user001",
      createdByPrison = "PRI",
      sentenceIds = emptyList(),
    )

    val createRecall = createRecall(recall)
    val actualRecall = getRecallByUUID(createRecall.recallUuid)

    assertThat(actualRecall)
      .usingRecursiveComparison()
      .ignoringFields("createdAt")
      .isEqualTo(
        Recall(
          recallUuid = createRecall.recallUuid,
          prisonerId = "A12345B",
          revocationDate = LocalDate.of(2024, 1, 2),
          returnToCustodyDate = LocalDate.of(2024, 2, 3),
          inPrisonOnRevocationDate = null,
          recallType = FTR_14,
          createdByUsername = "user001",
          createdAt = ZonedDateTime.now(),
          createdByPrison = "PRI",
          source = EventSource.DPS,
        ),
      )
    val messages = getMessages(1)
    assertThat(messages).hasSize(1).extracting<String> { it.eventType }.contains("recall.inserted")
  }

  @Test
  fun `Delete a recall with UAL deletes the adjustment as well`() {
    val (sentenceOne, sentenceTwo) = createCourtCaseTwoSentences()
    val recall = DpsDataCreator.dpsCreateRecall(
      sentenceIds = listOf(
        sentenceOne.sentenceUuid,
        sentenceTwo.sentenceUuid,
      ),
    )
    val createRecall = createRecall(recall)
    purgeQueues()

    val adjustment = AdjustmentDto(
      id = UUID.randomUUID().toString(),
      person = DpsDataCreator.DEFAULT_PRISONER_ID,
      adjustmentType = "UNLAWFULLY_AT_LARGE",
      toDate = LocalDate.of(2024, 1, 1),
      fromDate = LocalDate.of(2024, 1, 11),
      days = 10,
      recallId = createRecall.recallUuid.toString(),
      unlawfullyAtLarge = UnlawfullyAtLargeDto(),
    )
    adjustmentsApi.stubGetRecallAdjustments(
      DpsDataCreator.DEFAULT_PRISONER_ID,
      createRecall.recallUuid.toString(),
      listOf(adjustment),
    )
    adjustmentsApi.stubDeleteAdjustment(adjustment.id!!)
    deleteRecall(createRecall.recallUuid)

    val recalls = getRecallsByPrisonerId(DpsDataCreator.DEFAULT_PRISONER_ID)

    assertThat(recalls).isEmpty()

    val messages = getMessages(1)
    assertThat(messages).hasSize(1).extracting<String> { it.eventType }.contains("recall.deleted")

    val historicalRecalls = recallHistoryRepository.findByRecallUuid(createRecall.recallUuid)
    assertThat(historicalRecalls).hasSize(1)
    assertThat(historicalRecalls[0].historyStatusId).isEqualTo(RecallEntityStatus.DELETED)
    assertThat(historicalRecalls[0].historyCreatedAt).isNotNull()

    val historicalRecallSentences = recallSentenceHistoryRepository.findByRecallHistoryId(historicalRecalls[0].id)
    assertThat(historicalRecallSentences!!).hasSize(2)
    assertThat(historicalRecallSentences.map { it.sentence.sentenceUuid }).containsExactlyInAnyOrder(
      sentenceOne.sentenceUuid,
      sentenceTwo.sentenceUuid,
    )
    adjustmentsApi.verifyAdjustmentDeleted(adjustment.id)
  }

  @Test
  fun `Delete a recall without UAL`() {
    val (sentenceOne, sentenceTwo) = createCourtCaseTwoSentences()
    val recall = DpsDataCreator.dpsCreateRecall(
      sentenceIds = listOf(
        sentenceOne.sentenceUuid,
        sentenceTwo.sentenceUuid,
      ),
    )
    val createRecall = createRecall(recall)
    purgeQueues()

    adjustmentsApi.stubGetRecallAdjustments(
      DpsDataCreator.DEFAULT_PRISONER_ID,
      createRecall.recallUuid.toString(),
      emptyList(),
    )
    deleteRecall(createRecall.recallUuid)

    val recalls = getRecallsByPrisonerId(DpsDataCreator.DEFAULT_PRISONER_ID)

    assertThat(recalls).isEmpty()
  }

  @Test
  fun `Delete a recall where many recalls exist`() {
    val (sentenceOne, sentenceTwo) = createCourtCaseTwoSentences()
    val recallOne = DpsDataCreator.dpsCreateRecall(
      sentenceIds = listOf(
        sentenceOne.sentenceUuid,
        sentenceTwo.sentenceUuid,
      ),
    )
    val recallTwo = DpsDataCreator.dpsCreateRecall(
      sentenceIds = listOf(
        sentenceOne.sentenceUuid,
      ),
      revocationDate = recallOne.revocationDate.plusWeeks(4),
      returnToCustodyDate = recallOne.returnToCustodyDate!!.plusWeeks(4),
    )
    val recallOneId = createRecall(recallOne).recallUuid
    val recallTwoId = createRecall(recallTwo).recallUuid
    purgeQueues()

    adjustmentsApi.stubGetRecallAdjustments(
      DpsDataCreator.DEFAULT_PRISONER_ID,
      recallTwoId.toString(),
      emptyList(),
    )

    deleteRecall(recallTwoId)

    val recalls = getRecallsByPrisonerId(DpsDataCreator.DEFAULT_PRISONER_ID)

    assertThat(recalls).hasSize(1)
    assertThat(recalls[0].recallUuid).isEqualTo(recallOneId)

    val messages = getMessages(1)
    assertThat(messages).hasSize(1)
      .extracting<String> { it.eventType }.contains("recall.deleted")
    assertThat(messages[0].additionalInformation.get("previousRecallId").asText()).isEqualTo(recallOneId.toString())
  }

  @Test
  fun `Delete a legacy recall should also delete sentence`() {
    // Create a legacy sentence so that the legacy recall is also created.
    val (legacySentenceUuid, _) = createLegacySentence(
      legacySentence = DataCreator.legacyCreateSentence(
        sentenceLegacyData = DataCreator.sentenceLegacyData(
          sentenceCalcType = "FTR_ORA",
          sentenceCategory = "2020",
        ),
        returnToCustodyDate = LocalDate.of(2023, 1, 1),
      ),
    )
    val recall = getRecallsByPrisonerId(DpsDataCreator.DEFAULT_PRISONER_ID).first()
    purgeQueues()

    deleteRecall(recall.recallUuid)

    val recalls = getRecallsByPrisonerId(DpsDataCreator.DEFAULT_PRISONER_ID)

    assertThat(recalls).isEmpty()

    val messages = getMessages(2)
    assertThat(messages).hasSize(2)
      .extracting<String> { it.eventType }.contains("recall.deleted", "sentence.deleted")

    val historicalRecalls = recallHistoryRepository.findByRecallUuid(recall.recallUuid)
    assertThat(historicalRecalls).hasSize(1)
    assertThat(historicalRecalls[0].historyStatusId).isEqualTo(RecallEntityStatus.DELETED)
    assertThat(historicalRecalls[0].historyCreatedAt).isNotNull()

    val historicalRecallSentences = recallSentenceHistoryRepository.findByRecallHistoryId(historicalRecalls[0].id)
    assertThat(historicalRecallSentences!!).hasSize(1)
    assertThat(historicalRecallSentences.map { it.sentence.sentenceUuid }).containsExactlyInAnyOrder(legacySentenceUuid)
  }

  @Test
  fun `Can create a recall on a legacy recall sentence with mapping`() {
    val (sentenceOne, sentenceTwo) = createCourtCaseTwoSentences()
    val (legacySentenceUuid, _) = createLegacySentence(
      legacySentence = DataCreator.legacyCreateSentence(
        sentenceLegacyData = DataCreator.sentenceLegacyData(
          sentenceCalcType = "FTR_ORA",
          sentenceCategory = "2020",
        ),
        returnToCustodyDate = LocalDate.of(2023, 1, 1),
        active = false,
      ),
    )
    val recall = getRecallsByPrisonerId(DpsDataCreator.DEFAULT_PRISONER_ID).first()
    assertThat(recall.courtCases[0].sentences.first().sentenceUuid).isEqualTo(legacySentenceUuid)
    val recallIncludingALegacySentence = DpsDataCreator.dpsCreateRecall(
      sentenceIds = listOf(
        sentenceOne.sentenceUuid,
        sentenceTwo.sentenceUuid,
        legacySentenceUuid,
      ),
    )

    webTestClient
      .post()
      .uri("/recall")
      .bodyValue(recallIncludingALegacySentence)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_SENTENCING__RECORD_RECALL_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isEqualTo(201)

    val sentence = getLegacySentence(legacySentenceUuid)
    assertThat(sentence.active).isTrue
  }

  @Test
  fun `Cannot create a recall on a legacy recall sentence without mapping`() {
    val (sentenceOne, sentenceTwo) = createCourtCaseTwoSentences()
    val (legacySentenceUuid, _) = createLegacySentence(
      legacySentence = DataCreator.legacyCreateSentence(
        sentenceLegacyData = DataCreator.sentenceLegacyData(
          sentenceCalcType = "FTR",
          sentenceCategory = "2020",
        ),
        returnToCustodyDate = LocalDate.of(2023, 1, 1),
      ),
    )
    val recall = getRecallsByPrisonerId(DpsDataCreator.DEFAULT_PRISONER_ID).first()
    assertThat(recall.courtCases[0].sentences.first().sentenceUuid).isEqualTo(legacySentenceUuid)
    val recallIncludingALegacySentence = DpsDataCreator.dpsCreateRecall(
      recallTypeCode = LR,
      sentenceIds = listOf(
        sentenceOne.sentenceUuid,
        sentenceTwo.sentenceUuid,
        legacySentenceUuid,
      ),
    )

    webTestClient
      .post()
      .uri("/recall")
      .bodyValue(recallIncludingALegacySentence)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_SENTENCING__RECORD_RECALL_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isEqualTo(422)
      .expectBody()
      .jsonPath("$.developerMessage")
      .isEqualTo("Tried to create a recall for sentence ($legacySentenceUuid) but not possible due to UNKNOWN_PRE_RECALL_MAPPING")
  }

  @ParameterizedTest(name = "Test classification {0} and recall type {1} combination for a DPS sentence results in {2} possible recall")
  @MethodSource("dpsSentenceAndClassificationCombinationParameters")
  fun `Test each classification and recall type combination for a DPS sentence`(sentenceTypeClassification: SentenceTypeClassification, recallType: RecallType, expectedIsPossible: IsRecallPossible) {
    val sentenceTypeId = sentenceTypeRepository.findAll()
      .first { it.classification == sentenceTypeClassification }
      .sentenceTypeUuid

    val sentence = DpsDataCreator.dpsCreateSentence(
      sentenceTypeId = sentenceTypeId,
    )
    val charge = DpsDataCreator.dpsCreateCharge(sentence = sentence)
    val appearance = DpsDataCreator.dpsCreateCourtAppearance(charges = listOf(charge))
    val (_, courtCase) = createCourtCase(DpsDataCreator.dpsCreateCourtCase(appearances = listOf(appearance)))

    val result = webTestClient
      .post()
      .uri("/recall/is-possible")
      .bodyValue(IsRecallPossibleRequest(sentenceIds = listOf(sentence.sentenceUuid), recallType = recallType))
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_SENTENCING__RECORD_RECALL_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isEqualTo(200)
      .expectBody()
      .jsonPath("$.isRecallPossible")
      .isEqualTo(expectedIsPossible)

    if (expectedIsPossible != IsRecallPossible.YES) {
      result.jsonPath("$.sentenceIds").isEqualTo(listOf(sentence.sentenceUuid.toString()))
    }
  }

  @ParameterizedTest(name = "Test legacy recall {0} and recall type {1} combination results in {2} possible recall")
  @MethodSource("legacySentenceAndClassificationCombinationParameters")
  fun `Test each legacy sentence and recall type combination for a DPS sentence`(legacySentenceType: String, recallType: RecallType, expectedIsPossible: IsRecallPossible) {
    // Create a legacy sentence so that the legacy recall is also created.
    val (legacySentenceUuid, _) = createLegacySentence(
      legacySentence = DataCreator.legacyCreateSentence(
        sentenceLegacyData = DataCreator.sentenceLegacyData(
          sentenceCalcType = legacySentenceType,
          sentenceCategory = "2020",
        ),
        returnToCustodyDate = LocalDate.of(2023, 1, 1),
      ),
    )
    val result = webTestClient
      .post()
      .uri("/recall/is-possible")
      .bodyValue(IsRecallPossibleRequest(sentenceIds = listOf(legacySentenceUuid), recallType = recallType))
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_SENTENCING__RECORD_RECALL_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isEqualTo(200)
      .expectBody()
      .jsonPath("$.isRecallPossible")
      .isEqualTo(expectedIsPossible)

    if (expectedIsPossible != IsRecallPossible.YES) {
      result.jsonPath("$.sentenceIds").isEqualTo(listOf(legacySentenceUuid.toString()))
    }
  }

  private fun getLegacySentence(legacySentenceUuid: UUID): LegacySentence = webTestClient
    .get()
    .uri("/legacy/sentence/$legacySentenceUuid")
    .headers { it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_SENTENCE_RO")) }
    .exchange()
    .expectStatus()
    .isOk
    .expectBody<LegacySentence>()
    .returnResult().responseBody!!

  companion object {
    @JvmStatic
    fun dpsSentenceAndClassificationCombinationParameters(): Stream<Arguments> = Stream.of(
      Arguments.of(SentenceTypeClassification.STANDARD, LR, IsRecallPossible.YES),
      Arguments.of(SentenceTypeClassification.STANDARD, FTR_28, IsRecallPossible.YES),
      Arguments.of(SentenceTypeClassification.STANDARD, FTR_14, IsRecallPossible.YES),
      Arguments.of(SentenceTypeClassification.STANDARD, FTR_HDC_14, IsRecallPossible.YES),
      Arguments.of(SentenceTypeClassification.STANDARD, FTR_HDC_28, IsRecallPossible.YES),
      Arguments.of(SentenceTypeClassification.STANDARD, CUR_HDC, IsRecallPossible.YES),
      Arguments.of(SentenceTypeClassification.STANDARD, IN_HDC, IsRecallPossible.YES),
      Arguments.of(SentenceTypeClassification.STANDARD, FTR_56, IsRecallPossible.YES),
      Arguments.of(SentenceTypeClassification.EXTENDED, LR, IsRecallPossible.YES),
      Arguments.of(SentenceTypeClassification.EXTENDED, FTR_28, IsRecallPossible.RECALL_TYPE_AND_SENTENCE_MAPPING_NOT_POSSIBLE),
      Arguments.of(SentenceTypeClassification.EXTENDED, FTR_14, IsRecallPossible.RECALL_TYPE_AND_SENTENCE_MAPPING_NOT_POSSIBLE),
      Arguments.of(SentenceTypeClassification.EXTENDED, FTR_HDC_14, IsRecallPossible.RECALL_TYPE_AND_SENTENCE_MAPPING_NOT_POSSIBLE),
      Arguments.of(SentenceTypeClassification.EXTENDED, FTR_HDC_28, IsRecallPossible.RECALL_TYPE_AND_SENTENCE_MAPPING_NOT_POSSIBLE),
      Arguments.of(SentenceTypeClassification.EXTENDED, CUR_HDC, IsRecallPossible.RECALL_TYPE_AND_SENTENCE_MAPPING_NOT_POSSIBLE),
      Arguments.of(SentenceTypeClassification.EXTENDED, IN_HDC, IsRecallPossible.RECALL_TYPE_AND_SENTENCE_MAPPING_NOT_POSSIBLE),
      Arguments.of(SentenceTypeClassification.EXTENDED, FTR_56, IsRecallPossible.RECALL_TYPE_AND_SENTENCE_MAPPING_NOT_POSSIBLE),
      Arguments.of(SentenceTypeClassification.SOPC, LR, IsRecallPossible.YES),
      Arguments.of(SentenceTypeClassification.SOPC, FTR_28, IsRecallPossible.YES),
      Arguments.of(SentenceTypeClassification.SOPC, FTR_14, IsRecallPossible.RECALL_TYPE_AND_SENTENCE_MAPPING_NOT_POSSIBLE),
      Arguments.of(SentenceTypeClassification.SOPC, FTR_HDC_14, IsRecallPossible.RECALL_TYPE_AND_SENTENCE_MAPPING_NOT_POSSIBLE),
      Arguments.of(SentenceTypeClassification.SOPC, FTR_HDC_28, IsRecallPossible.RECALL_TYPE_AND_SENTENCE_MAPPING_NOT_POSSIBLE),
      Arguments.of(SentenceTypeClassification.SOPC, CUR_HDC, IsRecallPossible.RECALL_TYPE_AND_SENTENCE_MAPPING_NOT_POSSIBLE),
      Arguments.of(SentenceTypeClassification.SOPC, IN_HDC, IsRecallPossible.RECALL_TYPE_AND_SENTENCE_MAPPING_NOT_POSSIBLE),
      Arguments.of(SentenceTypeClassification.SOPC, FTR_56, IsRecallPossible.RECALL_TYPE_AND_SENTENCE_MAPPING_NOT_POSSIBLE),
      Arguments.of(SentenceTypeClassification.INDETERMINATE, LR, IsRecallPossible.YES),
      Arguments.of(SentenceTypeClassification.INDETERMINATE, FTR_28, IsRecallPossible.RECALL_TYPE_AND_SENTENCE_MAPPING_NOT_POSSIBLE),
      Arguments.of(SentenceTypeClassification.INDETERMINATE, FTR_14, IsRecallPossible.RECALL_TYPE_AND_SENTENCE_MAPPING_NOT_POSSIBLE),
      Arguments.of(SentenceTypeClassification.INDETERMINATE, FTR_HDC_14, IsRecallPossible.RECALL_TYPE_AND_SENTENCE_MAPPING_NOT_POSSIBLE),
      Arguments.of(SentenceTypeClassification.INDETERMINATE, FTR_HDC_28, IsRecallPossible.RECALL_TYPE_AND_SENTENCE_MAPPING_NOT_POSSIBLE),
      Arguments.of(SentenceTypeClassification.INDETERMINATE, CUR_HDC, IsRecallPossible.RECALL_TYPE_AND_SENTENCE_MAPPING_NOT_POSSIBLE),
      Arguments.of(SentenceTypeClassification.INDETERMINATE, IN_HDC, IsRecallPossible.RECALL_TYPE_AND_SENTENCE_MAPPING_NOT_POSSIBLE),
      Arguments.of(SentenceTypeClassification.INDETERMINATE, FTR_56, IsRecallPossible.RECALL_TYPE_AND_SENTENCE_MAPPING_NOT_POSSIBLE),
    )

    @JvmStatic
    fun legacySentenceAndClassificationCombinationParameters(): Stream<Arguments> = Stream.of(
      Arguments.of("LR_EDS18", LR, IsRecallPossible.YES),
      Arguments.of("LR_EDS18", FTR_28, IsRecallPossible.RECALL_TYPE_AND_SENTENCE_MAPPING_NOT_POSSIBLE),

      Arguments.of("LR_EDS21", LR, IsRecallPossible.YES),
      Arguments.of("LR_EDS21", FTR_28, IsRecallPossible.RECALL_TYPE_AND_SENTENCE_MAPPING_NOT_POSSIBLE),

      Arguments.of("LR_EDSU18", LR, IsRecallPossible.YES),
      Arguments.of("LR_EDSU18", FTR_28, IsRecallPossible.RECALL_TYPE_AND_SENTENCE_MAPPING_NOT_POSSIBLE),

      Arguments.of("LR_LIFE", LR, IsRecallPossible.YES),
      Arguments.of("LR_LIFE", FTR_28, IsRecallPossible.RECALL_TYPE_AND_SENTENCE_MAPPING_NOT_POSSIBLE),

      Arguments.of("LR_MLP", LR, IsRecallPossible.YES),
      Arguments.of("LR_MLP", FTR_28, IsRecallPossible.RECALL_TYPE_AND_SENTENCE_MAPPING_NOT_POSSIBLE),

      Arguments.of("LR_ALP", LR, IsRecallPossible.YES),
      Arguments.of("LR_ALP", FTR_28, IsRecallPossible.RECALL_TYPE_AND_SENTENCE_MAPPING_NOT_POSSIBLE),

      Arguments.of("LR_ALP_CDE18", LR, IsRecallPossible.YES),
      Arguments.of("LR_ALP_CDE18", FTR_28, IsRecallPossible.RECALL_TYPE_AND_SENTENCE_MAPPING_NOT_POSSIBLE),

      Arguments.of("LR_DLP", LR, IsRecallPossible.YES),
      Arguments.of("LR_DLP", FTR_28, IsRecallPossible.RECALL_TYPE_AND_SENTENCE_MAPPING_NOT_POSSIBLE),

      Arguments.of("LR_SOPC18", LR, IsRecallPossible.YES),
      Arguments.of("LR_SOPC18", FTR_28, IsRecallPossible.YES),
      Arguments.of("LR_SOPC18", FTR_14, IsRecallPossible.RECALL_TYPE_AND_SENTENCE_MAPPING_NOT_POSSIBLE),

      Arguments.of("LR_SOPC21", LR, IsRecallPossible.YES),
      Arguments.of("LR_SOPC21", FTR_28, IsRecallPossible.YES),
      Arguments.of("LR_SOPC21", FTR_14, IsRecallPossible.RECALL_TYPE_AND_SENTENCE_MAPPING_NOT_POSSIBLE),

      Arguments.of("LR", LR, IsRecallPossible.YES),
      Arguments.of("LR_SOPC21", FTR_28, IsRecallPossible.YES),
      Arguments.of("LR_SOPC21", FTR_14, IsRecallPossible.RECALL_TYPE_AND_SENTENCE_MAPPING_NOT_POSSIBLE),

      Arguments.of("CUR", LR, IsRecallPossible.UNKNOWN_PRE_RECALL_MAPPING),
      Arguments.of("CUR", FTR_28, IsRecallPossible.YES),

      Arguments.of("CUR_ORA", LR, IsRecallPossible.UNKNOWN_PRE_RECALL_MAPPING),
      Arguments.of("CUR_ORA", FTR_28, IsRecallPossible.YES),

      Arguments.of("FTR", LR, IsRecallPossible.UNKNOWN_PRE_RECALL_MAPPING),
      Arguments.of("FTR", FTR_28, IsRecallPossible.YES),

      Arguments.of("FTR_HDC", LR, IsRecallPossible.UNKNOWN_PRE_RECALL_MAPPING),
      Arguments.of("FTR_HDC", FTR_28, IsRecallPossible.YES),

      Arguments.of("FTR_HDC_ORA", LR, IsRecallPossible.UNKNOWN_PRE_RECALL_MAPPING),
      Arguments.of("FTR_HDC_ORA", FTR_28, IsRecallPossible.YES),

      Arguments.of("HDR", LR, IsRecallPossible.UNKNOWN_PRE_RECALL_MAPPING),
      Arguments.of("HDR", FTR_28, IsRecallPossible.YES),

      Arguments.of("FTR_56ORA", LR, IsRecallPossible.UNKNOWN_PRE_RECALL_MAPPING),
      Arguments.of("FTR_56ORA", FTR_28, IsRecallPossible.YES),
    )
  }
}
