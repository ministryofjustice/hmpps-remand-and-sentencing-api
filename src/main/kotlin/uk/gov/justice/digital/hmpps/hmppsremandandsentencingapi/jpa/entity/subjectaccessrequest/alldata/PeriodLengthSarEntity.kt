package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.subjectaccessrequest.alldata

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.Immutable
import org.hibernate.annotations.Subselect
import org.hibernate.annotations.Synchronize
import org.hibernate.proxy.HibernateProxy
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.config.ConditionalOnSarEnabled

@ConditionalOnSarEnabled
@Immutable
@Entity
@Subselect("select * from period_length")
@Synchronize("period_length")
@Table(name = "period_length")
class PeriodLengthSarEntity(
  @Id
  @Column
  var id: Int,
  @ManyToOne
  @JoinColumn(name = "sentence_id")
  var sentenceEntity: SentenceSarEntity?,
  var years: Int?,
  var months: Int?,
  var weeks: Int?,
  var days: Int?,
  var periodOrder: String,
) {
  final override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null) return false
    val oEffectiveClass =
      if (other is HibernateProxy) other.hibernateLazyInitializer.persistentClass else other.javaClass
    val thisEffectiveClass =
      if (this is HibernateProxy) this.hibernateLazyInitializer.persistentClass else this.javaClass
    if (thisEffectiveClass != oEffectiveClass) return false
    other as PeriodLengthSarEntity

    return id == other.id
  }

  final override fun hashCode(): Int = if (this is HibernateProxy) this.hibernateLazyInitializer.persistentClass.hashCode() else javaClass.hashCode()
}
