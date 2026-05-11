package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.subjectaccessrequest

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import org.hibernate.annotations.Immutable
import org.hibernate.annotations.Subselect
import org.hibernate.annotations.Synchronize
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.config.ConditionalOnSarEnabled

@ConditionalOnSarEnabled
@Entity
@Immutable
@Subselect(
  """
  select id, prisoner_id 
  from court_case 
  where status_id not in ('DELETED', 'DUPLICATE')""",
)
@Synchronize("court_case")
class CourtCaseUnsyncedSarEntity(
  @Id
  @Column
  var id: Int = 0,
  var prisonerId: String,
)
