package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.audit

import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.audit.ChargeHistoryEntity

interface ChargeHistoryRepository : CrudRepository<ChargeHistoryEntity, Int> {
  @Modifying
  @Query(
    """
    DELETE FROM ChargeHistoryEntity ch 
    WHERE ch.originalCharge.id IN (
        SELECT c.id FROM ChargeEntity c
        JOIN AppearanceChargeEntity ac ON ac.charge = c
        JOIN CourtAppearanceEntity a ON ac.appearance = a
        WHERE a.courtCase.id = :caseId
    )
""",
  )
  fun deleteAllByAppearanceCourtCaseId(@Param("caseId") caseId: Int)
}
