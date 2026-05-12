package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.subjectaccessrequest

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import org.hibernate.annotations.Immutable
import org.hibernate.annotations.Subselect
import org.hibernate.annotations.Synchronize
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.config.ConditionalOnSarEnabled
import java.time.LocalDate

@ConditionalOnSarEnabled
@Immutable
@Entity
@Subselect(
  """
  select id
  ,appearance_date
  from next_court_appearance""",
)
@Synchronize("next_court_appearance")
class NextCourtAppearanceSarEntity(
  @Id
  @Column
  val id: Int,
  @Column
  var appearanceDate: LocalDate,
)
