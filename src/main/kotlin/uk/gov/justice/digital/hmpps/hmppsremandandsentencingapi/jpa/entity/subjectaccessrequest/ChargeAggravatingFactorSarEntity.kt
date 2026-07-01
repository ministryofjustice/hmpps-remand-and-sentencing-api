package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.subjectaccessrequest

import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.ManyToOne
import jakarta.persistence.MapsId
import org.hibernate.annotations.Immutable
import org.hibernate.annotations.Subselect
import org.hibernate.annotations.Synchronize
import org.hibernate.proxy.HibernateProxy
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.config.ConditionalOnSarEnabled

@ConditionalOnSarEnabled
@Immutable
@Entity
@Subselect(
  """
  select charge_id
  ,aggravating_factor_id
  from charge_aggravating_factor""",
)
@Synchronize("charge_aggravating_factor")
class ChargeAggravatingFactorSarEntity(

  @EmbeddedId
  val id: ChargeAggravatingFactorSarId,
  @ManyToOne(fetch = FetchType.LAZY)
  @MapsId("chargeId")
  val charge: ChargeSarEntity? = null,
  @ManyToOne(fetch = FetchType.LAZY)
  @MapsId("aggravatingFactorId")
  val aggravatingFactor: AggravatingFactorSarEntity? = null,
) {
  final override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null) return false
    val oEffectiveClass =
      if (other is HibernateProxy) other.hibernateLazyInitializer.persistentClass else other.javaClass
    val thisEffectiveClass =
      if (this is HibernateProxy) this.hibernateLazyInitializer.persistentClass else this.javaClass
    if (thisEffectiveClass != oEffectiveClass) return false
    other as ChargeAggravatingFactorSarEntity

    return id == other.id
  }

  final override fun hashCode(): Int = id.hashCode()
}
