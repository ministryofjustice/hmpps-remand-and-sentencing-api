package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateCharge
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateCourtAppearance
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateCourtCase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateFineAmount
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateNextCourtAppearance
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreatePeriodLength
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateSentence
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.PeriodLengthType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.ChargeLegacyData
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.CourtAppearanceLegacyData
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

class DpsDataCreator {
  companion object Factory {
    fun dpsCreateCourtCase(
      prisonerId: String = "PRI123",
      appearances: List<CreateCourtAppearance> = listOf(dpsCreateCourtAppearance()),
    ): CreateCourtCase {
      return CreateCourtCase(prisonerId, appearances, null)
    }

    fun dpsCreatePeriodLength(
      years: Int? = 1,
      months: Int? = null,
      weeks: Int? = null,
      days: Int? = null,
      periodOrder: String = "years",
      type: PeriodLengthType = PeriodLengthType.OVERALL_SENTENCE_LENGTH,
    ): CreatePeriodLength = CreatePeriodLength(years, months, weeks, days, periodOrder, type)

    fun dpsCreateNextCourtAppearance(
      appearanceDate: LocalDate = LocalDate.now().plusDays(2),
      appearanceTime: LocalTime? = LocalTime.now(),
      courtCode: String = "COURT1",
      appearanceType: String = "Court Appearance",
    ): CreateNextCourtAppearance = CreateNextCourtAppearance(appearanceDate, appearanceTime, courtCode, appearanceType)

    fun dpsCreateCourtAppearance(
      courtCaseUuid: String? = null,
      appearanceUUID: UUID = UUID.randomUUID(),
      lifetimeUuid: UUID = UUID.randomUUID(),
      outcomeUuid: UUID = UUID.fromString("62412083-9892-48c9-bf01-7864af4a8b3c"),
      courtCode: String = "COURT1",
      courtCaseReference: String? = "GH123456789",
      appearanceDate: LocalDate = LocalDate.now(),
      warrantId: String? = "123",
      warrantType: String = "REMAND",
      taggedBail: Int? = 1,
      overallSentenceLength: CreatePeriodLength? = dpsCreatePeriodLength(),
      nextCourtAppearance: CreateNextCourtAppearance? = dpsCreateNextCourtAppearance(),
      charges: List<CreateCharge> = listOf(dpsCreateCharge()),
      overallConvictionDate: LocalDate? = LocalDate.now().minusDays(7),
      legacyData: CourtAppearanceLegacyData? = null,
    ): CreateCourtAppearance = CreateCourtAppearance(courtCaseUuid, appearanceUUID, lifetimeUuid, outcomeUuid, courtCode, courtCaseReference, appearanceDate, warrantId, warrantType, taggedBail, overallSentenceLength, nextCourtAppearance, charges, overallConvictionDate, legacyData)

    fun dpsCreateCharge(
      appearanceUuid: UUID? = null,
      chargeUuid: UUID = UUID.randomUUID(),
      chargeLifetimeUuid: UUID = UUID.randomUUID(),
      offenceCode: String = "OFF123",
      offenceStartDate: LocalDate = LocalDate.now(),
      offenceEndDate: LocalDate? = null,
      outcomeUuid: UUID? = UUID.fromString("f17328cf-ceaa-43c2-930a-26cf74480e18"),
      terrorRelated: Boolean? = true,
      sentence: CreateSentence = dpsCreateSentence(),
      legacyData: ChargeLegacyData? = null,
    ): CreateCharge = CreateCharge(appearanceUuid, chargeUuid, chargeLifetimeUuid, offenceCode, offenceStartDate, offenceEndDate, outcomeUuid, terrorRelated, sentence, legacyData)

    fun dpsCreateSentence(
      sentenceUuid: UUID? = UUID.randomUUID(),
      chargeNumber: String = "1",
      periodLengths: List<CreatePeriodLength> = listOf(dpsCreatePeriodLength(type = PeriodLengthType.SENTENCE_LENGTH)),
      sentenceServeType: String = "FORTHWITH",
      consecutiveToChargeNumber: String? = null,
      consecutiveToSentenceUuid: UUID? = null,
      sentenceTypeId: UUID = UUID.fromString("1104e683-5467-4340-b961-ff53672c4f39"),
      convictionDate: LocalDate? = LocalDate.now().minusDays(7),
      fineAmount: CreateFineAmount? = null,
    ): CreateSentence = CreateSentence(sentenceUuid, chargeNumber, periodLengths, sentenceServeType, consecutiveToChargeNumber, consecutiveToSentenceUuid, sentenceTypeId, convictionDate, fineAmount)
  }
}
