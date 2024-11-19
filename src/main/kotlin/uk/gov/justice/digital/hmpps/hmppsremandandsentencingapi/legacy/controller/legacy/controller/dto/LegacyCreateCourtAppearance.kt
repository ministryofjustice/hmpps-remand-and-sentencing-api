package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.legacy.controller.dto

import java.time.LocalDate

data class LegacyCreateCourtAppearance(
  val courtCaseUuid: String,
  val courtCode: String,
  val appearanceDate: LocalDate,
  val legacyData: CourtAppearanceLegacyData,
)
