package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.embeddable

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import java.io.Serializable

@Embeddable
class AppearanceChargeId : Serializable {
  @Column(name = "appearance_id")
  val appearanceId: Int

  @Column(name = "charge_id")
  val chargeId: Int

  constructor(appearanceId: Int, chargeId: Int) {
    this.appearanceId = appearanceId
    this.chargeId = chargeId
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as AppearanceChargeId

    return appearanceId == other.appearanceId && chargeId == other.chargeId
  }

  override fun hashCode(): Int = 31 * appearanceId.hashCode() + chargeId.hashCode()
}
