package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.paged

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.projection.CourtCaseRow
import java.time.LocalDate
import java.util.UUID

data class PagedMergedToCase(
  val appearanceUuid: UUID,
  val caseReference: String?,
  val courtCode: String,
  val warrantDate: LocalDate,
  val mergedToDate: LocalDate,
) {
  companion object {
    fun from(courtCaseRow: CourtCaseRow): PagedMergedToCase = PagedMergedToCase(
      courtCaseRow.mergedToAppearanceUuid!!,
      courtCaseRow.mergedToCaseReference,
      courtCaseRow.mergedToCourtCode!!,
      courtCaseRow.mergedToWarrantDate!!,
      courtCaseRow.mergedToDate!!,
    )
  }
}
