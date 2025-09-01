package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.merge

data class DeactivatedCourtCase(
  val dpsCourtCaseUuid: String,
  val active: Boolean,
)
