package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto

data class LegacyCreateCourtCase(
  val prisonerId: String,
  val active: Boolean,
  val legacyData: CourtCaseLegacyData,
  val performedByUser: String,
)
