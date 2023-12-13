package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto

data class CreateCourtCase(
  val prisonerId: String,
  val appearances: List<CreateCourtAppearance>,
)
