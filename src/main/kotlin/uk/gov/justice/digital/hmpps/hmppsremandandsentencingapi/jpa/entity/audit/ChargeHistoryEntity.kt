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
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.ChargeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ChangeSource
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ChargeEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.ChargeLegacyData
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.UUID

@Entity
@Table(name = "charge_history")
class ChargeHistoryEntity(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Int = 0,
  var chargeUuid: UUID,
  val offenceCode: String,
  val offenceStartDate: LocalDate?,
  val offenceEndDate: LocalDate?,
  @Enumerated(EnumType.STRING)
  var statusId: ChargeEntityStatus,
  val chargeOutcomeId: Int?,
  var supersedingChargeId: Int?,
  val terrorRelated: Boolean?,
  val foreignPowerRelated: Boolean?,
  val domesticViolenceRelated: Boolean?,
  val createdAt: ZonedDateTime = ZonedDateTime.now(),
  val createdBy: String,
  val createdPrison: String?,
  val updatedAt: ZonedDateTime?,
  val updatedBy: String?,
  val updatedPrison: String?,
  @JdbcTypeCode(SqlTypes.JSON)
  var legacyData: ChargeLegacyData? = null,
  val mergedFromCaseId: Int?,
  @OneToOne
  @JoinColumn(name = "original_charge_id")
  val originalCharge: ChargeEntity,
  val mergedFromDate: LocalDate? = null,
  @Enumerated(EnumType.STRING)
  val changeSource: ChangeSource,
) {
  companion object {
    fun from(chargeEntity: ChargeEntity, changeSource: ChangeSource): ChargeHistoryEntity = ChargeHistoryEntity(
      0, chargeEntity.chargeUuid, chargeEntity.offenceCode, chargeEntity.offenceStartDate, chargeEntity.offenceEndDate,
      chargeEntity.statusId, chargeEntity.chargeOutcome?.id, chargeEntity.supersedingCharge?.id, chargeEntity.terrorRelated,
      chargeEntity.foreignPowerRelated, chargeEntity.domesticViolenceRelated, chargeEntity.createdAt, chargeEntity.createdBy, chargeEntity.createdPrison, chargeEntity.updatedAt, chargeEntity.updatedBy,
      chargeEntity.updatedPrison, chargeEntity.legacyData, chargeEntity.mergedFromCourtCase?.id, chargeEntity,
      mergedFromDate = chargeEntity.mergedFromDate, changeSource = changeSource,
    )
  }
}
