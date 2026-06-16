package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.embeddable

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import java.io.Serializable

@Embeddable
data class ChargeAggravatingFactorId(

  @Column(name = "charge_id")
  val chargeId: Int = 0,

  @Column(name = "aggravating_factor_id")
  val aggravatingFactorId: Int = 0,
) : Serializable {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as ChargeAggravatingFactorId

    return chargeId == other.chargeId && aggravatingFactorId == other.aggravatingFactorId
  }

  override fun hashCode(): Int = 31 * chargeId.hashCode() + aggravatingFactorId.hashCode()
}
