package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.paged

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.PeriodLengthType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.projection.CourtCaseRow
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.PeriodLengthLegacyData
import java.util.UUID

data class PagedSentencePeriodLength(
  val periodLengthUuid: UUID,
  val years: Int?,
  val months: Int?,
  val weeks: Int?,
  val days: Int?,
  val order: String,
  val type: PeriodLengthType?,
  val legacyData: PeriodLengthLegacyData?,
) {
  companion object {
    fun from(courtCaseRow: CourtCaseRow): PagedSentencePeriodLength = PagedSentencePeriodLength(
      courtCaseRow.sentencePeriodLengthUuid!!,
      courtCaseRow.sentencePeriodLengthYears,
      courtCaseRow.sentencePeriodLengthMonths,
      courtCaseRow.sentencePeriodLengthWeeks,
      courtCaseRow.sentencePeriodLengthDays,
      courtCaseRow.sentencePeriodLengthOrder!!,
      courtCaseRow.sentencePeriodLengthType,
      courtCaseRow.sentencePeriodLengthLegacyData,
    )
  }
}
