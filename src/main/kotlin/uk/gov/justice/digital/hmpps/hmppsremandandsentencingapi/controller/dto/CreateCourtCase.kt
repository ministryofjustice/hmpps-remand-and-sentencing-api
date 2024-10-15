package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.legacy.CourtCaseLegacyData

data class CreateCourtCase(
  val prisonerId: String,
  val appearances: List<CreateCourtAppearance>,
  val legacyData: CourtCaseLegacyData?,
)
