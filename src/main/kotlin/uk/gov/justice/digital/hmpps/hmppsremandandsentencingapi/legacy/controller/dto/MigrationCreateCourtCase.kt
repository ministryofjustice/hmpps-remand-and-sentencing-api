package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto

data class MigrationCreateCourtCase(
  val prisonerId: String,
  val active: Boolean,
  val courtCaseLegacyData: CourtCaseLegacyData,
  val appearances: List<MigrationCreateCourtAppearance>,
)
