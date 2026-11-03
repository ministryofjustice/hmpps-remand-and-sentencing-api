package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto

import java.math.BigDecimal
import java.time.LocalDate

data class LegacyUpdateCharge(
  val offenceStartDate: LocalDate?,
  val offenceEndDate: LocalDate?,
  var legacyData: ChargeLegacyData,
  val performedByUser: String?,
  val offenceCode: String?,
  val value: BigDecimal?,
)
