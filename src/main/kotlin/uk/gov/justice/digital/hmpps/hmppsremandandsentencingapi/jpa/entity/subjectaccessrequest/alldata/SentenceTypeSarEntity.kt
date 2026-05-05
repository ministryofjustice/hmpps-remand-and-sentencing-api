package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.subjectaccessrequest.alldata

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.Immutable
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.config.ConditionalOnSarEnabled

@ConditionalOnSarEnabled
@Immutable
@Entity
@Table(name = "sentence_type")
class SentenceTypeSarEntity(
  @Id
  @Column
  var id: Int,
  var classification: String,
  var description: String,
  @Column(name = "is_recallable")
  var isRecallable: Boolean,
)
