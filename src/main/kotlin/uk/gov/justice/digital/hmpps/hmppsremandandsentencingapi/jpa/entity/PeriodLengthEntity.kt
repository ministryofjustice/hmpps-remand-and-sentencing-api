package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreatePeriodLength

@Entity
@Table(name = "period_length")
data class PeriodLengthEntity(
  @Id
  @Column
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Int = 0,
  @Column
  val years: Int?,
  @Column
  val months: Int?,
  @Column
  val weeks: Int?,
  @Column
  val days: Int?,
  @Column
  val periodOrder: String,
) {
  fun isSame(other: PeriodLengthEntity?): Boolean {
    return years == other?.years &&
      months == other?.months &&
      weeks == other?.weeks &&
      days == other?.days &&
      periodOrder == other?.periodOrder
  }

  companion object {
    fun from(periodLength: CreatePeriodLength): PeriodLengthEntity {
      return PeriodLengthEntity(
        years = periodLength.years,
        months = periodLength.months,
        weeks = periodLength.weeks,
        days = periodLength.days,
        periodOrder = periodLength.periodOrder,
      )
    }
  }
}
