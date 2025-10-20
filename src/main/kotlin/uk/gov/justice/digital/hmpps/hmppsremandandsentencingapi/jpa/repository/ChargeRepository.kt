package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository

import org.springframework.data.repository.CrudRepository
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

  fun findByChargeUuidAndStatusId(lifetimeUUID: UUID, status: ChargeEntityStatus): List<ChargeEntity>
}
