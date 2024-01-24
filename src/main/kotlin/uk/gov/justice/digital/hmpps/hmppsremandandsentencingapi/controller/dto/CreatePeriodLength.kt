package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto

import java.math.BigDecimal

data class CreatePeriodLength(
  val years: BigDecimal?,
  val months: BigDecimal?,
  val weeks: BigDecimal?,
  val days: BigDecimal?,
  val periodOrder: String,
)
