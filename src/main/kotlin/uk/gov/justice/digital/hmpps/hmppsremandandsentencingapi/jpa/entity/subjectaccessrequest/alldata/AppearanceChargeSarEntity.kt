package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.subjectaccessrequest.alldata

import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.ManyToOne
import jakarta.persistence.MapsId
import jakarta.persistence.Table
import org.hibernate.annotations.Immutable
import org.hibernate.proxy.HibernateProxy
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.config.ConditionalOnSarEnabled

@ConditionalOnSarEnabled
@Immutable
@Entity
@Table(name = "appearance_charge")
class AppearanceChargeSarEntity(
  @EmbeddedId
  var id: AppearanceChargeSarId,
  @ManyToOne(fetch = FetchType.LAZY)
  @MapsId("appearanceId")
  var appearance: CourtAppearanceSarEntity? = null,
  @ManyToOne(fetch = FetchType.LAZY)
  @MapsId("chargeId")
  var charge: ChargeSarEntity? = null,
) {
  final override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null) return false
    val oEffectiveClass =
      if (other is HibernateProxy) other.hibernateLazyInitializer.persistentClass else other.javaClass
    val thisEffectiveClass =
      if (this is HibernateProxy) this.hibernateLazyInitializer.persistentClass else this.javaClass
    if (thisEffectiveClass != oEffectiveClass) return false
    other as AppearanceChargeSarEntity

    return id == other.id
  }

  final override fun hashCode(): Int = id.hashCode()
}
