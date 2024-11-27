package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto

data class DraftCreateCourtCase(
  val prisonerId: String,
  val draftAppearances: List<DraftCreateCourtAppearance>,
)
