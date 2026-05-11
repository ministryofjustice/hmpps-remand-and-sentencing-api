package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.subjectaccessrequest.alldata

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import org.hibernate.annotations.Immutable
import org.hibernate.annotations.Subselect
import org.hibernate.annotations.Synchronize
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.config.ConditionalOnSarEnabled

@ConditionalOnSarEnabled
@Immutable
@Entity
@Subselect(
  """
  select id
   ,code
  from recall_type""",
)
@Synchronize("recall_type")
class RecallTypeSarEntity(
  @Id
  @Column
  var id: Int,
  var code: String,
)
