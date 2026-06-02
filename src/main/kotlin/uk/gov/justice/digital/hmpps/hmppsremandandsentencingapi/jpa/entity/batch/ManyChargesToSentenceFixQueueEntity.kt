package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.batch

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "many_charges_to_sentence_fix_queue")
class ManyChargesToSentenceFixQueueEntity(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  var id: Long = 0,
  val caseUniqueIdentifier: String,
)
