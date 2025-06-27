package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.paged

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.projection.CourtCaseRow
import java.time.LocalDate

data class PagedMergedToCase(
  val caseReference: String?,
  val courtCode: String,
  val warrantDate: LocalDate,
  val mergedToDate: LocalDate,
) {
  companion object {
    fun from(courtCaseRow: CourtCaseRow): PagedMergedToCase = PagedMergedToCase(
      courtCaseRow.mergedToCaseReference,
      courtCaseRow.mergedToCourtCode!!,
      courtCaseRow.mergedToWarrantDate!!,
      courtCaseRow.mergedToDate!!,
    )
  }
}
