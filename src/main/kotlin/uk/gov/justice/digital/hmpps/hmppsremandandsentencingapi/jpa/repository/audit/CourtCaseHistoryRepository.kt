package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.audit

import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.audit.CourtCaseHistoryEntity

interface CourtCaseHistoryRepository : CrudRepository<CourtCaseHistoryEntity, Int> {
  @Modifying
  @Query(
    """
    DELETE FROM court_case_history WHERE prisoner_id = :prisonerId
  """,
    nativeQuery = true,
  )
  fun deleteByPrisonerId(@Param("prisonerId") prisonerId: String)
}
