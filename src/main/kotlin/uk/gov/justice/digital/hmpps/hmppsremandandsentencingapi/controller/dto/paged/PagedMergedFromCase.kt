package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.paged

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.projection.CourtCaseRow
import java.time.LocalDate
import java.util.UUID

data class PagedMergedFromCase(
  val appearanceUuid: UUID,
  val caseReference: String?,
  val courtCode: String,
  val warrantDate: LocalDate,
  val mergedFromDate: LocalDate,
) {
  companion object {
    fun from(courtCaseRows: List<CourtCaseRow>): PagedMergedFromCase {
      val mergedFromCase = courtCaseRows.first()
      return PagedMergedFromCase(
        mergedFromCase.mergedFromAppearanceUuid!!,
        mergedFromCase.mergedFromCaseReference,
        mergedFromCase.mergedFromCourtCode!!,
        mergedFromCase.mergedFromWarrantDate!!,
        mergedFromCase.chargeMergedFromDate!!,
      )
    }
  }
}
