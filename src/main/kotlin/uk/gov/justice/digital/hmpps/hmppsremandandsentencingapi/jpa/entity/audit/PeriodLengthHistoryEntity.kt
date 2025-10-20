package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.audit

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.PeriodLengthEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.PeriodLengthEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.PeriodLengthType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.PeriodLengthLegacyData
import java.time.ZonedDateTime
import java.util.UUID

@Entity
@Table(name = "period_length_history")
class PeriodLengthHistoryEntity(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Int = 0,
  val periodLengthUuid: UUID,
  val years: Int?,
  val months: Int?,
  val weeks: Int?,
  val days: Int?,
  val periodOrder: String,
  @Enumerated(EnumType.STRING)
  val periodLengthType: PeriodLengthType,
  val sentenceId: Int?,
  val appearanceId: Int?,
  @JdbcTypeCode(SqlTypes.JSON)
  val legacyData: PeriodLengthLegacyData?,
  @Enumerated(EnumType.STRING)
  val statusId: PeriodLengthEntityStatus,
  val createdAt: ZonedDateTime,
  val createdBy: String,
  val createdPrison: String?,
  val updatedAt: ZonedDateTime?,
  val updatedBy: String?,
  val updatedPrison: String?,
  @OneToOne
  @JoinColumn(name = "original_period_length_id")
  val originalPeriodLength: PeriodLengthEntity,
) {
  companion object {
    fun from(periodLength: PeriodLengthEntity) = PeriodLengthHistoryEntity(
      0, periodLength.periodLengthUuid, periodLength.years, periodLength.months, periodLength.weeks, periodLength.days,
      periodLength.periodOrder, periodLength.periodLengthType, periodLength.sentenceEntity?.id, periodLength.appearanceEntity?.id,
      periodLength.legacyData, periodLength.statusId, periodLength.createdAt, periodLength.createdBy, periodLength.createdPrison,
      periodLength.updatedAt, periodLength.updatedBy, periodLength.updatedPrison, periodLength,
    )
  }
}
