package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity

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
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreatePeriodLength
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.PeriodLengthType

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

) {
  fun isSame(other: PeriodLengthEntity?): Boolean {
    return years == other?.years &&
      months == other?.months &&
      weeks == other?.weeks &&
      days == other?.days &&
      periodOrder == other?.periodOrder &&
      periodLengthType == other.periodLengthType
  }

  companion object {
    fun from(periodLength: CreatePeriodLength): PeriodLengthEntity {
      return PeriodLengthEntity(
        years = periodLength.years,
        months = periodLength.months,
        weeks = periodLength.weeks,
        days = periodLength.days,
        periodOrder = periodLength.periodOrder,
        periodLengthType = periodLength.type,
        sentenceEntity = null,
        appearanceEntity = null,
      )
    }
  }
}
