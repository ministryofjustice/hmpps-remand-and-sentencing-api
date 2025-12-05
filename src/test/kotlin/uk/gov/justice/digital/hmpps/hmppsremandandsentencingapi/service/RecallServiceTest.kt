package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.AdjustmentsApiClient
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.dto.AdjustmentDto
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.dto.UnlawfullyAtLargeDto
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.event.EventSource
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.AppearanceChargeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.ChargeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.CourtAppearanceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.CourtCaseEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.RecallEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.RecallSentenceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.RecallTypeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.SentenceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.SentenceTypeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ChargeEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.CourtAppearanceEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.CourtCaseEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.RecallEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.RecallType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ReferenceEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.SentenceEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.SentenceTypeClassification
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.RecallRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.RecallSentenceRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.RecallTypeRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.SentenceRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.audit.RecallHistoryRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.audit.RecallSentenceHistoryRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.service.LegacySentenceService
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.DpsDataCreator
import java.time.LocalDate
import java.util.*

class RecallServiceTest {

  private val recallRepository: RecallRepository = mockk(relaxed = true)
  private val recallSentenceRepository: RecallSentenceRepository = mockk(relaxed = true)
  private val recallTypeRepository: RecallTypeRepository = mockk(relaxed = true)
  private val sentenceRepository: SentenceRepository = mockk(relaxed = true)
  private val sentenceService: SentenceService = mockk(relaxed = true)
  private val recallHistoryRepository: RecallHistoryRepository = mockk(relaxed = true)
  private val recallSentenceHistoryRepository: RecallSentenceHistoryRepository = mockk(relaxed = true)
  private val adjustmentsApiClient: AdjustmentsApiClient = mockk(relaxed = true)

  private val service = RecallService(
    recallRepository,
    recallSentenceRepository,
    recallTypeRepository,
    sentenceRepository,
    sentenceService,
    recallHistoryRepository,
    recallSentenceHistoryRepository,
    adjustmentsApiClient,
  )

  @Test
  fun `delete a dps recall checks for adjustment and deletes if found`() {
    val recallUuid = testNonLegacyRecallEntity.recallUuid
    every { recallRepository.findOneByRecallUuid(recallUuid) } returns testNonLegacyRecallEntity
    every { recallRepository.save(any()) } returns testNonLegacyRecallEntity
    every { recallHistoryRepository.save(any()) } returns mockk()
    every { recallSentenceHistoryRepository.save(any()) } returns mockk()

    val adjustment = AdjustmentDto(
      id = UUID.randomUUID().toString(),
      person = DpsDataCreator.DEFAULT_PRISONER_ID,
      adjustmentType = "UNLAWFULLY_AT_LARGE",
      toDate = LocalDate.of(2024, 1, 1),
      fromDate = LocalDate.of(2024, 1, 11),
      days = 10,
      recallId = recallUuid.toString(),
      unlawfullyAtLarge = UnlawfullyAtLargeDto(),
    )
    every {
      adjustmentsApiClient.getRecallAdjustment(
        DpsDataCreator.DEFAULT_PRISONER_ID,
        recallUuid,
      )
    } returns adjustment

    service.deleteRecall(recallUuid)

    verify { adjustmentsApiClient.deleteAdjustment(adjustment.id!!) }
  }

  @Test
  fun `delete a dps recall checks for adjustment and handles none found`() {
    val recallUuid = testNonLegacyRecallEntity.recallUuid
    every { recallRepository.findOneByRecallUuid(recallUuid) } returns testNonLegacyRecallEntity
    every { recallRepository.save(any()) } returns testNonLegacyRecallEntity
    every { recallHistoryRepository.save(any()) } returns mockk()
    every { recallSentenceHistoryRepository.save(any()) } returns mockk()

    every { adjustmentsApiClient.getRecallAdjustment(DpsDataCreator.DEFAULT_PRISONER_ID, recallUuid) } returns null

    service.deleteRecall(recallUuid)

    verify(exactly = 0) { adjustmentsApiClient.deleteAdjustment(any()) }
  }

  @Test
  fun `delete a legacy recall does not even check for adjustments`() {
    val recallUuid = testLegacyRecallEntity.recallUuid
    every { recallRepository.findOneByRecallUuid(recallUuid) } returns testLegacyRecallEntity
    every { recallRepository.save(any()) } returns testLegacyRecallEntity

    service.deleteRecall(recallUuid)

    verify(exactly = 0) { adjustmentsApiClient.deleteAdjustment(any()) }
    verify(exactly = 0) { adjustmentsApiClient.getRecallAdjustment(any(), any()) }
  }

  companion object {
    private val testCourtCase = CourtCaseEntity(
      caseUniqueIdentifier = "CASE123",
      prisonerId = DpsDataCreator.DEFAULT_PRISONER_ID,
      statusId = CourtCaseEntityStatus.ACTIVE,
      createdBy = "test-user",
    )

    private val testCourtAppearance = CourtAppearanceEntity(
      id = 0,
      appearanceUuid = UUID.randomUUID(),
      courtCase = testCourtCase,
      courtCode = "COURT1",
      appearanceDate = LocalDate.now(),
      statusId = CourtAppearanceEntityStatus.ACTIVE,
      createdBy = "test-user",
      createdPrison = "TEST",
      warrantType = "TEST",
      appearanceOutcome = null,
      courtCaseReference = null,
      updatedAt = null,
      updatedBy = null,
      updatedPrison = null,
      appearanceCharges = mutableSetOf(),
      nextCourtAppearance = null,
      overallConvictionDate = null,
      legacyData = null,
    )

    private val testNonLegacySentenceType = SentenceTypeEntity(
      sentenceTypeUuid = UUID.randomUUID(),
      description = "Life sentence",
      classification = SentenceTypeClassification.INDETERMINATE,
      nomisCjaCode = "LIFE",
      nomisSentenceCalcType = "LIFE",
      displayOrder = 1,
      status = ReferenceEntityStatus.ACTIVE,
      minAgeInclusive = null,
      maxAgeExclusive = null,
      minDateInclusive = null,
      maxDateExclusive = null,
      minOffenceDateInclusive = null,
      maxOffenceDateExclusive = null,
      hintText = null,
      isRecallable = true,
    )
    private val testLegacySentenceType = SentenceTypeEntity(
      sentenceTypeUuid = LegacySentenceService.recallSentenceTypeBucketUuid,
      description = "Life sentence",
      classification = SentenceTypeClassification.LEGACY_RECALL,
      nomisCjaCode = "LIFE",
      nomisSentenceCalcType = "LIFE",
      displayOrder = 1,
      status = ReferenceEntityStatus.ACTIVE,
      minAgeInclusive = null,
      maxAgeExclusive = null,
      minDateInclusive = null,
      maxDateExclusive = null,
      minOffenceDateInclusive = null,
      maxOffenceDateExclusive = null,
      hintText = null,
      isRecallable = true,
    )
    private val testCharge = ChargeEntity(
      chargeUuid = UUID.randomUUID(),
      offenceCode = "TEST123",
      statusId = ChargeEntityStatus.ACTIVE,
      createdBy = "test-user",
      offenceStartDate = null,
      offenceEndDate = null,
      chargeOutcome = null,
      supersedingCharge = null,
      terrorRelated = null,
      foreignPowerRelated = null,
      domesticViolenceRelated = null,
      createdPrison = null,
      legacyData = null,
      appearanceCharges = mutableSetOf(),
    ).apply {
      appearanceCharges.add(
        AppearanceChargeEntity(
          courtAppearanceEntity = testCourtAppearance,
          chargeEntity = this,
          createdBy = "test-user",
          createdPrison = "TEST",
        ),
      )
    }
    private val testNonLegacyRecallEntity = RecallEntity(
      recallUuid = UUID.randomUUID(),
      prisonerId = DpsDataCreator.DEFAULT_PRISONER_ID,
      revocationDate = LocalDate.of(2024, 1, 1),
      returnToCustodyDate = LocalDate.of(2024, 1, 11),
      inPrisonOnRevocationDate = false,
      recallType = RecallTypeEntity(0, RecallType.LR, "Standard"),
      statusId = RecallEntityStatus.ACTIVE,
      createdByUsername = "FOO",
      source = EventSource.DPS,
    ).apply {
      recallSentences = mutableSetOf(
        RecallSentenceEntity(
          recallSentenceUuid = UUID.randomUUID(),
          sentence = SentenceEntity(
            sentenceUuid = UUID.randomUUID(),
            statusId = SentenceEntityStatus.ACTIVE,
            createdBy = "FOO",
            sentenceServeType = "CONCURRENT",
            consecutiveTo = null,
            sentenceType = testNonLegacySentenceType,
            supersedingSentence = null,
            charge = testCharge,
            convictionDate = null,
            fineAmount = null,
          ),
          recall = this,
          createdByUsername = "FOO",
        ),
      )
    }
    private val testLegacyRecallEntity = RecallEntity(
      recallUuid = UUID.randomUUID(),
      prisonerId = DpsDataCreator.DEFAULT_PRISONER_ID,
      revocationDate = LocalDate.of(2024, 1, 1),
      returnToCustodyDate = LocalDate.of(2024, 1, 11),
      inPrisonOnRevocationDate = false,
      recallType = RecallTypeEntity(0, RecallType.LR, "Standard"),
      statusId = RecallEntityStatus.ACTIVE,
      createdByUsername = "FOO",
      source = EventSource.DPS,
    ).apply {
      recallSentences = mutableSetOf(
        RecallSentenceEntity(
          recallSentenceUuid = UUID.randomUUID(),
          sentence = SentenceEntity(
            sentenceUuid = UUID.randomUUID(),
            statusId = SentenceEntityStatus.ACTIVE,
            createdBy = "FOO",
            sentenceServeType = "CONCURRENT",
            consecutiveTo = null,
            sentenceType = testLegacySentenceType,
            supersedingSentence = null,
            charge = testCharge,
            convictionDate = null,
            fineAmount = null,
          ),
          recall = this,
          createdByUsername = "FOO",
        ),
      )
    }
  }
}
