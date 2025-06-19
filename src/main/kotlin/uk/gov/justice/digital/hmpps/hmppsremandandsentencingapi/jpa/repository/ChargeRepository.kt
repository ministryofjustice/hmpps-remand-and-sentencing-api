package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository

import org.springframework.data.repository.CrudRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.ChargeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityStatus
import java.util.UUID

interface ChargeRepository : CrudRepository<ChargeEntity, Int> {

  fun findFirstByChargeUuidAndStatusIdNotOrderByUpdatedAtDesc(chargeUuid: UUID, status: EntityStatus = EntityStatus.DELETED): ChargeEntity?

  fun findByChargeUuid(chargeUuid: UUID): List<ChargeEntity>

  fun findFirstByAppearanceChargesAppearanceAppearanceUuidAndChargeUuidAndStatusIdNotOrderByCreatedAtDesc(
    appearanceUuid: UUID,
    chargeUuid: UUID,
    status: EntityStatus = EntityStatus.DELETED,
  ): ChargeEntity?

  fun findByChargeUuidAndStatusId(lifetimeUUID: UUID, status: EntityStatus): List<ChargeEntity>
}
