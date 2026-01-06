package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.AdjustmentsApiClient
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.dto.AdjustmentDto
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.dto.UnlawfullyAtLargeDto
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateRecall
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
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.audit.SentenceHistoryEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ChargeEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.CourtAppearanceEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.CourtCaseEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.RecallEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.RecallType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.RecallType.FTR_14
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ReferenceEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.SentenceEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.SentenceTypeClassification
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.RecallRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.RecallSentenceRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.RecallTypeRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.SentenceRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.audit.RecallHistoryRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.audit.RecallSentenceHistoryRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.audit.SentenceHistoryRepository
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
  private val sentenceHistoryRepository: SentenceHistoryRepository = mockk(relaxed = true)
  private val serviceUserService: ServiceUserService = mockk(relaxed = true)

  private val service = RecallService(
    recallRepository,
    recallSentenceRepository,
    recallTypeRepository,
    sentenceRepository,
    sentenceService,
    recallHistoryRepository,
    recallSentenceHistoryRepository,
    adjustmentsApiClient,
    sentenceHistoryRepository,
    serviceUserService,
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

  @Test
  fun `delete a dps recall restores sentence status, writes history and deletes recall sentence`() {
    val recallUuid = UUID.randomUUID()

    val sentence = SentenceEntity(
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
    )

    val recall = RecallEntity(
      recallUuid = recallUuid,
      prisonerId = DpsDataCreator.DEFAULT_PRISONER_ID,
      revocationDate = LocalDate.of(2024, 1, 1),
      returnToCustodyDate = LocalDate.of(2024, 1, 11),
      inPrisonOnRevocationDate = false,
      recallType = RecallTypeEntity(0, RecallType.LR, "Standard"),
      statusId = RecallEntityStatus.ACTIVE,
      createdByUsername = "FOO",
      source = EventSource.DPS,
    )

    val recallSentence = RecallSentenceEntity(
      recallSentenceUuid = UUID.randomUUID(),
      sentence = sentence,
      recall = recall,
      createdByUsername = "FOO",
      preRecallSentenceStatus = SentenceEntityStatus.INACTIVE,
    )

    recall.recallSentences = mutableSetOf(recallSentence)

    every { recallRepository.findOneByRecallUuid(recallUuid) } returns recall
    every { recallRepository.save(any()) } returns recall
    every { recallHistoryRepository.save(any()) } returns mockk()
    every { recallSentenceHistoryRepository.save(any()) } returns mockk()
    every { serviceUserService.getUsername() } returns "DELETE RECALL"

    val sentenceHistory = slot<SentenceHistoryEntity>()
    every { sentenceHistoryRepository.save(capture(sentenceHistory)) } answers { firstArg() }

    service.deleteRecall(recallUuid)

    assertThat(sentence.statusId).isEqualTo(SentenceEntityStatus.INACTIVE)
    assertThat(sentence.updatedBy).isEqualTo("DELETE RECALL")
    assertThat(sentenceHistory.captured.statusId).isEqualTo(SentenceEntityStatus.INACTIVE)
    verify { recallSentenceRepository.delete(recallSentence) }
  }

  @Nested
  inner class CreateRecallTests {
    @Test
    fun `create recall stores pre recall sentence status`() {
      val sentenceUuid = UUID.randomUUID()
      val sentence = SentenceEntity(
        sentenceUuid = sentenceUuid,
        statusId = SentenceEntityStatus.INACTIVE,
        createdBy = "FOO",
        sentenceServeType = "CONCURRENT",
        consecutiveTo = null,
        sentenceType = testStandardSentenceType,
        supersedingSentence = null,
        charge = testCharge,
        convictionDate = null,
        fineAmount = null,
      )

      every { recallTypeRepository.findOneByCode(any()) } returns
        RecallTypeEntity(0, RecallType.LR, "Standard")
      every { recallRepository.save(any()) } answers { firstArg() }
      every { sentenceRepository.findBySentenceUuidIn(any()) } returns listOf(sentence)
      val recallSentenceSaved = slot<RecallSentenceEntity>()
      val sentenceHistory = slot<SentenceHistoryEntity>()
      every { recallSentenceRepository.save(capture(recallSentenceSaved)) } answers { firstArg() }
      every { recallHistoryRepository.save(any()) } answers { firstArg() }
      every { recallSentenceHistoryRepository.save(any()) } answers { firstArg() }
      every { sentenceHistoryRepository.save(capture(sentenceHistory)) } answers { firstArg() }

      service.createRecall(
        baseRecall.copy(sentenceIds = listOf(sentenceUuid)),
      )

      assertThat(recallSentenceSaved.captured.preRecallSentenceStatus).isEqualTo(SentenceEntityStatus.INACTIVE)
      assertThat(sentence.statusId).isEqualTo(SentenceEntityStatus.ACTIVE)
      assertThat(sentence.createdBy).isEqualTo("FOO")
      assertThat(sentenceHistory.captured.statusId).isEqualTo(SentenceEntityStatus.ACTIVE)
    }
  }

  @Nested
  inner class UpdateRecallTests {
    @Test
    fun `update a dps recall happy path, updates correct fields`() {
      val recallUuid = testNonLegacyRecallEntity.recallUuid
      every { recallRepository.findOneByRecallUuid(recallUuid) } returns testNonLegacyRecallEntity
      every { recallRepository.save(any()) } returns testNonLegacyRecallEntity
      every { recallHistoryRepository.save(any()) } returns mockk()
      every { recallSentenceHistoryRepository.save(any()) } returns mockk()
      val recallToUpdate = baseRecall.copy(inPrisonOnRevocationDate = true)

      service.updateRecall(recallUuid, baseRecall.copy(inPrisonOnRevocationDate = true))

      verify(exactly = 1) {
        recallRepository.save(
          match { saved ->
            saved.prisonerId == "PRI123" &&
              saved.revocationDate == recallToUpdate.revocationDate &&
              saved.returnToCustodyDate == recallToUpdate.returnToCustodyDate &&
              saved.inPrisonOnRevocationDate == recallToUpdate.inPrisonOnRevocationDate
          },
        )
      }
    }

    @Test
    fun `update a dps recall when removing one sentence and adding another updates recall sentences and writes history`() {
      // recall starts with TWO sentences
      val recallUuid = UUID.randomUUID()

      val keptSentenceUuid = UUID.randomUUID()
      val removedSentenceUuid = UUID.randomUUID()

      val keptSentence = SentenceEntity(
        sentenceUuid = keptSentenceUuid,
        statusId = SentenceEntityStatus.ACTIVE,
        createdBy = "FOO",
        sentenceServeType = "CONCURRENT",
        consecutiveTo = null,
        sentenceType = testStandardSentenceType,
        supersedingSentence = null,
        charge = testCharge,
        convictionDate = null,
        fineAmount = null,
      )

      val removedSentence = SentenceEntity(
        sentenceUuid = removedSentenceUuid,
        statusId = SentenceEntityStatus.ACTIVE,
        createdBy = "FOO",
        sentenceServeType = "CONCURRENT",
        consecutiveTo = null,
        sentenceType = testStandardSentenceType,
        supersedingSentence = null,
        charge = testCharge,
        convictionDate = null,
        fineAmount = null,
      )

      val recallToUpdate = RecallEntity(
        recallUuid = recallUuid,
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
            sentence = keptSentence,
            recall = this,
            createdByUsername = "FOO",
          ),
          RecallSentenceEntity(
            recallSentenceUuid = UUID.randomUUID(),
            sentence = removedSentence,
            recall = this,
            createdByUsername = "FOO",
            preRecallSentenceStatus = SentenceEntityStatus.INACTIVE,
          ),
        )
      }

      // New sentence to add
      val newSentenceUuid = UUID.randomUUID()
      val newSentence = SentenceEntity(
        sentenceUuid = newSentenceUuid,
        statusId = SentenceEntityStatus.INACTIVE,
        createdBy = "FOO",
        sentenceServeType = "CONCURRENT",
        consecutiveTo = null,
        sentenceType = testStandardSentenceType,
        supersedingSentence = null,
        charge = testCharge,
        convictionDate = null,
        fineAmount = null,
      )

      every { recallRepository.findOneByRecallUuid(recallUuid) } returns recallToUpdate
      every { recallHistoryRepository.save(any()) } returns mockk()
      every { recallSentenceHistoryRepository.save(any()) } returns mockk()
      every { recallTypeRepository.findOneByCode(any()) } returns RecallTypeEntity(0, RecallType.LR, "Standard")
      every { sentenceRepository.findBySentenceUuidIn(match { it == listOf(newSentenceUuid) }) } returns listOf(newSentence)

      val deletedRecallSentenceSlot = slot<RecallSentenceEntity>()
      every { recallSentenceRepository.delete(capture(deletedRecallSentenceSlot)) } returns Unit

      val sentenceHistorySaves = mutableListOf<SentenceHistoryEntity>()
      every { sentenceHistoryRepository.save(capture(sentenceHistorySaves)) } answers { firstArg() }
      every { recallSentenceRepository.save(any()) } answers {
        val saved = firstArg<RecallSentenceEntity>()
        recallToUpdate.recallSentences.add(saved)
        saved
      }
      val savedRecallSlot = slot<RecallEntity>()
      every { recallRepository.save(capture(savedRecallSlot)) } answers { firstArg() }

      service.updateRecall(
        recallUuid,
        baseRecall.copy(
          sentenceIds = listOf(keptSentenceUuid, newSentenceUuid),
          createdByUsername = "user001",
          createdByPrison = "PRI",
        ),
      )

      assertThat(deletedRecallSentenceSlot.isCaptured).isTrue()
      assertThat(deletedRecallSentenceSlot.captured.sentence.sentenceUuid).isEqualTo(removedSentenceUuid)

      // recall now contains kept + new (and not the removed)
      val savedRecall = savedRecallSlot.captured
      val savedSentenceUuids = savedRecall.recallSentences.map { it.sentence.sentenceUuid }
      assertThat(savedSentenceUuids).contains(keptSentenceUuid)
      assertThat(savedSentenceUuids).contains(newSentenceUuid)
      assertThat(savedSentenceUuids).doesNotContain(removedSentenceUuid)

      // removed and new sentences status correct
      assertThat(removedSentence.statusId).isEqualTo(SentenceEntityStatus.INACTIVE)
      assertThat(newSentence.statusId).isEqualTo(SentenceEntityStatus.ACTIVE)
      assertThat(newSentence.updatedBy).isEqualTo("user001")
      assertThat(newSentence.updatedPrison).isEqualTo("PRI")

      assertThat(sentenceHistorySaves.map { it.statusId }).contains(SentenceEntityStatus.ACTIVE)
      assertThat(sentenceHistorySaves.map { it.statusId }).contains(SentenceEntityStatus.INACTIVE)
    }
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

    private val testStandardSentenceType = SentenceTypeEntity(
      sentenceTypeUuid = UUID.randomUUID(),
      description = "Life sentence",
      classification = SentenceTypeClassification.STANDARD,
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

    val baseRecall = CreateRecall(
      prisonerId = "PRI123",
      revocationDate = LocalDate.of(2024, 1, 2),
      returnToCustodyDate = LocalDate.of(2024, 1, 2),
      inPrisonOnRevocationDate = false,
      recallTypeCode = FTR_14,
      createdByUsername = "user001",
      createdByPrison = "PRI",
    )
  }
}
