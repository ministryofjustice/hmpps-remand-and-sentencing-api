package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.paged

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.PeriodLengthType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.projection.CourtCaseRow

data class PagedAppearancePeriodLength(
  val years: Int?,
  val months: Int?,
  val weeks: Int?,
  val days: Int?,
  val order: String,
  val type: PeriodLengthType,
) {
  companion object {
    fun from(courtCaseRow: CourtCaseRow): PagedAppearancePeriodLength = PagedAppearancePeriodLength(
      courtCaseRow.appearancePeriodLengthYears,
      courtCaseRow.appearancePeriodLengthMonths,
      courtCaseRow.appearancePeriodLengthWeeks,
      courtCaseRow.appearancePeriodLengthDays,
      courtCaseRow.appearancePeriodLengthOrder!!,
      courtCaseRow.appearancePeriodLengthType!!,
    )
  }
}
