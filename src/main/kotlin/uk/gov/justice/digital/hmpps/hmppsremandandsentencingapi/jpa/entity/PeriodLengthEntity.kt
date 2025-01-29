package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity

import com.fasterxml.jackson.databind.JsonNode
import io.hypersistence.utils.hibernate.type.json.JsonType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.Type
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreatePeriodLength
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.PeriodLengthType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacyCreatePeriodLength
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.MigrationCreatePeriodLength
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.util.PeriodLengthTypeMapper

@Entity
@Table(name = "period_length")
class PeriodLengthEntity(
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
  @Enumerated(EnumType.STRING)
  val periodLengthType: PeriodLengthType,
  @ManyToOne
  @JoinColumn(name = "sentence_id")
  var sentenceEntity: SentenceEntity?,
  @ManyToOne
  @JoinColumn(name = "appearance_id")
  var appearanceEntity: CourtAppearanceEntity?,
  @Type(value = JsonType::class)
  @Column(columnDefinition = "jsonb")
  var legacyData: JsonNode? = null,

) {
  fun isSame(other: PeriodLengthEntity?): Boolean = years == other?.years &&
    months == other?.months &&
    weeks == other?.weeks &&
    days == other?.days &&
    periodOrder == other?.periodOrder &&
    periodLengthType == other.periodLengthType

  companion object {
    fun from(periodLength: CreatePeriodLength): PeriodLengthEntity = PeriodLengthEntity(
      years = periodLength.years,
      months = periodLength.months,
      weeks = periodLength.weeks,
      days = periodLength.days,
      periodOrder = periodLength.periodOrder,
      periodLengthType = periodLength.type,
      sentenceEntity = null,
      appearanceEntity = null,
    )

    fun from(periodLength: LegacyCreatePeriodLength, sentenceCalcType: String): PeriodLengthEntity {
      val order = getPeriodOrder(periodLength.periodYears, periodLength.periodMonths, periodLength.periodWeeks, periodLength.periodDays)
      val type = PeriodLengthTypeMapper.convert(periodLength.legacyData, sentenceCalcType)
      return PeriodLengthEntity(
        years = periodLength.periodYears,
        months = periodLength.periodMonths,
        weeks = periodLength.periodWeeks,
        days = periodLength.periodDays,
        periodLengthType = type,
        periodOrder = order,
        sentenceEntity = null,
        appearanceEntity = null,
      )
    }

    fun from(periodLength: MigrationCreatePeriodLength, sentenceCalcType: String): PeriodLengthEntity {
      val order = getPeriodOrder(periodLength.periodYears, periodLength.periodMonths, periodLength.periodWeeks, periodLength.periodDays)
      val type = PeriodLengthTypeMapper.convert(periodLength.legacyData, sentenceCalcType)
      return PeriodLengthEntity(
        years = periodLength.periodYears,
        months = periodLength.periodMonths,
        weeks = periodLength.periodWeeks,
        days = periodLength.periodDays,
        periodLengthType = type,
        periodOrder = order,
        sentenceEntity = null,
        appearanceEntity = null,
      )
    }

    private fun getPeriodOrder(years: Int?, months: Int?, weeks: Int?, days: Int?): String {
      val units: MutableList<String> = mutableListOf()
      if (years != null) {
        units.add("years")
      }
      if (months != null) {
        units.add("months")
      }
      if (weeks != null) {
        units.add("weeks")
      }
      if (days != null) {
        units.add("days")
      }
      return units.joinToString(",")
    }
  }
}
