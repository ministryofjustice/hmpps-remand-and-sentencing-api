package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.audit

import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.audit.SentenceHistoryEntity

interface SentenceHistoryRepository : CrudRepository<SentenceHistoryEntity, Int> {
  @Modifying
  @Query(
    """
        DELETE FROM SentenceHistoryEntity sh 
        WHERE sh.originalSentence IN (
            SELECT s FROM SentenceEntity s 
            JOIN s.charge c 
            JOIN c.appearanceCharges ac 
            JOIN ac.appearance a 
            WHERE a.courtCase.id = :caseId
        )
    """,
  )
  fun deleteAllByOriginalSentenceChargeAppearanceCourtCaseId(caseId: Int)
}
