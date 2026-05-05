package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.subjectaccessrequest.alldata

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import org.hibernate.annotations.Immutable
import org.hibernate.proxy.HibernateProxy
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.config.ConditionalOnSarEnabled

@ConditionalOnSarEnabled
@Immutable
@Entity
@Table(name = "sentence")
class SentenceSarEntity(
  @Id
  @Column
  var id: Int,
  @ManyToOne
  @JoinColumn(name = "charge_id")
  var charge: ChargeSarEntity,
  @OneToOne
  @JoinColumn(name = "sentence_type_id")
  var sentenceType: SentenceTypeSarEntity?,
  @Column
  var sentenceServeType: String,
  @Column
  var statusId: String,
  @OneToMany
  @JoinColumn(name = "sentence_id")
  var periodLengths: MutableSet<PeriodLengthSarEntity> = mutableSetOf(),
  @OneToMany(mappedBy = "sentence")
  var recallSentences: MutableSet<RecallSentenceSarEntity> = mutableSetOf(),
) {
  final override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null) return false
    val oEffectiveClass =
      if (other is HibernateProxy) other.hibernateLazyInitializer.persistentClass else other.javaClass
    val thisEffectiveClass =
      if (this is HibernateProxy) this.hibernateLazyInitializer.persistentClass else this.javaClass
    if (thisEffectiveClass != oEffectiveClass) return false
    other as SentenceSarEntity

    return id == other.id
  }

  final override fun hashCode(): Int = if (this is HibernateProxy) this.hibernateLazyInitializer.persistentClass.hashCode() else javaClass.hashCode()
}
