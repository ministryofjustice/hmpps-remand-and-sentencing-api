package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto

data class CreateCourtCaseResponse(
  val courtCaseUuid: String,
  val appearances: List<CreateCourtAppearanceResponse>,
)
