package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository

import org.springframework.data.repository.CrudRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.ChargeEntity
import java.util.UUID

interface ChargeRepository : CrudRepository<ChargeEntity, Int> {
  fun findByChargeUuid(chargeUuid: UUID): ChargeEntity?

  fun findFirstByLifetimeChargeUuidOrderByCreatedAtDesc(lifetimeUUID: UUID): ChargeEntity?
}
