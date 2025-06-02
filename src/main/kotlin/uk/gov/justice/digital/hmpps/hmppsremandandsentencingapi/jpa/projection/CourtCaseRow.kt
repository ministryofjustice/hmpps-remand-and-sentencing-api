package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.projection

import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.PeriodLengthType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.CourtAppearanceLegacyData
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.CourtCaseLegacyData
import java.time.LocalDate
import java.time.LocalTime

data class CourtCaseRow(
  var courtCaseId: Int,
  var prisonerId: String,
  var courtCaseUuid: String,
  @Enumerated(EnumType.ORDINAL)
  var courtCaseStatus: EntityStatus,
  var courtCaseLegacyData: CourtCaseLegacyData?,
  var appearanceCount: Long,
  var caseReferences: String,
  var firstDayInCustody: LocalDate,
  var latestCourtAppearanceWarrantType: String,
  var dpsOverallCourtCaseOutcome: String?,
  var latestCourtAppearanceLegacyData: CourtAppearanceLegacyData?,
  var appearancePeriodLengthYears: Int?,
  var appearancePeriodLengthMonths: Int?,
  var appearancePeriodLengthWeeks: Int?,
  var appearancePeriodLengthDays: Int?,
  var appearancePeriodLengthOrder: String?,
  var appearancePeriodLengthType: PeriodLengthType?,
  var latestCourtAppearanceOverallConvictionDate: LocalDate?,
  var nextCourtAppearanceCourtCode: String?,
  var nextCourtAppearanceTypeDescription: String?,
  var nextCourtAppearanceDate: LocalDate?,
  var nextCourtAppearanceTime: LocalTime?,
)
