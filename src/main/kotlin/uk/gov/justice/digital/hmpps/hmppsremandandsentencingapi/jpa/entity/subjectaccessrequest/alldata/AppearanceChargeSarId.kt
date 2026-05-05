package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.subjectaccessrequest.alldata

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import org.hibernate.annotations.Immutable
import org.hibernate.proxy.HibernateProxy
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.config.ConditionalOnSarEnabled
import java.io.Serializable
import java.util.Objects

@ConditionalOnSarEnabled
@Immutable
@Embeddable
class AppearanceChargeSarId(
  @Column(name = "appearance_id")
  var appearanceId: Int,
  @Column(name = "charge_id")
  var chargeId: Int,
) : Serializable {

  final override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null) return false
    val oEffectiveClass =
      if (other is HibernateProxy) other.hibernateLazyInitializer.persistentClass else other.javaClass
    val thisEffectiveClass =
      if (this is HibernateProxy) this.hibernateLazyInitializer.persistentClass else this.javaClass
    if (thisEffectiveClass != oEffectiveClass) return false
    other as AppearanceChargeSarId

    return appearanceId == other.appearanceId &&
      chargeId == other.chargeId
  }

  final override fun hashCode(): Int = Objects.hash(appearanceId, chargeId)
}
