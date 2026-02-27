package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.custom

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.CourtAppearanceEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.CourtCaseEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.PagedCourtCaseOrderBy
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.projection.CourtCaseRow
import java.time.LocalDate

interface CourtCaseSearchRepository {

  fun searchCourtCases(
    prisonerId: String,
    limit: Int,
    offset: Long,
    pagedCourtCaseOrderBy: PagedCourtCaseOrderBy,
    appearanceStatus: CourtAppearanceEntityStatus,
    courtCaseStatus: CourtCaseEntityStatus,
    appearanceDateFrom: LocalDate,
    appearanceDateTo: LocalDate,
  ): List<CourtCaseRow>
}
