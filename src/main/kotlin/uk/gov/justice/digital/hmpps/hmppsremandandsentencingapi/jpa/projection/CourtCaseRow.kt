package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.projection

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
  var courtCaseStatus: EntityStatus,
  var courtCaseLegacyData: CourtCaseLegacyData?,
  var appearanceCount: Long,
  var caseReferences: String,
  var firstDayInCustody: LocalDate,
  var appearancePeriodLengthYears: Int?,
  var appearancePeriodLengthMonths: Int?,
  var appearancePeriodLengthWeeks: Int?,
  var appearancePeriodLengthDays: Int?,
  var appearancePeriodLengthOrder: String?,
  var appearancePeriodLengthType: PeriodLengthType?,
  var nextCourtAppearanceCourtCode: String?,
  var nextCourtAppearanceTypeDescription: String?,
  var nextCourtAppearanceDate: LocalDate?,
  var nextCourtAppearanceTime: LocalTime?,
  var latestCourtAppearanceCaseReference: String?,
  var latestCourtAppearanceCourtCode: String,
  var latestCourtAppearanceDate: LocalDate,
  var latestCourtAppearanceWarrantType: String,
  var latestCourtAppearanceOutcome: String?,
  var latestCourtAppearanceLegacyData: CourtAppearanceLegacyData?,
  var latestCourtAppearanceOverallConvictionDate: LocalDate?,
)
