package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.subjectaccessrequest

import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.repository.CrudRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.config.ConditionalOnSarEnabled
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.subjectaccessrequest.RecallSarEntity

@ConditionalOnSarEnabled
interface RecallSarRepository : CrudRepository<RecallSarEntity, Integer> {

  @EntityGraph(
    attributePaths = [
      "recallType",
      "recallSentences",
      "recallSentences.sentence",
      "recallSentences.sentence.sentenceType",
      "recallSentences.sentence.periodLengths",
    ],
  )
  fun findByPrisonerId(prisonerId: String): List<RecallSarEntity>
}
