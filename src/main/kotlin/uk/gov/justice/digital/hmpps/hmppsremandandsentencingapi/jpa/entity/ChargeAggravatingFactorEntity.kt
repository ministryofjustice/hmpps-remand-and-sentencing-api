package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity

import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.MapsId
import jakarta.persistence.Table
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.embeddable.ChargeAggravatingFactorId

@Entity
@Table(name = "charge_aggravating_factor")
class ChargeAggravatingFactorEntity(

  @EmbeddedId
  val id: ChargeAggravatingFactorId,

  @ManyToOne(fetch = FetchType.LAZY)
  @MapsId("chargeId")
  @JoinColumn(name = "charge_id", nullable = false)
  val charge: ChargeEntity,

  @ManyToOne(fetch = FetchType.LAZY)
  @MapsId("aggravatingFactorId")
  @JoinColumn(name = "aggravating_factor_id", nullable = false)
  val aggravatingFactor: AggravatingFactorEntity,
) {
  constructor(charge: ChargeEntity, aggravatingFactor: AggravatingFactorEntity) : this(
    id = ChargeAggravatingFactorId(charge.id, aggravatingFactor.id),
    charge = charge,
    aggravatingFactor = aggravatingFactor,
  )

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false
    other as ChargeAggravatingFactorEntity
    return id == other.id
  }

  override fun hashCode(): Int = 31 * charge.hashCode() + aggravatingFactor.hashCode()
}
