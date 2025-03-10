package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto

data class MigrationCreateCourtCases(
  val prisonerId: String,
  val courtCases: List<MigrationCreateCourtCase>,
)
