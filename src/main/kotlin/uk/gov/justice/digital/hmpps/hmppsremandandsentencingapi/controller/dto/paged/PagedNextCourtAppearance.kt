package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.paged

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.projection.CourtCaseRow
import java.time.LocalDate
import java.time.LocalTime

data class PagedNextCourtAppearance(
  val appearanceDate: LocalDate,
  val appearanceTime: LocalTime?,
  val courtCode: String?,
  val appearanceTypeDescription: String,
) {
  companion object {
    fun from(courtCaseRow: CourtCaseRow): PagedNextCourtAppearance = PagedNextCourtAppearance(
      courtCaseRow.nextCourtAppearanceDate!!,
      courtCaseRow.nextCourtAppearanceTime,
      courtCaseRow.nextCourtAppearanceCourtCode!!,
      courtCaseRow.nextCourtAppearanceTypeDescription!!,
    )
  }
}
