package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

import jakarta.transaction.Transactional
import jakarta.transaction.Transactional.TxType
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateCharge
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.error.ImmutableChargeException
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.ChargeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.ChargeOutcomeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.ChargeOutcomeRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.ChargeRepository
import java.util.UUID

@Service
class ChargeService(private val chargeRepository: ChargeRepository, private val chargeOutcomeRepository: ChargeOutcomeRepository) {

  @Transactional(TxType.REQUIRED)
  fun createCharge(charge: CreateCharge): ChargeEntity {
    val outcome = chargeOutcomeRepository.findByOutcomeName(charge.outcome) ?: chargeOutcomeRepository.save(ChargeOutcomeEntity(outcomeName = charge.outcome))
    val toCreateCharge = charge.chargeUuid?.let { chargeRepository.findByChargeUuid(it) }
      ?.let { chargeEntity ->
        if (chargeEntity.statusId == EntityStatus.EDITED) {
          throw ImmutableChargeException("Cannot edit an already edited charge")
        }
        val compareCharge = ChargeEntity.from(charge, outcome)
        if (chargeEntity.isSame(compareCharge)) {
          return@let chargeEntity
        }
        chargeEntity.statusId = EntityStatus.EDITED
        compareCharge.chargeUuid = UUID.randomUUID()
        compareCharge.supersedingCharge = chargeEntity
        compareCharge.lifetimeChargeUuid = chargeEntity.lifetimeChargeUuid
        compareCharge
      } ?: ChargeEntity.from(charge, outcome)
    return chargeRepository.save(toCreateCharge)
  }

  @Transactional(TxType.REQUIRED)
  fun deleteCharge(charge: ChargeEntity) {
    charge.statusId = EntityStatus.DELETED
  }

  fun findChargeByUuid(chargeUuid: UUID): ChargeEntity? = chargeRepository.findByChargeUuid(chargeUuid)
}
