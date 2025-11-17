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
    DELETE FROM charge_history ch
    WHERE original_charge_id IN (
    SELECT c.id
    FROM charge c
      JOIN appearance_charge ac ON ac.charge_id = c.id
      JOIN court_appearance a ON ac.appearance_id = a.id
      JOIN court_case cc ON a.court_case_id = cc.id
      WHERE cc.prisoner_id = :prisonerId
    )
  """,
    nativeQuery = true,
  )
  fun deleteByCourtCasePrisonerId(@Param("prisonerId") prisonerId: String)

  @Modifying
  @Query(
    """
    DELETE from charge_history ch
    WHERE original_charge_id IN (
      SELECT c.id
      FROM charge c
      JOIN court_case mfcc ON c.merged_from_case_id = mfcc.id
      WHERE mfcc.prisoner_id = :prisonerId
    )
  """,
    nativeQuery = true,
  )
  fun deleteByChargeMergedFromCasePrisonerId(@Param("prisonerId") prisonerId: String)
}
