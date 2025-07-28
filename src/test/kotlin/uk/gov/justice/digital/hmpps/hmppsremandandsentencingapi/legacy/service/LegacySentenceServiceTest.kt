package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.service

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.ChargeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.LegacySentenceTypeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.RecallEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.RecallSentenceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.RecallTypeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.SentenceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.SentenceTypeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.audit.SentenceHistoryEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.RecallType
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
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.SentenceLegacyData
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.ServiceUserService
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.UUID

class LegacySentenceServiceTest {

  private val sentenceRepository = mockk<SentenceRepository>()
  private val chargeRepository = mockk<ChargeRepository>()
  private val sentenceTypeRepository = mockk<SentenceTypeRepository>()
  private val serviceUserService = mockk<ServiceUserService>()
  private val sentenceHistoryRepository = mockk<SentenceHistoryRepository>()
  private val legacySentenceTypeRepository = mockk<LegacySentenceTypeRepository>()
  private val recallTypeRepository = mockk<RecallTypeRepository>()
  private val recallRepository = mockk<RecallRepository>()
  private val recallSentenceRepository = mockk<RecallSentenceRepository>()
  private val periodLengthRepository = mockk<PeriodLengthRepository>()
  private val periodLengthHistoryRepository = mockk<PeriodLengthHistoryRepository>()
  private val legacyPeriodLengthService = mockk<LegacyPeriodLengthService>()
  private val chargeHistoryRepository = mockk<ChargeHistoryRepository>()
  private val appearanceChargeHistoryRepository = mockk<AppearanceChargeHistoryRepository>()
  private val recallHistoryRepository = mockk<RecallHistoryRepository>()
  private val recallSentenceHistoryRepository = mockk<RecallSentenceHistoryRepository>()

  private lateinit var service: LegacySentenceService

  private val recallSentenceTypeBucketUuid = UUID.fromString("f9a1551e-86b1-425b-96f7-23465a0f05fc")
  private val testPrisonerId = "A1234BC"
  private val testUsername = "testuser"

  @BeforeEach
  fun setup() {
    service = LegacySentenceService(
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

    every { serviceUserService.getUsername() } returns testUsername
    every { chargeHistoryRepository.save(any()) } returns mockk()
    every { appearanceChargeHistoryRepository.save(any()) } returns mockk()
  }

  @Test
  fun `createRecall should create new recall when none exists`() {
    // Given
    val chargeUuid = UUID.randomUUID()
    val appearanceUuid = UUID.randomUUID()
    val sentenceUuid = UUID.randomUUID()
    val newRecallUuid = UUID.randomUUID()
    
    val mockCharge = createMockCharge(chargeUuid, appearanceUuid, testPrisonerId)
    val mockSentence = createMockSentence(sentenceUuid, mockCharge)
    val mockSentenceType = createMockSentenceType(recallSentenceTypeBucketUuid)
    val mockRecallType = createMockRecallType()
    val mockNewRecall = createMockRecall(newRecallUuid, testPrisonerId)
    val mockRecallSentence = mockk<RecallSentenceEntity>()
    
    val legacySentence = createLegacySentence(listOf(chargeUuid), appearanceUuid)
    
    // When
    every { chargeRepository.findFirstByAppearanceChargesAppearanceAppearanceUuidAndChargeUuidAndStatusIdNotOrderByCreatedAtDesc(appearanceUuid, chargeUuid) } returns mockCharge
    every { sentenceRepository.save(any()) } returns mockSentence
    every { sentenceHistoryRepository.save(any()) } returns mockk()
    every { sentenceTypeRepository.findBySentenceTypeUuid(recallSentenceTypeBucketUuid) } returns mockSentenceType
    every { legacySentenceTypeRepository.findByNomisSentenceTypeReferenceAndSentencingAct(any(), any()) } returns null
    every { recallTypeRepository.findOneByCode(RecallType.LR) } returns mockRecallType
    every { recallRepository.findFirstByPrisonerIdAndStatusIdOrderByCreatedAtDesc(testPrisonerId, EntityStatus.ACTIVE) } returns null
    every { recallRepository.save(any()) } returns mockNewRecall
    every { recallSentenceRepository.save(any()) } returns mockRecallSentence
    
    // Then
    val result = service.create(legacySentence)
    
    assertThat(result).hasSize(1)
    
    // Verify that a new recall was created
    verify(exactly = 1) { recallRepository.findFirstByPrisonerIdAndStatusIdOrderByCreatedAtDesc(testPrisonerId, EntityStatus.ACTIVE) }
    verify(exactly = 1) { recallRepository.save(any()) }
    verify(exactly = 1) { recallSentenceRepository.save(any()) }
  }

  @Test
  fun `createRecall should reuse existing recall when one exists`() {
    // Given
    val chargeUuid = UUID.randomUUID()
    val appearanceUuid = UUID.randomUUID()
    val sentenceUuid = UUID.randomUUID()
    val existingRecallUuid = UUID.randomUUID()
    
    val mockCharge = createMockCharge(chargeUuid, appearanceUuid, testPrisonerId)
    val mockSentence = createMockSentence(sentenceUuid, mockCharge)
    val mockSentenceType = createMockSentenceType(recallSentenceTypeBucketUuid)
    val mockRecallType = createMockRecallType()
    val mockExistingRecall = createMockRecall(existingRecallUuid, testPrisonerId)
    val mockRecallSentence = mockk<RecallSentenceEntity>()
    
    val legacySentence = createLegacySentence(listOf(chargeUuid), appearanceUuid)
    
    // When
    every { chargeRepository.findFirstByAppearanceChargesAppearanceAppearanceUuidAndChargeUuidAndStatusIdNotOrderByCreatedAtDesc(appearanceUuid, chargeUuid) } returns mockCharge
    every { sentenceRepository.save(any()) } returns mockSentence
    every { sentenceHistoryRepository.save(any()) } returns mockk()
    every { sentenceTypeRepository.findBySentenceTypeUuid(recallSentenceTypeBucketUuid) } returns mockSentenceType
    every { legacySentenceTypeRepository.findByNomisSentenceTypeReferenceAndSentencingAct(any(), any()) } returns null
    every { recallTypeRepository.findOneByCode(RecallType.LR) } returns mockRecallType
    every { recallRepository.findFirstByPrisonerIdAndStatusIdOrderByCreatedAtDesc(testPrisonerId, EntityStatus.ACTIVE) } returns mockExistingRecall
    every { recallSentenceRepository.save(any()) } returns mockRecallSentence
    
    // Then
    val result = service.create(legacySentence)
    
    assertThat(result).hasSize(1)
    
    // Verify that existing recall was found and no new recall was created
    verify(exactly = 1) { recallRepository.findFirstByPrisonerIdAndStatusIdOrderByCreatedAtDesc(testPrisonerId, EntityStatus.ACTIVE) }
    verify(exactly = 0) { recallRepository.save(any()) }
    verify(exactly = 1) { recallSentenceRepository.save(any()) }
  }

  @Test
  fun `createRecall should handle multiple charges with same recall`() {
    // Given
    val chargeUuid1 = UUID.randomUUID()
    val chargeUuid2 = UUID.randomUUID()
    val appearanceUuid = UUID.randomUUID()
    val sentenceUuid = UUID.randomUUID()
    val recallUuid = UUID.randomUUID()
    
    val mockCharge1 = createMockCharge(chargeUuid1, appearanceUuid, testPrisonerId)
    val mockCharge2 = createMockCharge(chargeUuid2, appearanceUuid, testPrisonerId)
    val mockSentence1 = createMockSentence(sentenceUuid, mockCharge1)
    val mockSentence2 = createMockSentence(sentenceUuid, mockCharge2)
    val mockSentenceType = createMockSentenceType(recallSentenceTypeBucketUuid)
    val mockRecallType = createMockRecallType()
    val mockRecall = createMockRecall(recallUuid, testPrisonerId)
    val mockRecallSentence = mockk<RecallSentenceEntity>()
    
    val legacySentence = createLegacySentence(listOf(chargeUuid1, chargeUuid2), appearanceUuid)
    
    // When
    every { chargeRepository.findFirstByAppearanceChargesAppearanceAppearanceUuidAndChargeUuidAndStatusIdNotOrderByCreatedAtDesc(appearanceUuid, chargeUuid1) } returns mockCharge1
    every { chargeRepository.findFirstByAppearanceChargesAppearanceAppearanceUuidAndChargeUuidAndStatusIdNotOrderByCreatedAtDesc(appearanceUuid, chargeUuid2) } returns mockCharge2
    every { sentenceRepository.save(any()) } returnsMany listOf(mockSentence1, mockSentence2)
    every { sentenceHistoryRepository.save(any()) } returns mockk()
    every { sentenceTypeRepository.findBySentenceTypeUuid(recallSentenceTypeBucketUuid) } returns mockSentenceType
    every { legacySentenceTypeRepository.findByNomisSentenceTypeReferenceAndSentencingAct(any(), any()) } returns null
    every { recallTypeRepository.findOneByCode(RecallType.LR) } returns mockRecallType
    // First call returns null (no existing recall), subsequent calls return the created recall
    every { recallRepository.findFirstByPrisonerIdAndStatusIdOrderByCreatedAtDesc(testPrisonerId, EntityStatus.ACTIVE) } returnsMany listOf(null, mockRecall)
    every { recallRepository.save(any()) } returns mockRecall
    every { recallSentenceRepository.save(any()) } returns mockRecallSentence
    
    // Then
    val result = service.create(legacySentence)
    
    assertThat(result).hasSize(2)
    
    // Verify that recall was created only once for the first charge
    verify(exactly = 2) { recallRepository.findFirstByPrisonerIdAndStatusIdOrderByCreatedAtDesc(testPrisonerId, EntityStatus.ACTIVE) }
    verify(exactly = 1) { recallRepository.save(any()) }
    verify(exactly = 2) { recallSentenceRepository.save(any()) }
  }

  // Helper methods
  private fun createMockCharge(chargeUuid: UUID, appearanceUuid: UUID, prisonerId: String): ChargeEntity {
    val mockCharge = mockk<ChargeEntity>(relaxed = true)
    every { mockCharge.chargeUuid } returns chargeUuid
    every { mockCharge.getActiveOrInactiveSentence() } returns null
    every { mockCharge.sentences } returns mutableSetOf()
    every { mockCharge.appearanceCharges } returns mutableSetOf(
      mockk {
        every { appearance } returns mockk {
          every { this@mockk.appearanceUuid } returns appearanceUuid
          every { appearanceDate } returns LocalDate.now()
          every { statusId } returns EntityStatus.ACTIVE
          every { courtCase } returns mockk {
            every { this@mockk.prisonerId } returns prisonerId
            every { caseUniqueIdentifier } returns "CASE123"
          }
        }
      }
    )
    return mockCharge
  }

  private fun createMockSentence(sentenceUuid: UUID, charge: ChargeEntity): SentenceEntity {
    val mockSentence = mockk<SentenceEntity>(relaxed = true)
    every { mockSentence.sentenceUuid } returns sentenceUuid
    every { mockSentence.charge } returns charge
    return mockSentence
  }

  private fun createMockSentenceType(sentenceTypeUuid: UUID): SentenceTypeEntity {
    val mockSentenceType = mockk<SentenceTypeEntity>()
    every { mockSentenceType.sentenceTypeUuid } returns sentenceTypeUuid
    return mockSentenceType
  }

  private fun createMockRecallType(): RecallTypeEntity {
    val mockRecallType = mockk<RecallTypeEntity>()
    every { mockRecallType.code } returns RecallType.LR
    return mockRecallType
  }

  private fun createMockRecall(recallUuid: UUID, prisonerId: String): RecallEntity {
    val mockRecall = mockk<RecallEntity>()
    every { mockRecall.recallUuid } returns recallUuid
    every { mockRecall.prisonerId } returns prisonerId
    every { mockRecall.statusId } returns EntityStatus.ACTIVE
    every { mockRecall.createdAt } returns ZonedDateTime.now()
    return mockRecall
  }

  private fun createLegacySentence(chargeUuids: List<UUID>, appearanceUuid: UUID): LegacyCreateSentence {
    return LegacyCreateSentence(
      chargeUuids = chargeUuids,
      appearanceUuid = appearanceUuid,
      fine = null,
      consecutiveToLifetimeUuid = null,
      active = true,
      legacyData = SentenceLegacyData(
        sentenceCalcType = "FTR_ORA",
        sentenceCategory = "2020",
        sentenceTypeDesc = "Fixed Term Recall",
        postedDate = LocalDate.now().toString(),
        active = true,
        nomisLineReference = "1",
        bookingId = 12345L
      ),
      returnToCustodyDate = LocalDate.now()
    )
  }
}