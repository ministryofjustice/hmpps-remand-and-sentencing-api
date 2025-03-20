package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity

import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.ManyToOne
import jakarta.persistence.MapsId
import jakarta.persistence.Table
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.embeddable.AppearanceChargeId
import java.time.ZonedDateTime

@Entity
@Table(name = "appearance_charge")
class AppearanceChargeEntity {

  @EmbeddedId
  val id: AppearanceChargeId

  @ManyToOne
  @MapsId("appearanceId")
  var appearance: CourtAppearanceEntity?

  @ManyToOne
  @MapsId("chargeId")
  var charge: ChargeEntity?

  val createdAt: ZonedDateTime = ZonedDateTime.now()
  val createdBy: String
  val createdPrison: String?

  constructor(courtAppearanceEntity: CourtAppearanceEntity, chargeEntity: ChargeEntity, createdBy: String, createdPrison: String?) {
    this.appearance = courtAppearanceEntity
    this.charge = chargeEntity
    this.createdBy = createdBy
    this.createdPrison = createdPrison
    this.id = AppearanceChargeId(courtAppearanceEntity.id, chargeEntity.id)
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as AppearanceChargeEntity
    return appearance == other.appearance && charge == other.charge
  }

  override fun hashCode(): Int = 31 * appearance.hashCode() + charge.hashCode()
}
