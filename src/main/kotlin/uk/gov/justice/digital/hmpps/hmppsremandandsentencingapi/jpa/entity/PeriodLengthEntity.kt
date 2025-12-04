package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.DynamicUpdate
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreatePeriodLength
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.PeriodLengthEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.PeriodLengthType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacyCreatePeriodLength
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.MigrationCreatePeriodLength
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.PeriodLengthLegacyData
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.booking.BookingCreatePeriodLength
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.merge.MergeCreatePeriodLength
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.util.PeriodLengthTypeMapper
import java.time.ZonedDateTime
import java.util.UUID

@DynamicUpdate
@Entity
@Table(name = "period_length")
class PeriodLengthEntity(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Int = 0,
  var periodLengthUuid: UUID,
  var years: Int?,
  var months: Int?,
  var weeks: Int?,
  var days: Int?,
  var periodOrder: String,
  @Enumerated(EnumType.STRING)
  var periodLengthType: PeriodLengthType,
  @Enumerated(EnumType.STRING)
  var statusId: PeriodLengthEntityStatus,
  val createdAt: ZonedDateTime = ZonedDateTime.now(),
  val createdBy: String,
  val createdPrison: String? = null,
  var updatedAt: ZonedDateTime? = null,
  var updatedBy: String? = null,
  var updatedPrison: String? = null,
  @ManyToOne
  @JoinColumn(name = "sentence_id")
  var sentenceEntity: SentenceEntity?,
  @ManyToOne
  @JoinColumn(name = "appearance_id")
  var appearanceEntity: CourtAppearanceEntity?,
  @JdbcTypeCode(SqlTypes.JSON)
  var legacyData: PeriodLengthLegacyData? = null,

) {
  fun isSame(other: PeriodLengthEntity?): Boolean = years == other?.years &&
    months == other?.months &&
    weeks == other?.weeks &&
    days == other?.days &&
    periodOrder == other?.periodOrder &&
    periodLengthType == other.periodLengthType &&
    legacyData == other.legacyData &&
    statusId == other.statusId

  fun updateFrom(periodLength: PeriodLengthEntity, username: String) {
    years = periodLength.years
    months = periodLength.months
    weeks = periodLength.weeks
    days = periodLength.days
    periodOrder = getDefaultPeriodOrder()
    periodLengthType = periodLength.periodLengthType
    statusId = periodLength.statusId
    legacyData = periodLength.legacyData
    updatedAt = ZonedDateTime.now()
    updatedBy = username
    updatedPrison = periodLength.createdPrison
  }

  fun updateFrom(
    periodLength: LegacyCreatePeriodLength,
    sentenceEntity: SentenceEntity,
    username: String,
  ) {
    // The sentenceCalcType from NOMIS never gets updated (i.e. cannot change after sentence creation), so we don't need to
    // update the periodLength.type via the legacy updateSentence route
    val sentenceCalcType = sentenceEntity.sentenceType?.nomisSentenceCalcType
      ?: sentenceEntity.legacyData?.sentenceCalcType
      ?: throw IllegalStateException("Sentence calculation type not found")

    val type = PeriodLengthTypeMapper.convertNomisToDps(periodLength.legacyData, sentenceCalcType)

    years = periodLength.periodYears
    months = periodLength.periodMonths
    weeks = periodLength.periodWeeks
    days = periodLength.periodDays
    periodLengthType = type
    legacyData = periodLength.legacyData
    updatedAt = ZonedDateTime.now()
    updatedBy = username
  }

  fun copy(): PeriodLengthEntity = PeriodLengthEntity(
    0,
    periodLengthUuid,
    years,
    months,
    weeks,
    days,
    periodOrder,
    periodLengthType,
    statusId,
    createdAt,
    createdBy,
    createdPrison,
    updatedAt,
    updatedBy,
    updatedPrison,
    sentenceEntity,
    appearanceEntity,
    legacyData,
  )

  fun copy(
    periodLengthUuid: UUID = this.periodLengthUuid,
    years: Int? = this.years,
    months: Int? = this.months,
    weeks: Int? = this.weeks,
    days: Int? = this.days,
    periodOrder: String = this.periodOrder,
    periodLengthType: PeriodLengthType = this.periodLengthType,
    statusId: PeriodLengthEntityStatus = this.statusId,
    createdAt: ZonedDateTime = this.createdAt,
    createdBy: String = this.createdBy,
    createdPrison: String? = this.createdPrison,
    updatedAt: ZonedDateTime? = this.updatedAt,
    updatedBy: String? = this.updatedBy,
    updatedPrison: String? = this.updatedPrison,
    sentenceEntity: SentenceEntity? = this.sentenceEntity,
    appearanceEntity: CourtAppearanceEntity? = this.appearanceEntity,
    legacyData: PeriodLengthLegacyData? = this.legacyData,
  ): PeriodLengthEntity = PeriodLengthEntity(
    id = 0,
    periodLengthUuid = periodLengthUuid,
    years = years,
    months = months,
    weeks = weeks,
    days = days,
    periodOrder = periodOrder,
    periodLengthType = periodLengthType,
    statusId = statusId,
    createdAt = createdAt,
    createdBy = createdBy,
    createdPrison = createdPrison,
    updatedAt = updatedAt,
    updatedBy = updatedBy,
    updatedPrison = updatedPrison,
    sentenceEntity = sentenceEntity,
    appearanceEntity = appearanceEntity,
    legacyData = legacyData,
  )

  fun delete(username: String) {
    statusId = PeriodLengthEntityStatus.DELETED
    updatedAt = ZonedDateTime.now()
    updatedBy = username
  }
  companion object {
    fun from(periodLength: CreatePeriodLength, createdBy: String): PeriodLengthEntity = PeriodLengthEntity(
      periodLengthUuid = periodLength.periodLengthUuid,
      years = periodLength.years,
      months = periodLength.months,
      weeks = periodLength.weeks,
      days = periodLength.days,
      periodOrder = periodLength.periodOrder,
      periodLengthType = periodLength.type,
      statusId = PeriodLengthEntityStatus.ACTIVE,
      sentenceEntity = null,
      appearanceEntity = null,
      createdBy = createdBy,
      createdPrison = periodLength.prisonId,
      legacyData = periodLength.legacyData,
    )

    fun from(periodLengthUuid: UUID, periodLength: LegacyCreatePeriodLength, sentenceEntity: SentenceEntity, createdBy: String): PeriodLengthEntity {
      val order = getDefaultPeriodOrder()
      val sentenceCalcType = sentenceEntity.sentenceType?.nomisSentenceCalcType ?: sentenceEntity.legacyData?.sentenceCalcType
      val type = PeriodLengthTypeMapper.convertNomisToDps(periodLength.legacyData, sentenceCalcType!!)
      return PeriodLengthEntity(
        periodLengthUuid = periodLengthUuid,
        years = periodLength.periodYears,
        months = periodLength.periodMonths,
        weeks = periodLength.periodWeeks,
        days = periodLength.periodDays,
        periodOrder = order,
        periodLengthType = type,
        statusId = PeriodLengthEntityStatus.from(sentenceEntity.statusId),
        sentenceEntity = sentenceEntity,
        appearanceEntity = null,
        legacyData = periodLength.legacyData,
        createdBy = createdBy,
      )
    }

    fun from(periodLength: MigrationCreatePeriodLength, sentenceCalcType: String, createdBy: String): PeriodLengthEntity {
      val order = getDefaultPeriodOrder()
      val type = PeriodLengthTypeMapper.convertNomisToDps(periodLength.legacyData, sentenceCalcType)
      return PeriodLengthEntity(
        periodLengthUuid = UUID.randomUUID(),
        years = periodLength.periodYears,
        months = periodLength.periodMonths,
        weeks = periodLength.periodWeeks,
        days = periodLength.periodDays,
        periodOrder = order,
        periodLengthType = type,
        statusId = PeriodLengthEntityStatus.ACTIVE,
        sentenceEntity = null,
        appearanceEntity = null,
        legacyData = periodLength.legacyData,
        createdBy = createdBy,
        createdPrison = null,
      )
    }

    fun from(periodLength: MergeCreatePeriodLength, sentenceCalcType: String, createdBy: String): PeriodLengthEntity {
      val order = getDefaultPeriodOrder()
      val type = PeriodLengthTypeMapper.convertNomisToDps(periodLength.legacyData, sentenceCalcType)
      return PeriodLengthEntity(
        periodLengthUuid = UUID.randomUUID(),
        years = periodLength.periodYears,
        months = periodLength.periodMonths,
        weeks = periodLength.periodWeeks,
        days = periodLength.periodDays,
        periodOrder = order,
        periodLengthType = type,
        statusId = PeriodLengthEntityStatus.ACTIVE,
        sentenceEntity = null,
        appearanceEntity = null,
        legacyData = periodLength.legacyData,
        createdBy = createdBy,
        createdPrison = null,
      )
    }

    fun from(periodLength: BookingCreatePeriodLength, sentenceCalcType: String, createdBy: String): PeriodLengthEntity {
      val order = getDefaultPeriodOrder()
      val type = PeriodLengthTypeMapper.convertNomisToDps(periodLength.legacyData, sentenceCalcType)
      return PeriodLengthEntity(
        periodLengthUuid = UUID.randomUUID(),
        years = periodLength.periodYears,
        months = periodLength.periodMonths,
        weeks = periodLength.periodWeeks,
        days = periodLength.periodDays,
        periodOrder = order,
        periodLengthType = type,
        statusId = PeriodLengthEntityStatus.DUPLICATE,
        sentenceEntity = null,
        appearanceEntity = null,
        legacyData = periodLength.legacyData,
        createdBy = createdBy,
        createdPrison = null,
      )
    }

    private fun getDefaultPeriodOrder(): String = "years,months,weeks,days"
  }
}
