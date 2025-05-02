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
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreatePeriodLength
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.PeriodLengthType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacyCreatePeriodLength
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.MigrationCreatePeriodLength
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.PeriodLengthLegacyData
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.util.PeriodLengthTypeMapper
import java.time.ZonedDateTime
import java.util.UUID

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
  @Enumerated(EnumType.ORDINAL)
  var statusId: EntityStatus,
  val createdAt: ZonedDateTime = ZonedDateTime.now(),
  val createdBy: String,
  val createdPrison: String?,
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
    legacyData == other.legacyData

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

  fun delete(username: String) {
    statusId = EntityStatus.DELETED
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
      statusId = EntityStatus.ACTIVE,
      sentenceEntity = null,
      appearanceEntity = null,
      createdBy = createdBy,
      createdPrison = periodLength.prisonId,
    )

    fun from(periodLength: LegacyCreatePeriodLength, sentenceEntity: SentenceEntity, createdBy: String, isManyCharges: Boolean): PeriodLengthEntity {
      val order = getDefaultPeriodOrder()
      val sentenceCalcType = sentenceEntity.sentenceType?.nomisSentenceCalcType ?: sentenceEntity.legacyData?.sentenceCalcType
      val type = PeriodLengthTypeMapper.convertNomisToDps(periodLength.legacyData, sentenceCalcType!!)
      val legacyData = if (type == PeriodLengthType.UNSUPPORTED) periodLength.legacyData else null
      return PeriodLengthEntity(
        periodLengthUuid = periodLength.periodLengthUuid ?: UUID.randomUUID(),
        years = periodLength.periodYears,
        months = periodLength.periodMonths,
        weeks = periodLength.periodWeeks,
        days = periodLength.periodDays,
        periodOrder = order,
        periodLengthType = type,
        statusId = if (isManyCharges) EntityStatus.MANY_CHARGES_DATA_FIX else EntityStatus.ACTIVE,
        sentenceEntity = sentenceEntity,
        appearanceEntity = null,
        legacyData = legacyData,
        createdBy = createdBy,
        createdPrison = null,
      )
    }

    fun from(periodLength: MigrationCreatePeriodLength, sentenceCalcType: String, createdBy: String): PeriodLengthEntity {
      val order = getDefaultPeriodOrder()
      val type = PeriodLengthTypeMapper.convertNomisToDps(periodLength.legacyData, sentenceCalcType)
      val legacyData = if (type == PeriodLengthType.UNSUPPORTED) periodLength.legacyData else null
      return PeriodLengthEntity(
        periodLengthUuid = UUID.randomUUID(),
        years = periodLength.periodYears,
        months = periodLength.periodMonths,
        weeks = periodLength.periodWeeks,
        days = periodLength.periodDays,
        periodOrder = order,
        periodLengthType = type,
        statusId = EntityStatus.ACTIVE,
        sentenceEntity = null,
        appearanceEntity = null,
        legacyData = legacyData,
        createdBy = createdBy,
        createdPrison = null,
      )
    }

    private fun getDefaultPeriodOrder(): String = "years,months,weeks,days"
  }
}
