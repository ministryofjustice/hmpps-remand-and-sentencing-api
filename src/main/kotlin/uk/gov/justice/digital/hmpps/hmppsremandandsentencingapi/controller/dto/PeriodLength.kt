package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.PeriodLengthEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.PeriodLengthType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.PeriodLengthLegacyData
import java.util.UUID

data class PeriodLength(
  val years: Int?,
  val months: Int?,
  val weeks: Int?,
  val days: Int?,
  val periodOrder: String,
  val periodLengthType: PeriodLengthType,
  val legacyData: PeriodLengthLegacyData?,
  val periodLengthUuid: UUID,
) {
  companion object {
    fun from(periodLengthEntity: PeriodLengthEntity): PeriodLength = PeriodLength(
      periodLengthEntity.years,
      periodLengthEntity.months,
      periodLengthEntity.weeks,
      periodLengthEntity.days,
      periodLengthEntity.periodOrder,
      periodLengthEntity.periodLengthType,
      periodLengthEntity.legacyData,
      periodLengthEntity.periodLengthUuid,
    )
  }
}
