package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.util

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.ChargeLegacyData
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.CourtAppearanceLegacyData
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacyCreateCharge
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacyCreateCourtAppearance
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacyCreateCourtCase
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
      nomisOutcomeCode: String = "1",
      outcomeDescription: String = "Outcome Description",
      nextEventDateTime: LocalDateTime = LocalDateTime.now(),
    ): CourtAppearanceLegacyData = CourtAppearanceLegacyData(eventId, caseId, postedDate, nomisOutcomeCode, outcomeDescription, nextEventDateTime)

    fun legacyCreateCourtAppearance(courtCaseUuid: String = UUID.randomUUID().toString(), courtCode: String = "COURT1", appearanceDate: LocalDate = LocalDate.now(), legacyData: CourtAppearanceLegacyData = courtAppearanceLegacyData()): LegacyCreateCourtAppearance {
      return LegacyCreateCourtAppearance(courtCaseUuid, courtCode, appearanceDate, legacyData)
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
  }
}
