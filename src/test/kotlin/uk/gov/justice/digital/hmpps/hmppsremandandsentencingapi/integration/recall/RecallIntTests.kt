package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.recall

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.dto.AdjustmentDto
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.dto.UnlawfullyAtLargeDto
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateRecall
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.FineAmount
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.PeriodLength
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.Sentence
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.SentenceType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.recall.Recall
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.recall.RecallCourtCaseDetails
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.recall.RecalledSentence
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.event.EventSource
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.util.DataCreator
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.wiremock.AdjustmentsApiExtension.Companion.adjustmentsApi
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.PeriodLengthType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.RecallEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.RecallType.FTR_14
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.RecallType.FTR_28
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.RecallType.LR
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.SentenceTypeClassification
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacySentenceCreatedResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.SentenceLegacyData
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.service.LegacySentenceService
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.DpsDataCreator
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class RecallIntTests : IntegrationTestBase() {

  @BeforeEach
  fun setUp() {
    adjustmentsApi.stubAllowCreateAdjustments()
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
          sentences = emptyList(),
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
          sentences = emptyList(),
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
          sentences = emptyList(),
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
      returnToCustodyDate = LocalDate.of(2024, 7, 1),
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
    )
    val uuidTwo = createRecall(recallTwo).recallUuid

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
            returnToCustodyDate = LocalDate.of(2024, 7, 1),
            inPrisonOnRevocationDate = null,
            recallType = LR,
            createdByUsername = "user001",
            createdAt = ZonedDateTime.now(),
            createdByPrison = "PRISON1",
            source = EventSource.DPS,
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
          ),
        ),
      )

    assertThat(recalls).allMatch { it.sentences?.size == 1 && it.courtCaseIds?.size == 1 && it.courtCases.size == 1 }
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
    assertThat(allRecallsForPrisoner).containsOnly(recallByUuid) // ensure get all recalls populates fully

    assertThat(recallByUuid).isEqualTo(
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
        sentences = listOf(
          Sentence(
            sentenceUuid = sentenceOneOnCourtCaseOne.sentenceUuid,
            chargeNumber = "1",
            periodLengths = listOf(
              PeriodLength(
                years = 1,
                months = null,
                weeks = null,
                days = null,
                periodOrder = "years",
                periodLengthType = PeriodLengthType.SENTENCE_LENGTH,
                legacyData = null,
                periodLengthUuid = recallByUuid.sentences!![0].periodLengths[0].periodLengthUuid,
              ),
            ),
            sentenceServeType = "FORTHWITH",
            sentenceType = SentenceType(
              sentenceTypeUuid = recallByUuid.sentences[0].sentenceType!!.sentenceTypeUuid,
              description = "Serious Offence Sec 250 Sentencing Code (U18)",
              classification = SentenceTypeClassification.STANDARD,
              hintText = null,
              displayOrder = 220,
            ),
            convictionDate = appearanceDateOne,
            fineAmount = null,
            legacyData = null,
            consecutiveToSentenceUuid = null,
            hasRecall = true,
          ),
          Sentence(
            sentenceUuid = sentenceTwoOnCourtCaseOne.sentenceUuid,
            chargeNumber = "1",
            periodLengths = listOf(
              PeriodLength(
                years = 1,
                months = null,
                weeks = null,
                days = null,
                periodOrder = "years",
                periodLengthType = PeriodLengthType.SENTENCE_LENGTH,
                legacyData = null,
                periodLengthUuid = recallByUuid.sentences[1].periodLengths[0].periodLengthUuid,
              ),
            ),
            sentenceServeType = "FORTHWITH",
            sentenceType = SentenceType(
              sentenceTypeUuid = recallByUuid.sentences[1].sentenceType!!.sentenceTypeUuid,
              description = "Serious Offence Sec 250 Sentencing Code (U18)",
              classification = SentenceTypeClassification.STANDARD,
              hintText = null,
              displayOrder = 220,
            ),
            convictionDate = appearanceDateOne,
            fineAmount = null,
            legacyData = null,
            consecutiveToSentenceUuid = null,
            hasRecall = true,
          ),
          Sentence(
            sentenceUuid = sentenceThreeOnCourtCaseTwo.sentenceUuid,
            chargeNumber = "1",
            periodLengths = listOf(
              PeriodLength(
                years = 1,
                months = null,
                weeks = null,
                days = null,
                periodOrder = "years",
                periodLengthType = PeriodLengthType.SENTENCE_LENGTH,
                legacyData = null,
                periodLengthUuid = recallByUuid.sentences[2].periodLengths[0].periodLengthUuid,
              ),
            ),
            sentenceServeType = "FORTHWITH",
            sentenceType = SentenceType(
              sentenceTypeUuid = recallByUuid.sentences[2].sentenceType!!.sentenceTypeUuid,
              description = "Serious Offence Sec 250 Sentencing Code (U18)",
              classification = SentenceTypeClassification.STANDARD,
              hintText = null,
              displayOrder = 220,
            ),
            convictionDate = appearanceDateTwo,
            fineAmount = null,
            legacyData = null,
            consecutiveToSentenceUuid = null,
            hasRecall = true,
          ),
        ),
        courtCaseIds = listOf(courtCaseOneUuid, courtCaseOneUuid, courtCaseTwoUuid),
        courtCases = listOf(
          RecallCourtCaseDetails(
            courtCaseReference = "CC1",
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
                    periodLengthUuid = recallByUuid.sentences[0].periodLengths[0].periodLengthUuid,
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
                    periodLengthUuid = recallByUuid.sentences[1].periodLengths[0].periodLengthUuid,
                  ),
                ),
                sentenceServeType = "FORTHWITH",
                sentenceTypeDescription = "Serious Offence Sec 250 Sentencing Code (U18)",
              ),
            ),
          ),
          RecallCourtCaseDetails(
            courtCaseReference = "CC2",
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
                    periodLengthUuid = recallByUuid.sentences[2].periodLengths[0].periodLengthUuid,
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
          sentences = listOf(
            Sentence(
              sentenceUuid = response.lifetimeUuid,
              chargeNumber = null,
              periodLengths = emptyList(),
              sentenceServeType = "CONCURRENT",
              sentenceType = SentenceType(
                sentenceTypeUuid = theRecall.sentences!![0].sentenceType!!.sentenceTypeUuid,
                description = "Unknown pre-recall sentence",
                classification = SentenceTypeClassification.LEGACY_RECALL,
                hintText = null,
                displayOrder = 0,
              ),
              convictionDate = null,
              fineAmount = FineAmount(fineAmount = BigDecimal("10.00")),
              legacyData = SentenceLegacyData(
                sentenceCalcType = null,
                sentenceCategory = null,
                sentenceTypeDesc = null,
                postedDate = LocalDate.now().format(DateTimeFormatter.ISO_DATE),
                active = true,
                nomisLineReference = "4",
                bookingId = 1,
              ),
              consecutiveToSentenceUuid = null,
              hasRecall = true,
            ),
          ),
          courtCaseIds = listOf(response.courtCaseId),
          courtCases = listOf(
            RecallCourtCaseDetails(
              courtCaseReference = null,
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
        ),
      )

    assertThat(savedRecall.sentences).hasSize(1)
    assertThat(savedRecall.sentences).extracting<UUID> { it.sentenceUuid }.contains(sentenceOne.sentenceUuid)

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
      days = null,
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
        ),
      )

    adjustmentsApi.verifyNoAdjustmentsCreated()
    adjustmentsApi.verifyAdjustmentUpdated(
      originalAdjustment.id!!,
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
          sentences = emptyList(),
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
  fun `Cannot create a recall on a legacy recall sentence`() {
    val (sentenceOne, sentenceTwo) = createCourtCaseTwoSentences()
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
    assertThat(recall.sentences!!.first().sentenceType!!.sentenceTypeUuid).isEqualTo(LegacySentenceService.recallSentenceTypeBucketUuid)
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
      .isEqualTo(422)
      .expectBody()
      .jsonPath("$.developerMessage")
      .isEqualTo("Tried to create a recall using a legacy recall sentence ($legacySentenceUuid)")
  }
}
