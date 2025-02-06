package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto

import java.time.LocalDate

data class LegacyUpdateCharge(
  val offenceStartDate: LocalDate?,
  val offenceEndDate: LocalDate?,
  var legacyData: ChargeLegacyData,
)
