package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.subjectaccessrequest

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import org.hibernate.annotations.Immutable
import org.hibernate.annotations.Subselect
import org.hibernate.annotations.Synchronize
import org.hibernate.proxy.HibernateProxy
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.config.ConditionalOnSarEnabled
import java.time.ZonedDateTime

@ConditionalOnSarEnabled
@Entity
@Immutable
@Subselect(
  """
  select id
   ,prisoner_id
   ,latest_court_appearance_id
   ,case_unique_identifier
   ,status_id
   ,created_at
   ,updated_at
  from court_case
  where status_id not in ('DELETED', 'DUPLICATE')
  """,
)
@Synchronize("court_case")
class CourtCaseSarEntity(
  @Id
  @Column
  var id: Int = 0,
  var prisonerId: String,
  @OneToMany(mappedBy = "courtCase", cascade = [CascadeType.ALL], orphanRemoval = true)
  var appearances: MutableSet<CourtAppearanceSarEntity> = mutableSetOf(),
  @Suppress("JpaDataSourceORMInspection")
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "latest_court_appearance_id", referencedColumnName = "id", nullable = true)
  var latestCourtAppearance: CourtAppearanceSarEntity?,
  var caseUniqueIdentifier: String,
  var statusId: String,
  var createdAt: ZonedDateTime,
  var updatedAt: ZonedDateTime,
) {

  final override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null) return false
    val oEffectiveClass =
      if (other is HibernateProxy) other.hibernateLazyInitializer.persistentClass else other.javaClass
    val thisEffectiveClass =
      if (this is HibernateProxy) this.hibernateLazyInitializer.persistentClass else this.javaClass
    if (thisEffectiveClass != oEffectiveClass) return false
    other as CourtCaseSarEntity

    return id == other.id
  }

  final override fun hashCode(): Int = if (this is HibernateProxy) this.hibernateLazyInitializer.persistentClass.hashCode() else javaClass.hashCode()
}
