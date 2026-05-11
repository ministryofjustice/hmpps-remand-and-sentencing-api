package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.subjectaccessrequest.alldata

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
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.config.ConditionalOnSarEnabled
import java.time.LocalDate

@ConditionalOnSarEnabled
@Immutable
@Entity
@Subselect(
  """
  select id
   ,prisoner_id
   ,revocation_date
   ,return_to_custody_date
   ,in_prison_on_revocation_date
   ,status
   ,recall_type_id
  from recall""",
)
@Synchronize("recall")
class RecallSarEntity(
  @Id
  @Column
  var id: Int,
  var prisonerId: String,
  var revocationDate: LocalDate?,
  var returnToCustodyDate: LocalDate?,
  var inPrisonOnRevocationDate: Boolean?,
  @Column
  var status: String,
  @OneToMany(mappedBy = "recall")
  var recallSentences: MutableSet<RecallSentenceSarEntity> = mutableSetOf(),
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "recall_type_id")
  var recallType: RecallTypeSarEntity,
)
