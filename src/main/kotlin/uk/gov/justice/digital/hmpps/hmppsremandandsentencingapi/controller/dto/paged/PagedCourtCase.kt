package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.paged

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.CourtCaseLegacyData
import java.time.LocalDate

data class PagedCourtCase(
  val prisonerId: String,
  val courtCaseUuid: String,
  val courtCaseStatus: EntityStatus,
  val legacyData: CourtCaseLegacyData?,
  val appearanceCount: Long,
  val caseReferences: Set<String>,
  val firstDayInCustody: LocalDate,

)
