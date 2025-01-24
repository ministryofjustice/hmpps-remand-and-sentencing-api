package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto

data class LegacyCreatePeriodLength(
  val periodType: String,
  val periodYears: Int?,
  val periodMonths: Int?,
  val periodWeeks: Int?,
  val periodDays: Int?,
  val legacyData: PeriodLengthLegacyData,
)
