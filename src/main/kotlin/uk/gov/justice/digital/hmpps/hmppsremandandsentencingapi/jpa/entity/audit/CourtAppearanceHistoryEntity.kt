package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.audit

import jakarta.persistence.Column
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
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.event.EventSource
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.CourtAppearanceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.CourtAppearanceLegacyData
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.*

@Entity
@Table(name = "court_appearance_history")
class CourtAppearanceHistoryEntity(
  @Id
  @Column
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Int = 0,
  val appearanceUuid: UUID,
  val appearanceOutcomeId: Int?,
  val courtCaseId: Int,
  val courtCode: String,
  val courtCaseReference: String?,
  val appearanceDate: LocalDate,
  @Enumerated(EnumType.ORDINAL)
  val statusId: EntityStatus,
  val previousAppearanceId: Int?,
  val warrantId: String?,
  val createdAt: ZonedDateTime,
  val createdBy: String,
  val createdPrison: String?,
  val updatedAt: ZonedDateTime?,
  val updatedBy: String?,
  val updatedPrison: String?,
  val warrantType: String,
  val nextCourtAppearanceId: Int?,
  val overallConvictionDate: LocalDate?,
  @JdbcTypeCode(SqlTypes.JSON)
  val legacyData: CourtAppearanceLegacyData?,
  @OneToOne
  @JoinColumn(name = "original_appearance_id")
  val originalAppearance: CourtAppearanceEntity,
  @Enumerated(EnumType.STRING)
  val source: EventSource,
) {
  companion object {
    fun from(courtAppearanceEntity: CourtAppearanceEntity): CourtAppearanceHistoryEntity = CourtAppearanceHistoryEntity(
      id = 0,
      appearanceUuid = courtAppearanceEntity.appearanceUuid,
      appearanceOutcomeId = courtAppearanceEntity.appearanceOutcome?.id,
      courtCaseId = courtAppearanceEntity.courtCase.id,
      courtCode = courtAppearanceEntity.courtCode,
      courtCaseReference = courtAppearanceEntity.courtCaseReference,
      appearanceDate = courtAppearanceEntity.appearanceDate,
      statusId = courtAppearanceEntity.statusId,
      previousAppearanceId = courtAppearanceEntity.previousAppearance?.id,
      warrantId = courtAppearanceEntity.warrantId,
      createdAt = courtAppearanceEntity.createdAt,
      createdBy = courtAppearanceEntity.createdBy,
      createdPrison = courtAppearanceEntity.createdPrison,
      updatedAt = courtAppearanceEntity.updatedAt,
      updatedBy = courtAppearanceEntity.updatedBy,
      updatedPrison = courtAppearanceEntity.updatedPrison,
      warrantType = courtAppearanceEntity.warrantType,
      nextCourtAppearanceId = courtAppearanceEntity.nextCourtAppearance?.id,
      overallConvictionDate = courtAppearanceEntity.overallConvictionDate,
      legacyData = courtAppearanceEntity.legacyData,
      originalAppearance = courtAppearanceEntity,
      source = courtAppearanceEntity.source,
    )
  }
}
