package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateCharge
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateSentence
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.RecordResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.ChargeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.CourtAppearanceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.AppearanceOutcomeRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.AppearanceTypeRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.CourtAppearanceRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.CourtCaseRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.NextCourtAppearanceRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.audit.AppearanceChargeHistoryRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.audit.CourtAppearanceHistoryRepository
import java.time.LocalDate
import java.util.UUID

class CourtAppearanceServiceTests {
  private val courtCaseRepository = mockk<CourtCaseRepository>()
  private val courtAppearanceRepository = mockk<CourtAppearanceRepository>()
  private val serviceUserService = mockk<ServiceUserService>()
  private val courtAppearanceHistoryRepository = mockk<CourtAppearanceHistoryRepository>()
  private val nextCourtAppearanceRepository = mockk<NextCourtAppearanceRepository>()
  private val appearanceOutcomeRepository = mockk<AppearanceOutcomeRepository>()
  private val periodLengthService = mockk<PeriodLengthService>()
  private val chargeService = mockk<ChargeService>(relaxed = true)
  private val appearanceTypeRepository = mockk<AppearanceTypeRepository>()
  private val appearanceChargeHistoryRepository = mockk<AppearanceChargeHistoryRepository>()
  private val fixManyChargesToSentenceService = mockk<FixManyChargesToSentenceService>()
  private val documentService = mockk<UploadedDocumentService>()
  private val courtAppearanceService = CourtAppearanceService(
    courtAppearanceRepository = courtAppearanceRepository,
    nextCourtAppearanceRepository = nextCourtAppearanceRepository,
    appearanceOutcomeRepository = appearanceOutcomeRepository,
    periodLengthService = periodLengthService,
    chargeService = chargeService,
    serviceUserService = serviceUserService,
    courtCaseRepository = courtCaseRepository,
    appearanceTypeRepository = appearanceTypeRepository,
    courtAppearanceHistoryRepository = courtAppearanceHistoryRepository,
    appearanceChargeHistoryRepository = appearanceChargeHistoryRepository,
    fixManyChargesToSentenceService = fixManyChargesToSentenceService,
    documentService = documentService,
  )

  @Nested
  inner class OrderChargesByConsecutiveChain {
    @Test
    fun `should sort charges when passed in order is mixed`() {
      val c1 = createCharge(sentenceUuid = UUID.randomUUID(), consecutiveToUuid = null)
      val c2 = createCharge(sentenceUuid = UUID.randomUUID(), consecutiveToUuid = c1.sentence!!.sentenceUuid)
      val c3 = createCharge(sentenceUuid = UUID.randomUUID(), consecutiveToUuid = c2.sentence!!.sentenceUuid)
      val c4 = createCharge(sentenceUuid = UUID.randomUUID(), consecutiveToUuid = c3.sentence!!.sentenceUuid)

      val unorderedList = listOf(c4, c2, c3, c1)

      val sorted = courtAppearanceService.orderChargesByConsecutiveChain(unorderedList)

      val actual = sorted.map { it.sentence!!.sentenceUuid }
      assertThat(actual).containsExactly(c1.sentence.sentenceUuid, c2.sentence.sentenceUuid, c3.sentence.sentenceUuid, c4.sentence!!.sentenceUuid)
    }

    @Test
    fun `should sort charges with two independent chains`() {
      val c1 = createCharge(sentenceUuid = uuid(1), consecutiveToUuid = null)
      val c2 = createCharge(sentenceUuid = uuid(2), consecutiveToUuid = uuid(1))
      val c3 = createCharge(sentenceUuid = uuid(3), consecutiveToUuid = uuid(2))
      val c4 = createCharge(sentenceUuid = uuid(4), consecutiveToUuid = uuid(3))

      val c5 = createCharge(sentenceUuid = uuid(5), consecutiveToUuid = null)
      val c6 = createCharge(sentenceUuid = uuid(6), consecutiveToUuid = uuid(5))
      val c7 = createCharge(sentenceUuid = uuid(7), consecutiveToUuid = uuid(6))
      val c8 = createCharge(sentenceUuid = uuid(8), consecutiveToUuid = uuid(7))

      val mixedList = listOf(c6, c2, c4, c5, c1, c8, c3, c7)

      val sorted = courtAppearanceService.orderChargesByConsecutiveChain(mixedList)
      val refs = sorted.map { it.sentence!!.sentenceUuid }

      assertThat(refs).containsExactly(uuid(5), uuid(1), uuid(6), uuid(2), uuid(3), uuid(7), uuid(4), uuid(8))
    }
  }

  @Nested
  inner class CreateCharges {
    @Test
    fun `no charges are getting replaced`() {
      val charge = createCharge()
      val charges = listOf(charge)
      val courtAppearance = mockk<CourtAppearanceEntity>()
      val chargeEntity = mockk<ChargeEntity>(relaxed = true)
      every { chargeService.createCharge(charge, any(), any(), any(), any(), any(), null) } returns RecordResponse(chargeEntity, mutableSetOf())
      courtAppearanceService.createCharges(charges, "prisonerId", "courtCaseUuid", courtAppearance, false, null)
      verify(exactly = 1) { chargeService.createCharge(charge, any(), any(), any(), any(), any(), null) }
    }

    @Test
    fun `one charge is getting replaced`() {
      val replacedCharge = createCharge(outcomeUuid = ChargeService.replacedWithAnotherOutcomeUuid)
      val replacingCharge = createCharge(replacingChargeUuid = replacedCharge.chargeUuid)
      val charges = listOf(replacedCharge, replacingCharge)
      val courtAppearance = mockk<CourtAppearanceEntity>()
      val replacedChargeEntity = mockk<ChargeEntity>(relaxed = true)
      val replacingChargeEntity = mockk<ChargeEntity>(relaxed = true)
      every { replacedChargeEntity.chargeUuid } returns replacedCharge.chargeUuid
      every { replacingChargeEntity.supersedingCharge } returns replacedChargeEntity
      every { chargeService.createCharge(replacedCharge, any(), any(), any(), any(), any(), null) } returns RecordResponse(replacedChargeEntity, mutableSetOf())
      every { chargeService.createCharge(replacingCharge, any(), any(), any(), any(), any(), replacedChargeEntity) } returns RecordResponse(replacingChargeEntity, mutableSetOf())
      val result = courtAppearanceService.createCharges(charges, "prisonerId", "courtCaseUuid", courtAppearance, false, null)

      val resultReplacingCharge = result.heardCharges.find { it.record == replacingChargeEntity }
      assertThat(resultReplacingCharge).isNotNull
      assertThat(resultReplacingCharge!!.record.supersedingCharge).isEqualTo(replacedChargeEntity)
      verify(exactly = 1) { chargeService.createCharge(replacingCharge, any(), any(), any(), any(), any(), replacedChargeEntity) }
    }

    @Test
    fun `multiple charges are getting replaced`() {
      val replacedCharge1 = createCharge(outcomeUuid = ChargeService.replacedWithAnotherOutcomeUuid)
      val replacedCharge2 = createCharge(outcomeUuid = ChargeService.replacedWithAnotherOutcomeUuid)
      val replacingCharge1 = createCharge(replacingChargeUuid = replacedCharge1.chargeUuid)
      val replacingCharge2 = createCharge(replacingChargeUuid = replacedCharge2.chargeUuid)
      val charges = listOf(replacedCharge1, replacingCharge1, replacedCharge2, replacingCharge2)
      val courtAppearance = mockk<CourtAppearanceEntity>()
      val replacedChargeEntity1 = mockk<ChargeEntity>(relaxed = true)
      val replacingChargeEntity1 = mockk<ChargeEntity>(relaxed = true)
      val replacedChargeEntity2 = mockk<ChargeEntity>(relaxed = true)
      val replacingChargeEntity2 = mockk<ChargeEntity>(relaxed = true)
      every { replacedChargeEntity1.chargeUuid } returns replacedCharge1.chargeUuid
      every { replacedChargeEntity2.chargeUuid } returns replacedCharge2.chargeUuid
      every { replacingChargeEntity1.supersedingCharge } returns replacedChargeEntity1
      every { replacingChargeEntity2.supersedingCharge } returns replacedChargeEntity2
      every { chargeService.createCharge(replacedCharge1, any(), any(), any(), any(), any(), null) } returns RecordResponse(replacedChargeEntity1, mutableSetOf())
      every { chargeService.createCharge(replacingCharge1, any(), any(), any(), any(), any(), replacedChargeEntity1) } returns RecordResponse(replacingChargeEntity1, mutableSetOf())
      every { chargeService.createCharge(replacedCharge2, any(), any(), any(), any(), any(), null) } returns RecordResponse(replacedChargeEntity2, mutableSetOf())
      every { chargeService.createCharge(replacingCharge2, any(), any(), any(), any(), any(), replacedChargeEntity2) } returns RecordResponse(replacingChargeEntity2, mutableSetOf())
      val result = courtAppearanceService.createCharges(charges, "prisonerId", "courtCaseUuid", courtAppearance, false, null)

      val resultReplacingCharge1 = result.heardCharges.find { it.record == replacingChargeEntity1 }
      assertThat(resultReplacingCharge1).isNotNull
      assertThat(resultReplacingCharge1!!.record.supersedingCharge).isEqualTo(replacedChargeEntity1)
      val resultReplacingCharge2 = result.heardCharges.find { it.record == replacingChargeEntity2 }
      assertThat(resultReplacingCharge2).isNotNull
      assertThat(resultReplacingCharge2!!.record.supersedingCharge).isEqualTo(replacedChargeEntity2)
      verify(exactly = 1) { chargeService.createCharge(replacingCharge1, any(), any(), any(), any(), any(), replacedChargeEntity1) }
      verify(exactly = 1) { chargeService.createCharge(replacingCharge2, any(), any(), any(), any(), any(), replacedChargeEntity2) }
    }
  }

  private fun uuid(i: Long) = UUID(0L, i)

  private fun createCharge(sentenceUuid: UUID? = null, consecutiveToUuid: UUID? = null, replacingChargeUuid: UUID? = null, outcomeUuid: UUID? = null): CreateCharge = CreateCharge(
    appearanceUuid = null,
    offenceCode = "X",
    offenceStartDate = LocalDate.now(),
    offenceEndDate = null,
    outcomeUuid = outcomeUuid,
    terrorRelated = null,
    foreignPowerRelated = null,
    domesticViolenceRelated = null,
    prisonId = "P",
    legacyData = null,
    sentence = sentenceUuid?.let {
      CreateSentence(
        sentenceUuid = it,
        chargeNumber = "1",
        periodLengths = emptyList(),
        sentenceServeType = if (consecutiveToUuid == null) "FORTHWITH" else "CONSECUTIVE",
        consecutiveToSentenceUuid = consecutiveToUuid,
        sentenceTypeId = null,
        convictionDate = null,
        fineAmount = null,
        prisonId = null,
      )
    },
    replacingChargeUuid = replacingChargeUuid,
  )
}
