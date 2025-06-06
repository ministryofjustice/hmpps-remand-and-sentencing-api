package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.custom

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.PagedCourtCaseOrderBy
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.projection.CourtCaseRow

interface CourtCaseSearchRepository {

  fun searchCourtCases(prisonerId: String, limit: Int, offset: Long, pagedCourtCaseOrderBy: PagedCourtCaseOrderBy, appearanceStatus: EntityStatus, courtCaseStatus: EntityStatus): List<CourtCaseRow>
}
