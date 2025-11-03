package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto

import java.time.LocalDate
import java.util.UUID

data class LegacyCreateCourtAppearance(
  val courtCaseUuid: String,
  val courtCode: String,
  val appearanceDate: LocalDate,
  val legacyData: CourtAppearanceLegacyData,
  val appearanceTypeUuid: UUID,
  val performedByUser: String?,
)
