package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto

data class CreatePeriodLength(
  val years: Int?,
  val months: Int?,
  val weeks: Int?,
  val days: Int?,
  val periodOrder: String,
)
