package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.PeriodLengthEntity
import java.math.BigDecimal

data class PeriodLength(
  val years: BigDecimal?,
  val months: BigDecimal?,
  val weeks: BigDecimal?,
  val days: BigDecimal?,
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
