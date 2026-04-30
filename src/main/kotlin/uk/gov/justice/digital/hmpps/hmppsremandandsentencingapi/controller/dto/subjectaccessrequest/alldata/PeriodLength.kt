package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.subjectaccessrequest.alldata

data class PeriodLength(
  val years: Int = 0,
  val months: Int = 0,
  val weeks: Int = 0,
  val days: Int = 0,
  val periodOrder: String,
)
