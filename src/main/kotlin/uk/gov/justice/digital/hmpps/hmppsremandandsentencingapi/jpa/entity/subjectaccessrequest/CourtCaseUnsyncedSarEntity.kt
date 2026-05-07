package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.subjectaccessrequest

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.Immutable
import org.hibernate.annotations.Subselect
import org.hibernate.annotations.Synchronize
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.config.ConditionalOnSarEnabled

@ConditionalOnSarEnabled
@Entity
@Immutable
@Subselect("select * from court_case")
@Synchronize("court_case")
@Table(name = "court_case")
class CourtCaseUnsyncedSarEntity(
  @Id
  @Column
  var id: Int = 0,
  var prisonerId: String,
)
