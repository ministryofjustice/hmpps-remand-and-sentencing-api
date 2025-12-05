package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.audit

import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.audit.CourtAppearanceHistoryEntity

interface CourtAppearanceHistoryRepository : CrudRepository<CourtAppearanceHistoryEntity, Int> {
  @Modifying
  @Query(
    """
    DELETE FROM court_appearance_history
    WHERE original_appearance_id IN (
        SELECT a.id
        FROM court_appearance a
        JOIN court_case cc ON a.court_case_id = cc.id
        WHERE cc.prisoner_id = :prisonerId
    )
  """,
    nativeQuery = true,
  )
  fun deleteByCourtCasePrisonerId(@Param("prisonerId") prisonerId: String)
}
