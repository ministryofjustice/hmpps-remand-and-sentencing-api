package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository

import org.springframework.data.repository.CrudRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.RecallSentenceEntity

interface RecallSentenceRepository : CrudRepository<RecallSentenceEntity, Int> {
  fun findByRecallId(recallId: Int): List<RecallSentenceEntity>?
}
