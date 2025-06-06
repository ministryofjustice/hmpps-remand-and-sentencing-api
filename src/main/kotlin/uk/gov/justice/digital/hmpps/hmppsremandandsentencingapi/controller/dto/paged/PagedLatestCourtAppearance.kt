package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.paged

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.projection.CourtCaseRow
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.CourtAppearanceLegacyData
import java.time.LocalDate

data class PagedLatestCourtAppearance(
  val caseReference: String?,
  val courtCode: String,
  val warrantDate: LocalDate,
  val warrantType: String,
  val outcome: String?,
  val convictionDate: LocalDate?,
  val legacyData: CourtAppearanceLegacyData?,
  val nextCourtAppearance: PagedNextCourtAppearance?,
  val charges: List<PagedCharge>,
) {
  companion object {
    fun from(courtCaseRow: CourtCaseRow, latestAppearanceCharges: Map<Int, List<CourtCaseRow>>): PagedLatestCourtAppearance = PagedLatestCourtAppearance(
      courtCaseRow.latestCourtAppearanceCaseReference,
      courtCaseRow.latestCourtAppearanceCourtCode,
      courtCaseRow.latestCourtAppearanceDate,
      courtCaseRow.latestCourtAppearanceWarrantType,
      courtCaseRow.latestCourtAppearanceOutcome ?: courtCaseRow.latestCourtAppearanceLegacyData?.outcomeDescription,
      courtCaseRow.latestCourtAppearanceOverallConvictionDate,
      courtCaseRow.latestCourtAppearanceLegacyData,
      courtCaseRow.takeIf { it.nextCourtAppearanceId != null }?.let { PagedNextCourtAppearance.from(it) },
      latestAppearanceCharges.values.map {
        PagedCharge.from(it)
      },
    )
  }
}
