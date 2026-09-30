package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.document.dto

data class PeriodLength(
  val years: Int = 0,
  val months: Int = 0,
  val weeks: Int = 0,
  val days: Int = 0,
  val periodOrder: String = "years,months,weeks,days",
)
