package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
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
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.audit.RecallHistoryEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.audit.SentenceHistoryEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ChargeEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.CourtAppearanceEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.CourtCaseEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.RecallEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.RecallType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ReferenceEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.SentenceEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.SentenceTypeClassification
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.ChargeRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.LegacySentenceTypeRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.PeriodLengthRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.RecallRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.RecallSentenceRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.RecallTypeRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.SentenceRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.SentenceTypeRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.audit.AppearanceChargeHistoryRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.audit.ChargeHistoryRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.audit.PeriodLengthHistoryRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.audit.RecallHistoryRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.audit.RecallSentenceHistoryRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.audit.SentenceHistoryRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacyCreateSentence
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.RecallSentenceLegacyData
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.ServiceUserService
import java.time.LocalDate
import java.util.UUID

class LegacySentenceServiceTests {

  private val sentenceRepository: SentenceRepository = mockk(relaxed = true)
  private val chargeRepository: ChargeRepository = mockk(relaxed = true)
  private val sentenceTypeRepository: SentenceTypeRepository = mockk(relaxed = true)
  private val serviceUserService: ServiceUserService = mockk(relaxed = true)
  private val sentenceHistoryRepository: SentenceHistoryRepository = mockk(relaxed = true)
  private val legacySentenceTypeRepository: LegacySentenceTypeRepository = mockk(relaxed = true)
  private val recallTypeRepository: RecallTypeRepository = mockk(relaxed = true)
  private val recallRepository: RecallRepository = mockk(relaxed = true)
  private val recallSentenceRepository: RecallSentenceRepository = mockk(relaxed = true)
  private val periodLengthRepository: PeriodLengthRepository = mockk(relaxed = true)
  private val periodLengthHistoryRepository: PeriodLengthHistoryRepository = mockk(relaxed = true)
  private val legacyPeriodLengthService: LegacyPeriodLengthService = mockk(relaxed = true)
  private val chargeHistoryRepository: ChargeHistoryRepository = mockk(relaxed = true)
  private val appearanceChargeHistoryRepository: AppearanceChargeHistoryRepository = mockk(relaxed = true)
  private val recallHistoryRepository: RecallHistoryRepository = mockk(relaxed = true)
  private val recallSentenceHistoryRepository: RecallSentenceHistoryRepository = mockk(relaxed = true)

  private val service = LegacySentenceService(
    sentenceRepository = sentenceRepository,
    chargeRepository = chargeRepository,
    sentenceTypeRepository = sentenceTypeRepository,
    serviceUserService = serviceUserService,
    sentenceHistoryRepository = sentenceHistoryRepository,
    legacySentenceTypeRepository = legacySentenceTypeRepository,
    recallTypeRepository = recallTypeRepository,
    recallRepository = recallRepository,
    recallSentenceRepository = recallSentenceRepository,
    periodLengthRepository = periodLengthRepository,
    periodLengthHistoryRepository = periodLengthHistoryRepository,
    legacyPeriodLengthService = legacyPeriodLengthService,
    chargeHistoryRepository = chargeHistoryRepository,
    appearanceChargeHistoryRepository = appearanceChargeHistoryRepository,
    recallHistoryRepository = recallHistoryRepository,
    recallSentenceHistoryRepository = recallSentenceHistoryRepository,
  )

  @Test
  fun `update does not update RTC or write recall history when latest recall is not FTR`() {
    val sentenceUuid = UUID.randomUUID()
    val chargeUuid = UUID.randomUUID()
    val appearanceUuid = UUID.randomUUID()

    val existingRtc = LocalDate.of(2024, 1, 11)
    val incomingRtc = LocalDate.of(2024, 2, 1)

    val (existingSentence, recall) = getSentenceAndRecall(
      sentenceUuid = sentenceUuid,
      chargeUuid = chargeUuid,
      appearanceUuid = appearanceUuid,
      recallType = RecallType.LR,
      existingRtc = existingRtc,
    )

    stubMocks(sentenceUuid, chargeUuid, existingSentence)

    val legacySentence = legacySentenceUpdate(chargeUuid, appearanceUuid, incomingRtc)

    service.update(sentenceUuid, legacySentence)

    assertThat(recall.returnToCustodyDate).isEqualTo(existingRtc)
    verify(exactly = 0) { recallHistoryRepository.save(any()) }
    verify(exactly = 0) { recallSentenceHistoryRepository.save(any()) }
  }

  @Test
  fun `update updates RTC and writes recall history when latest recall is FTR and RTC differs`() {
    val sentenceUuid = UUID.randomUUID()
    val chargeUuid = UUID.randomUUID()
    val appearanceUuid = UUID.randomUUID()

    val existingRtc = LocalDate.of(2024, 1, 11)
    val incomingRtc = LocalDate.of(2024, 2, 1)

    val (existingSentence, recall) = getSentenceAndRecall(
      sentenceUuid = sentenceUuid,
      chargeUuid = chargeUuid,
      appearanceUuid = appearanceUuid,
      recallType = RecallType.FTR_14,
      existingRtc = existingRtc,
    )

    stubMocks(sentenceUuid, chargeUuid, existingSentence)

    every { sentenceHistoryRepository.save(any()) } returns mockk(relaxed = true)
    every { recallHistoryRepository.save(any()) } returns mockk(relaxed = true)
    every { recallSentenceHistoryRepository.save(any()) } returns mockk(relaxed = true)

    val legacySentence = legacySentenceUpdate(chargeUuid, appearanceUuid, incomingRtc)

    service.update(sentenceUuid, legacySentence)

    assertThat(recall.returnToCustodyDate).isEqualTo(incomingRtc)
    verify(exactly = 1) { recallHistoryRepository.save(any()) }
    verify(exactly = 1) { recallSentenceHistoryRepository.save(any()) }
  }

  private fun stubMocks(sentenceUuid: UUID, chargeUuid: UUID, existingSentence: SentenceEntity) {
    every { sentenceRepository.findFirstBySentenceUuidAndStatusIdNotOrderByUpdatedAtDesc(sentenceUuid) } returns existingSentence
    every { sentenceRepository.findBySentenceUuidAndChargeChargeUuidNotInAndStatusIdNot(sentenceUuid, any()) } returns emptyList()
    every {
      sentenceRepository.findBySentenceUuidAndChargeUuidsAndNotAppearanceUuidAndStatusIdNot(
        sentenceUuid,
        any(),
        any(),
      )
    } returns emptyList()
    every {
      sentenceRepository.findFirstBySentenceUuidAndChargeChargeUuidOrderByUpdatedAtDesc(
        sentenceUuid,
        chargeUuid,
      )
    } returns existingSentence

    every { periodLengthRepository.findAllBySentenceEntitySentenceUuidAndStatusIdNot(sentenceUuid) } returns emptyList()
    every { serviceUserService.getUsername() } returns "SYNC_USER"

    every { sentenceHistoryRepository.save(any()) } returns mockk(relaxed = true)
    every { recallHistoryRepository.save(any()) } returns mockk(relaxed = true)
    every { recallSentenceHistoryRepository.save(any()) } returns mockk(relaxed = true)
  }

  @Test
  fun `delete saves recallHistory after deleting so status should be DELETED im history table`() {
    val sentenceUuid = UUID.randomUUID()
    val chargeUuid = UUID.randomUUID()
    val appearanceUuid = UUID.randomUUID()

    val (existingSentence, _) = getSentenceAndRecall(
      sentenceUuid = sentenceUuid,
      chargeUuid = chargeUuid,
      appearanceUuid = appearanceUuid,
      recallType = RecallType.LR,
      existingRtc = LocalDate.of(2024, 1, 11),
    )

    every { sentenceHistoryRepository.save(any()) } returns mockk<SentenceHistoryEntity>(relaxed = true)
    every { recallHistoryRepository.save(any()) } returns mockk<RecallHistoryEntity>(relaxed = true)

    val recallHistorySlot = slot<RecallHistoryEntity>()

    service.delete(existingSentence, "SYNC_USER")

    verify {
      recallHistoryRepository.save(capture(recallHistorySlot))
    }

    assertThat(recallHistorySlot.captured.status).isEqualTo(RecallEntityStatus.DELETED)
  }

  private companion object {

    fun legacySentenceUpdate(chargeUuid: UUID, appearanceUuid: UUID, rtc: LocalDate): LegacyCreateSentence = LegacyCreateSentence(
      chargeUuids = listOf(chargeUuid),
      appearanceUuid = appearanceUuid,
      consecutiveToLifetimeUuid = null,
      active = true,
      performedByUser = "SYNC_USER",
      returnToCustodyDate = rtc,
      legacyData = mockk(relaxed = true),
    )

    fun getSentenceAndRecall(
      sentenceUuid: UUID,
      chargeUuid: UUID,
      appearanceUuid: UUID,
      recallType: RecallType,
      existingRtc: LocalDate,
    ): Pair<SentenceEntity, RecallEntity> {
      val recall = RecallEntity(
        recallUuid = UUID.randomUUID(),
        prisonerId = "A1234BC",
        revocationDate = LocalDate.of(2024, 1, 1),
        returnToCustodyDate = existingRtc,
        inPrisonOnRevocationDate = false,
        recallType = RecallTypeEntity(0, recallType, recallType.name),
        status = RecallEntityStatus.ACTIVE,
        createdByUsername = "SYNC_USER",
        source = EventSource.NOMIS,
      )

      val sentenceType = SentenceTypeEntity(
        sentenceTypeUuid = UUID.randomUUID(),
        description = "Any sentence type",
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

      val courtCase = CourtCaseEntity(
        caseUniqueIdentifier = "CASE123",
        prisonerId = "A1234BC",
        statusId = CourtCaseEntityStatus.ACTIVE,
        createdBy = "SYNC_USER",
      )

      val courtAppearance = CourtAppearanceEntity(
        id = 1,
        appearanceUuid = appearanceUuid,
        courtCase = courtCase,
        courtCode = "COURT1",
        appearanceDate = LocalDate.of(2024, 1, 1),
        statusId = CourtAppearanceEntityStatus.ACTIVE,
        createdBy = "SYNC_USER",
        createdPrison = "MDI",
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

      val charge = ChargeEntity(
        chargeUuid = chargeUuid,
        offenceCode = "TEST123",
        statusId = ChargeEntityStatus.ACTIVE,
        createdBy = "SYNC_USER",
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
            courtAppearanceEntity = courtAppearance,
            chargeEntity = this,
            createdBy = "SYNC_USER",
            createdPrison = "MDI",
          ),
        )
      }

      val existingSentence = SentenceEntity(
        sentenceUuid = sentenceUuid,
        statusId = SentenceEntityStatus.ACTIVE,
        createdBy = "SYNC_USER",
        sentenceServeType = "CONCURRENT",
        consecutiveTo = null,
        sentenceType = sentenceType,
        supersedingSentence = null,
        charge = charge,
        convictionDate = null,
        fineAmount = null,
      )

      val recallSentence = RecallSentenceEntity(
        recallSentenceUuid = UUID.randomUUID(),
        sentence = existingSentence,
        recall = recall,
        createdByUsername = "SYNC_USER",
        preRecallSentenceStatus = SentenceEntityStatus.INACTIVE,
        legacyData = RecallSentenceLegacyData.from(mockk(relaxed = true)),
      )

      recall.recallSentences = mutableSetOf(recallSentence)
      existingSentence.recallSentences = mutableSetOf(recallSentence)

      return existingSentence to recall
    }
  }
}
