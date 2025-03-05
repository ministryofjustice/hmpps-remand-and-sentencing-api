package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.ZonedDateTime

@Entity
@Table(name = "appearance_charge")
class AppearanceChargeEntity(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Int = 0,

  @ManyToOne
  @JoinColumn(name = "appearance_id")
  val courtAppearance: CourtAppearanceEntity,

  @ManyToOne
  @JoinColumn(name = "charge_id")
  val charge: ChargeEntity,

  val createdAt: ZonedDateTime = ZonedDateTime.now(),
  val createdBy: String,
  val createdPrison: String?,
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is AppearanceChargeEntity) return false

    return this.courtAppearance.appearanceUuid == other.courtAppearance.appearanceUuid &&
      this.charge.chargeUuid == other.charge.chargeUuid
  }

  override fun hashCode(): Int = 31 * (courtAppearance.appearanceUuid.hashCode() ?: 0) + charge.chargeUuid.hashCode()
}
