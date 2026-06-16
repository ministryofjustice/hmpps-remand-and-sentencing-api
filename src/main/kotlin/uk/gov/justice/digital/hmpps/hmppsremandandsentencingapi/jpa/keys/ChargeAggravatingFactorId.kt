package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.keys


import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import java.io.Serializable

@Embeddable
data class ChargeAggravatingFactorId(

  @Column(name = "charge_id")
  val chargeId: Int = 0,

  @Column(name = "aggravating_factor_id")
  val aggravatingFactorId: Int = 0,
) : Serializable
