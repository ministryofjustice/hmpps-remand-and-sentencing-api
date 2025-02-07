package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto

data class CreateCourtCaseResponse(
  val courtCaseUuid: String,
  val appearances: List<CreateCourtAppearanceResponse>,
  val charges: List<CreateChargeResponse>,
) {
  companion object {
    fun from(courtCaseUuid: String, createCourtCase: CreateCourtCase): CreateCourtCaseResponse = CreateCourtCaseResponse(
      courtCaseUuid,
      createCourtCase.appearances.map { CreateCourtAppearanceResponse.from(it.appearanceUuid, it) },
      createCourtCase.appearances.flatMap { it.charges }.map { CreateChargeResponse.from(it) },
    )
  }
}
