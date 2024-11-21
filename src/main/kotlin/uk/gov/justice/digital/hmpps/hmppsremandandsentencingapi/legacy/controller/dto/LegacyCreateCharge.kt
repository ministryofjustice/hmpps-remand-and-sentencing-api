package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto

import java.time.LocalDate
import java.util.UUID

data class LegacyCreateCharge(
  val appearanceLifetimeUuid: UUID,
  val offenceCode: String,
  val offenceStartDate: LocalDate,
  val offenceEndDate: LocalDate?,
  val active: Boolean,
  val legacyData: ChargeLegacyData,
)
