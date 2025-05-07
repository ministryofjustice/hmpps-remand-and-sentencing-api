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
) {
  companion object {
    fun from(courtAppearanceEntity: CourtAppearanceEntity): CourtAppearanceHistoryEntity = CourtAppearanceHistoryEntity(
      0, courtAppearanceEntity.appearanceUuid, courtAppearanceEntity.appearanceOutcome?.id, courtAppearanceEntity.courtCase.id, courtAppearanceEntity.courtCode,
      courtAppearanceEntity.courtCaseReference, courtAppearanceEntity.appearanceDate, courtAppearanceEntity.statusId, courtAppearanceEntity.previousAppearance?.id, courtAppearanceEntity.warrantId,
      courtAppearanceEntity.createdAt, courtAppearanceEntity.createdBy, courtAppearanceEntity.createdPrison, courtAppearanceEntity.updatedAt, courtAppearanceEntity.updatedBy,
      courtAppearanceEntity.updatedPrison, courtAppearanceEntity.warrantType, courtAppearanceEntity.nextCourtAppearance?.id, courtAppearanceEntity.overallConvictionDate,
      courtAppearanceEntity.legacyData, courtAppearanceEntity,
    )
  }
}
