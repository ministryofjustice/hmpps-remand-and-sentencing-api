package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository

import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.ChargeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ChargeEntityStatus
import java.util.*

interface ChargeRepository : CrudRepository<ChargeEntity, Int> {

  fun findFirstByChargeUuidAndStatusIdNotOrderByUpdatedAtDesc(chargeUuid: UUID, status: ChargeEntityStatus = ChargeEntityStatus.DELETED): ChargeEntity?

  fun findByChargeUuid(chargeUuid: UUID): List<ChargeEntity>

  fun findFirstByAppearanceChargesAppearanceAppearanceUuidAndChargeUuidAndStatusIdNotOrderByCreatedAtDesc(
    appearanceUuid: UUID,
    chargeUuid: UUID,
    status: ChargeEntityStatus = ChargeEntityStatus.DELETED,
  ): ChargeEntity?

  fun findByChargeUuidAndStatusIdNot(lifetimeUUID: UUID, status: ChargeEntityStatus): List<ChargeEntity>

  @Modifying
  @Query(
    """
    update charge set superseding_charge_id = null where superseding_charge_id in (
      SELECT c.id
      FROM charge c
      JOIN appearance_charge ac ON ac.charge_id = c.id
      JOIN court_appearance a ON ac.appearance_id = a.id
      JOIN court_case cc ON a.court_case_id = cc.id
      WHERE cc.prisoner_id = :prisonerId)
  """,
    nativeQuery = true,
  )
  fun updateSupersedingChargeIdNullByCourtCasePrisonerId(@Param("prisonerId") prisonerId: String)

  @Modifying
  @Query(
    """
    update charge set superseding_charge_id = null where superseding_charge_id in (
      SELECT c.id
      FROM charge c
      JOIN court_case mfcc ON c.merged_from_case_id = mfcc.id
      WHERE mfcc.prisoner_id = :prisonerId)
  """,
    nativeQuery = true,
  )
  fun updateSupersedingChargeIdNullByMergedCourtCasePrisonerId(@Param("prisonerId") prisonerId: String)

  @Modifying
  @Query(
    """
    WITH deleted_charges AS (
      DELETE FROM appearance_charge
      WHERE charge_id IN (
        SELECT ac.charge_id
        FROM appearance_charge ac
        JOIN court_appearance a ON ac.appearance_id = a.id
        JOIN court_case cc ON a.court_case_id = cc.id
        WHERE cc.prisoner_id = :prisonerId
      )
    RETURNING charge_id
    )
    DELETE FROM charge
    WHERE id IN (SELECT charge_id FROM deleted_charges)
  """,
    nativeQuery = true,
  )
  fun deleteByChargeCourtCasePrisonerId(@Param("prisonerId") prisonerId: String)

  @Modifying
  @Query(
    """
    DELETE FROM charge
    WHERE id IN (
      SELECT c.id
      FROM charge c
      JOIN court_case mfcc ON c.merged_from_case_id = mfcc.id
      WHERE mfcc.prisoner_id = :prisonerId)
  """,
    nativeQuery = true,
  )
  fun deleteByChargeMergedFromCasePrisonerId(@Param("prisonerId") prisonerId: String)

  @Modifying
  @Query(
    """
      update charge set charge_outcome_id = :chargeOutcomeId where legacy_data->>'nomisOutcomeCode' = :nomisCode
    """,
    nativeQuery = true,
  )
  fun updateToSupportedChargeOutcome(@Param("chargeOutcomeId") chargeOutcomeId: Int, @Param("nomisCode") nomisCode: String)
}
