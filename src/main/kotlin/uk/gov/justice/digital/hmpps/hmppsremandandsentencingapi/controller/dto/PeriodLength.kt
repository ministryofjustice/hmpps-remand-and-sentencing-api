package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.PeriodLengthEntity

data class PeriodLength(
  val years: Int?,
  val months: Int?,
  val weeks: Int?,
  val days: Int?,
  val periodOrder: String,
) {
  companion object {
    fun from(periodLengthEntity: PeriodLengthEntity): PeriodLength {
      return PeriodLength(
        periodLengthEntity.years,
        periodLengthEntity.months,
        periodLengthEntity.weeks,
        periodLengthEntity.days,
        periodLengthEntity.periodOrder,
      )
    }
  }
}
