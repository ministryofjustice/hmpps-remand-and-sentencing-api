package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.subjectaccessrequest.alldata

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.OneToOne
import org.hibernate.annotations.Immutable
import org.hibernate.annotations.Subselect
import org.hibernate.annotations.Synchronize
import org.hibernate.proxy.HibernateProxy
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.config.ConditionalOnSarEnabled
import java.time.LocalDate

@ConditionalOnSarEnabled
@Immutable
@Entity
@Subselect(
  """
  select id
  ,appearance_date
  ,warrant_type
  ,court_code
  ,overall_conviction_date
  ,court_case_id
  ,appearance_outcome_id
  ,next_court_appearance_id
  from court_appearance
  where status_id not in ('DELETED', 'DUPLICATE', 'FUTURE')
  """,
)
@Synchronize("court_appearance")
class CourtAppearanceSarEntity(
  @Id
  @Column
  var id: Int,
  @Column
  var appearanceDate: LocalDate,
  @Column
  var warrantType: String,
  @Column
  var courtCode: String,
  @Column
  var overallConvictionDate: LocalDate?,
  @OneToMany(mappedBy = "latestCourtAppearance", cascade = [CascadeType.ALL], orphanRemoval = true)
  var courtCases: MutableSet<CourtCaseSarEntity> = mutableSetOf(),
  @OneToMany(mappedBy = "appearance", cascade = [CascadeType.ALL], orphanRemoval = true)
  var appearanceCharges: MutableSet<AppearanceChargeSarEntity> = mutableSetOf(),
  @ManyToOne
  @JoinColumn(name = "court_case_id")
  var courtCase: CourtCaseSarEntity?,
  @ManyToOne
  @JoinColumn(name = "appearance_outcome_id")
  var appearanceOutcome: AppearanceOutcomeSarEntity?,
  @OneToOne
  @JoinColumn(name = "next_court_appearance_id")
  var nextCourtAppearance: NextCourtAppearanceSarEntity?,
) {
  final override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null) return false
    val oEffectiveClass =
      if (other is HibernateProxy) other.hibernateLazyInitializer.persistentClass else other.javaClass
    val thisEffectiveClass =
      if (this is HibernateProxy) this.hibernateLazyInitializer.persistentClass else this.javaClass
    if (thisEffectiveClass != oEffectiveClass) return false
    other as CourtAppearanceSarEntity

    return id == other.id
  }

  final override fun hashCode(): Int = if (this is HibernateProxy) this.hibernateLazyInitializer.persistentClass.hashCode() else javaClass.hashCode()
}
