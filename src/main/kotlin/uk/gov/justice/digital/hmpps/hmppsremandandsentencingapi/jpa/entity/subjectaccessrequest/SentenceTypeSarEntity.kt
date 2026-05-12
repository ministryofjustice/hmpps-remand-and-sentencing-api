package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.subjectaccessrequest

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
   ,classification
   ,description
   ,is_recallable
   from sentence_type
  """,
)
@Synchronize("sentence_type")
class SentenceTypeSarEntity(
  @Id
  @Column
  var id: Int,
  @Column
  var classification: String,
  @Column
  var description: String,
  @Column
  var isRecallable: Boolean,
)
