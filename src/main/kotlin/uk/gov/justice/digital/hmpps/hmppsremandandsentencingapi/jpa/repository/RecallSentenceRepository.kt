package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository

import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.RecallSentenceEntity

interface RecallSentenceRepository : CrudRepository<RecallSentenceEntity, Int> {
  fun findByRecallId(recallId: Int): List<RecallSentenceEntity>?

  @Modifying
  @Query(
    """
    DELETE FROM recall_sentence rs
      WHERE rs.recall_id IN (
        SELECT id FROM recall r WHERE r.prisoner_id = :prisonerId
      )
  """,
    nativeQuery = true,
  )
  fun deleteByRecallPrisonerId(prisonerId: String)
}
