package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.legacy.controller.dto.CourtCaseLegacyData

data class CreateCourtCase(
  val prisonerId: String,
  val appearances: List<CreateCourtAppearance>,
  val legacyData: CourtCaseLegacyData?,
)
