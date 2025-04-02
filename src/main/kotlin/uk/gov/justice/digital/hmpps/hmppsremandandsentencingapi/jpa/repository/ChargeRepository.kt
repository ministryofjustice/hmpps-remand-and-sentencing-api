package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository

import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.ChargeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityStatus
import java.util.UUID

interface ChargeRepository : CrudRepository<ChargeEntity, Int> {

  @Query("select c from ChargeEntity c where c.chargeUuid = :chargeUuid order by c.updatedAt desc nulls last limit 1")
  fun findFirstByChargeUuid(chargeUuid: UUID): ChargeEntity?

  fun findFirstByAppearanceChargesAppearanceAppearanceUuidAndChargeUuidOrderByCreatedAtDesc(
    appearanceUuid: UUID,
    chargeUuid: UUID,
  ): ChargeEntity?

  fun findByChargeUuidAndStatusId(lifetimeUUID: UUID, status: EntityStatus): List<ChargeEntity>
}
