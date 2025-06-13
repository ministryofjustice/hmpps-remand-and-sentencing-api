package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.audit

import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.audit.PeriodLengthHistoryEntity

interface PeriodLengthHistoryRepository : CrudRepository<PeriodLengthHistoryEntity, Int> {
  @Modifying
  @Query(
    """
        DELETE FROM PeriodLengthHistoryEntity plh 
        WHERE plh.originalPeriodLength IN (
            SELECT pl FROM PeriodLengthEntity pl 
            JOIN pl.sentenceEntity s 
            JOIN s.charge c 
            JOIN c.appearanceCharges ac 
            JOIN ac.appearance a 
            WHERE a.courtCase.id = :caseId
        )
    """,
  )
  fun deleteByCourtCaseId(caseId: Int)
}
