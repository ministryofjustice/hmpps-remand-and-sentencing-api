package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.util

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.CaseReferenceLegacyData
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.ChargeLegacyData
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.CourtAppearanceLegacyData
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.CourtCaseLegacyData
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacyCreateCharge
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacyCreateCourtAppearance
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacyCreateCourtCase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacyCreateFine
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacyCreatePeriodLength
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacyCreateSentence
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacyUpdateCharge
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacyUpdateWholeCharge
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.MigrationCreateCharge
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.MigrationCreateCourtAppearance
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.MigrationCreateCourtCase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.MigrationCreateFine
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.MigrationCreateSentence
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.MigrationSentenceId
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.PeriodLengthLegacyData
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.SentenceLegacyData
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.UUID

class DataCreator {
  companion object Factory {
    fun legacyCreateCourtCase(prisonerId: String = "PRI123", active: Boolean = true): LegacyCreateCourtCase {
      return LegacyCreateCourtCase(prisonerId, active)
    }

    fun courtAppearanceLegacyData(
      eventId: String = "1",
      caseId: String = "1",
      postedDate: String = LocalDate.now().format(
        DateTimeFormatter.ISO_DATE,
      ),
      nomisOutcomeCode: String? = "1",
      outcomeDescription: String? = "Outcome Description",
      nextEventDateTime: LocalDateTime? = LocalDateTime.now().plusDays(10),
      appearanceTime: LocalTime = LocalTime.now().truncatedTo(ChronoUnit.SECONDS),
    ): CourtAppearanceLegacyData = CourtAppearanceLegacyData(eventId, caseId, postedDate, nomisOutcomeCode, outcomeDescription, nextEventDateTime, appearanceTime)

    fun legacyCreateCourtAppearance(courtCaseUuid: String = UUID.randomUUID().toString(), courtCode: String = "COURT1", appearanceDate: LocalDate = LocalDate.now(), appearanceTypeUuid: UUID = UUID.fromString("63e8fce0-033c-46ad-9edf-391b802d547a"), legacyData: CourtAppearanceLegacyData = courtAppearanceLegacyData()): LegacyCreateCourtAppearance {
      return LegacyCreateCourtAppearance(courtCaseUuid, courtCode, appearanceDate, legacyData, appearanceTypeUuid)
    }

    fun chargeLegacyData(
      postedDate: String = LocalDate.now().format(
        DateTimeFormatter.ISO_DATE,
      ),
      nomisOutcomeCode: String = "1",
      outcomeDescription: String = "Outcome Description",
      outcomeDispositionCode: String = "INTERIM",
    ): ChargeLegacyData = ChargeLegacyData(postedDate, nomisOutcomeCode, outcomeDescription, outcomeDispositionCode)

    fun legacyCreateCharge(appearanceLifetimeUuid: UUID = UUID.randomUUID(), offenceCode: String = "OFF1", offenceStartDate: LocalDate = LocalDate.now(), offenceEndDate: LocalDate? = null, legacyData: ChargeLegacyData = chargeLegacyData()): LegacyCreateCharge {
      return LegacyCreateCharge(appearanceLifetimeUuid, offenceCode, offenceStartDate, offenceEndDate, legacyData)
    }

    fun legacyCreateFine(fineAmount: BigDecimal = BigDecimal.TEN): LegacyCreateFine = LegacyCreateFine(fineAmount)

    fun sentenceLegacyData(
      sentenceCalcType: String = "1",
      sentenceCategory: String = "1",
      sentenceTypeDescription: String = "Sentence Type Description",
      postedDate: String = LocalDate.now().format(
        DateTimeFormatter.ISO_DATE,
      ),
    ): SentenceLegacyData = SentenceLegacyData(sentenceCalcType, sentenceCategory, sentenceTypeDescription, postedDate)

    fun legacyCreateSentence(chargeLifetimeUuid: UUID = UUID.randomUUID(), chargeNumber: String = "1", fine: LegacyCreateFine = legacyCreateFine(), consecutiveToLifetimeUuid: UUID? = null, active: Boolean = true, prisonId: String = "PRISON1", sentenceLegacyData: SentenceLegacyData = sentenceLegacyData(), periodLengths: List<LegacyCreatePeriodLength> = listOf(legacyCreatePeriodLength())): LegacyCreateSentence =
      LegacyCreateSentence(chargeLifetimeUuid, chargeNumber, fine, consecutiveToLifetimeUuid, active, prisonId, sentenceLegacyData, periodLengths)

    fun legacyUpdateWholeCharge(offenceCode: String = "ANOTHERCODE"): LegacyUpdateWholeCharge {
      return LegacyUpdateWholeCharge(offenceCode)
    }

    fun legacyUpdateCharge(offenceStartDate: LocalDate = LocalDate.now().minusDays(20), offenceEndDate: LocalDate? = null, legacyData: ChargeLegacyData = chargeLegacyData()): LegacyUpdateCharge {
      return LegacyUpdateCharge(offenceStartDate, offenceEndDate, legacyData)
    }

    fun caseReferenceLegacyData(
      offenderCaseReference: String = "NOMIS123",
      updatedDate: LocalDateTime = LocalDateTime.now(),
    ): CaseReferenceLegacyData = CaseReferenceLegacyData(offenderCaseReference, updatedDate)

    fun courtCaseLegacyData(caseReferences: MutableList<CaseReferenceLegacyData> = mutableListOf(caseReferenceLegacyData())): CourtCaseLegacyData = CourtCaseLegacyData(caseReferences)

    fun migrationCreateCourtCase(prisonerId: String = "PRI123", active: Boolean = true, courtCaseLegacyData: CourtCaseLegacyData = courtCaseLegacyData(), appearances: List<MigrationCreateCourtAppearance> = listOf(migrationCreateCourtAppearance())): MigrationCreateCourtCase = MigrationCreateCourtCase(prisonerId, active, courtCaseLegacyData, appearances)

    fun migrationCreateCourtAppearance(courtCode: String = "COURT1", appearanceDate: LocalDate = LocalDate.now(), appearanceTypeUuid: UUID = UUID.fromString("63e8fce0-033c-46ad-9edf-391b802d547a"), legacyData: CourtAppearanceLegacyData = courtAppearanceLegacyData(), charges: List<MigrationCreateCharge> = listOf(migrationCreateCharge())): MigrationCreateCourtAppearance = MigrationCreateCourtAppearance(courtCode, appearanceDate, appearanceTypeUuid, legacyData, charges)

    fun migrationCreateCharge(chargeNOMISId: String = "5453", offenceCode: String = "OFF1", offenceStartDate: LocalDate = LocalDate.now(), offenceEndDate: LocalDate? = null, legacyData: ChargeLegacyData = chargeLegacyData(), sentence: MigrationCreateSentence? = migrationCreateSentence()): MigrationCreateCharge = MigrationCreateCharge(chargeNOMISId, offenceCode, offenceStartDate, offenceEndDate, legacyData, sentence = sentence)

    fun migrationCreateSentence(sentenceId: MigrationSentenceId = migrationSentenceId(), chargeNumber: String = "1", fine: MigrationCreateFine = migrationCreateFine(), active: Boolean = true, legacyData: SentenceLegacyData = sentenceLegacyData(), consecutiveToSentenceId: MigrationSentenceId? = null, consecutiveToSentenceLifetimeUuid: UUID? = null): MigrationCreateSentence =
      MigrationCreateSentence(sentenceId, chargeNumber, fine, active, legacyData, consecutiveToSentenceId, consecutiveToSentenceLifetimeUuid)

    fun migrationCreateFine(fineAmount: BigDecimal = BigDecimal.TEN): MigrationCreateFine = MigrationCreateFine(fineAmount)

    fun migrationSentenceId(offenderBookingId: Long = 1, sequence: Int = 1): MigrationSentenceId = MigrationSentenceId(offenderBookingId, sequence)

    fun periodLengthLegacyData(lifeSentence: Boolean = false, sentenceTermCode: String = "1"): PeriodLengthLegacyData =
      PeriodLengthLegacyData(lifeSentence, sentenceTermCode)

    fun legacyCreatePeriodLength(periodType: String = "IMP", periodYears: Int? = 2, periodMonths: Int? = null, periodWeeks: Int? = null, periodDays: Int? = 2, legacyData: PeriodLengthLegacyData = periodLengthLegacyData()): LegacyCreatePeriodLength =
      LegacyCreatePeriodLength(periodType, periodYears, periodMonths, periodWeeks, periodDays, legacyData)
  }
}
