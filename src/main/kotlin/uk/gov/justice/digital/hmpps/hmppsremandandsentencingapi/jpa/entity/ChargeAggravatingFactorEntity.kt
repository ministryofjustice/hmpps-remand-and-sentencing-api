package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity

import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.MapsId
import jakarta.persistence.Table
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.keys.ChargeAggravatingFactorId

@Entity
@Table(name = "charge_aggravating_factor")
class ChargeAggravatingFactorEntity(

  @EmbeddedId
  val id: ChargeAggravatingFactorId = ChargeAggravatingFactorId(),

  @ManyToOne(fetch = FetchType.LAZY)
  @MapsId("chargeId")
  @JoinColumn(name = "charge_id", nullable = false)
  val charge: ChargeEntity,

  @ManyToOne(fetch = FetchType.LAZY)
  @MapsId("aggravatingFactorId")
  @JoinColumn(name = "aggravating_factor_id", nullable = false)
  val aggravatingFactor: AggravatingFactorEntity,
)