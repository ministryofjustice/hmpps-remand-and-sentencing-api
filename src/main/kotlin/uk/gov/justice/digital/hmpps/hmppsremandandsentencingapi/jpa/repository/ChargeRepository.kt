package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository

import org.springframework.data.repository.CrudRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.ChargeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityStatus
import java.util.UUID

interface ChargeRepository : CrudRepository<ChargeEntity, Int> {
  fun findByChargeUuid(chargeUuid: UUID): ChargeEntity?

  fun findFirstByChargeUuidOrderByCreatedAtDesc(lifetimeUUID: UUID): ChargeEntity?

  fun findFirstByCourtAppearancesAppearanceUuidAndChargeUuidOrderByCreatedAtDesc(courtAppearanceLifetimeUuid: UUID, lifetimeUUID: UUID): ChargeEntity?

  fun findByChargeUuidAndStatusId(lifetimeUUID: UUID, status: EntityStatus): List<ChargeEntity>

  fun findByChargeUuidInAndStatusId(lifetimeUUIDs: List<UUID>, status: EntityStatus): List<ChargeEntity>
}
