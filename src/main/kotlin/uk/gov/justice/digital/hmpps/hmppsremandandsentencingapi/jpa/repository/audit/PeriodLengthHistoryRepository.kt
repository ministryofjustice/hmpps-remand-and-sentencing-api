package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.audit

import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.audit.PeriodLengthHistoryEntity

interface PeriodLengthHistoryRepository : CrudRepository<PeriodLengthHistoryEntity, Int> {
  @Modifying
  @Query(
    """
    DELETE FROM period_length_history
    WHERE original_period_length_id IN (
      SELECT DISTINCT pl.id
      FROM period_length pl
       JOIN sentence s ON pl.sentence_id = s.id
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

  @Modifying
  @Query(
    """
    DELETE FROM period_length_history
      WHERE original_period_length_id IN (
        SELECT DISTINCT pl.id
        FROM period_length pl
        JOIN court_appearance a ON pl.appearance_id = a.id
        join court_case cc on a.court_case_id = cc.id
        where cc.prisoner_id = :prisonerId
      )
  """,
    nativeQuery = true,
  )
  fun deleteByAppearanceCourtCasePrisonerId(@Param("prisonerId") prisonerId: String)
}
