package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto

import java.time.LocalDate

data class MigrationCreateCourtAppearance(
  val courtCode: String,
  val appearanceDate: LocalDate,
  val legacyData: CourtAppearanceLegacyData,
  val charges: List<MigrationCreateCharge>,
)
