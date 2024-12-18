package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.util

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.CaseReferenceLegacyData
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.ChargeLegacyData
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.CourtAppearanceLegacyData
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.CourtCaseLegacyData
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacyCreateCharge
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacyCreateCourtAppearance
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacyCreateCourtCase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacyUpdateCharge
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.MigrationCreateCharge
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.MigrationCreateCourtAppearance
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.MigrationCreateCourtCase
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
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
    ): CourtAppearanceLegacyData = CourtAppearanceLegacyData(eventId, caseId, postedDate, nomisOutcomeCode, outcomeDescription, nextEventDateTime)

    fun legacyCreateCourtAppearance(courtCaseUuid: String = UUID.randomUUID().toString(), courtCode: String = "COURT1", appearanceDate: LocalDate = LocalDate.now(), appearanceTypeUuid: UUID = UUID.fromString("63e8fce0-033c-46ad-9edf-391b802d547a"), legacyData: CourtAppearanceLegacyData = courtAppearanceLegacyData()): LegacyCreateCourtAppearance {
      return LegacyCreateCourtAppearance(courtCaseUuid, courtCode, appearanceDate, legacyData, appearanceTypeUuid)
    }

    fun chargeLegacyData(
      postedDate: String = LocalDate.now().format(
        DateTimeFormatter.ISO_DATE,
      ),
      nomisOutcomeCode: String = "1",
      outcomeDescription: String = "Outcome Description",
    ): ChargeLegacyData = ChargeLegacyData(postedDate, nomisOutcomeCode, outcomeDescription)

    fun legacyCreateCharge(appearanceLifetimeUuid: UUID = UUID.randomUUID(), offenceCode: String = "OFF1", offenceStartDate: LocalDate = LocalDate.now(), offenceEndDate: LocalDate? = null, active: Boolean = true, legacyData: ChargeLegacyData = chargeLegacyData()): LegacyCreateCharge {
      return LegacyCreateCharge(appearanceLifetimeUuid, offenceCode, offenceStartDate, offenceEndDate, active, legacyData)
    }

    fun legacyUpdateCharge(offenceStartDate: LocalDate = LocalDate.now().minusDays(20), offenceEndDate: LocalDate? = null, active: Boolean = true, legacyData: ChargeLegacyData = chargeLegacyData()): LegacyUpdateCharge {
      return LegacyUpdateCharge(offenceStartDate, offenceEndDate, active, legacyData)
    }

    fun caseReferenceLegacyData(
      offenderCaseReference: String = "NOMIS123",
      updatedDate: LocalDateTime = LocalDateTime.now(),
    ): CaseReferenceLegacyData = CaseReferenceLegacyData(offenderCaseReference, updatedDate)

    fun courtCaseLegacyData(caseReferences: MutableList<CaseReferenceLegacyData> = mutableListOf(caseReferenceLegacyData())): CourtCaseLegacyData = CourtCaseLegacyData(caseReferences)

    fun migrationCreateCourtCase(prisonerId: String = "PRI123", active: Boolean = true, courtCaseLegacyData: CourtCaseLegacyData = courtCaseLegacyData(), appearances: List<MigrationCreateCourtAppearance> = listOf(migrationCreateCourtAppearance())): MigrationCreateCourtCase = MigrationCreateCourtCase(prisonerId, active, courtCaseLegacyData, appearances)

    fun migrationCreateCourtAppearance(courtCode: String = "COURT1", appearanceDate: LocalDate = LocalDate.now(), appearanceTypeUuid: UUID = UUID.fromString("63e8fce0-033c-46ad-9edf-391b802d547a"), legacyData: CourtAppearanceLegacyData = courtAppearanceLegacyData(), charges: List<MigrationCreateCharge> = listOf(migrationCreateCharge())): MigrationCreateCourtAppearance = MigrationCreateCourtAppearance(courtCode, appearanceDate, appearanceTypeUuid, legacyData, charges)

    fun migrationCreateCharge(chargeNOMISId: String = "5453", offenceCode: String = "OFF1", offenceStartDate: LocalDate = LocalDate.now(), offenceEndDate: LocalDate? = null, active: Boolean = true, legacyData: ChargeLegacyData = chargeLegacyData()): MigrationCreateCharge = MigrationCreateCharge(chargeNOMISId, offenceCode, offenceStartDate, offenceEndDate, active, legacyData)
  }
}
