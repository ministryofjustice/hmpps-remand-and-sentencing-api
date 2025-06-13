package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.audit

import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.audit.CourtAppearanceHistoryEntity

interface CourtAppearanceHistoryRepository : CrudRepository<CourtAppearanceHistoryEntity, Int> {
  @Modifying
  @Query(
    """
    DELETE FROM CourtAppearanceHistoryEntity c 
    WHERE c.originalAppearance.courtCase.id = :caseId
    """,
  )
  fun deleteAllByOriginalAppearanceCourtCaseId(caseId: Int)
}
