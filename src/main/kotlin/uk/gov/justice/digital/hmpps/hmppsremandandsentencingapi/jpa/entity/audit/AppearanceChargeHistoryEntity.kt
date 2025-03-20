package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.audit

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.AppearanceChargeEntity
import java.time.ZonedDateTime

@Entity
@Table(name = "appearance_charge_history")
class AppearanceChargeHistoryEntity(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Int = 0,
  val appearanceId: Int,
  val chargeId: Int,
  val createdAt: ZonedDateTime,
  val createdBy: String,
  val createdPrison: String?,
  val removedAt: ZonedDateTime? = null,
  val removedBy: String? = null,
  val removedPrison: String? = null,
) {
  companion object {
    fun from(appearanceCharge: AppearanceChargeEntity) = AppearanceChargeHistoryEntity(
      id = 0,
      appearanceId = appearanceCharge.appearance!!.id,
      chargeId = appearanceCharge.charge!!.id,
      createdAt = appearanceCharge.createdAt,
      createdBy = appearanceCharge.createdBy,
      createdPrison = appearanceCharge.createdPrison,
    )

    fun removedFrom(appearanceCharge: AppearanceChargeEntity, removedBy: String, removedPrison: String?) = AppearanceChargeHistoryEntity(
      id = 0,
      appearanceId = appearanceCharge.appearance!!.id,
      chargeId = appearanceCharge.charge!!.id,
      createdAt = appearanceCharge.createdAt,
      createdBy = appearanceCharge.createdBy,
      createdPrison = appearanceCharge.createdPrison,
      removedBy = removedBy,
      removedPrison = removedPrison,
      removedAt = ZonedDateTime.now(),
    )
  }
}
