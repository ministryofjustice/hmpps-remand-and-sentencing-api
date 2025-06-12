package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository

import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.ChargeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityStatus
import java.util.UUID

interface ChargeRepository : CrudRepository<ChargeEntity, Int> {

  fun findFirstByChargeUuidAndStatusIdNotOrderByUpdatedAtDesc(chargeUuid: UUID, status: EntityStatus = EntityStatus.DELETED): ChargeEntity?

  fun findByChargeUuid(chargeUuid: UUID): List<ChargeEntity>

  fun findFirstByAppearanceChargesAppearanceAppearanceUuidAndChargeUuidOrderByCreatedAtDesc(
    appearanceUuid: UUID,
    chargeUuid: UUID,
  ): ChargeEntity?

  fun findByChargeUuidAndStatusId(lifetimeUUID: UUID, status: EntityStatus): List<ChargeEntity>

  @Modifying
  @Query(
    """
        DELETE FROM ChargeEntity c 
        WHERE EXISTS (
            SELECT 1 FROM AppearanceChargeEntity ac
            JOIN CourtAppearanceEntity a ON ac.appearance.id = a.id
            WHERE ac.charge.id = c.id
            AND a.courtCase.id = :caseId
        )
    """,
  )
  fun deleteAllByAppearanceCourtCaseId(caseId: Int): Int
}
