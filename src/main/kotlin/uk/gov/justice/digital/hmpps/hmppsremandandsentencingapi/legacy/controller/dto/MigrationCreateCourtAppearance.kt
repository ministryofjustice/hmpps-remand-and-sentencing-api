package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto

import java.time.LocalDate
import java.util.UUID

data class MigrationCreateCourtAppearance(
  val eventId: Long,
  val courtCode: String,
  val appearanceDate: LocalDate,
  val appearanceTypeUuid: UUID,
  val legacyData: CourtAppearanceLegacyData,
  val charges: List<MigrationCreateCharge>,
)
