package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.audit

import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.audit.SentenceHistoryEntity

interface SentenceHistoryRepository : CrudRepository<SentenceHistoryEntity, Int> {
  @Modifying
  @Query(
    """
    DELETE FROM sentence_history
    WHERE original_sentence_id IN (
      SELECT s.id
      FROM sentence s
       JOIN charge c ON s.charge_id = c.id
       JOIN appearance_charge ac ON c.id = ac.charge_id
       JOIN court_appearance a ON ac.appearance_id = a.id
       join court_case cc on a.court_case_id = cc.id
       where cc.prisoner_id = :prisonerId
    )
  """,
    nativeQuery = true,
  )
  fun deleteBySentenceCourtCasePrisonerId(@Param("prisonerId") prisonerId: String)
}
