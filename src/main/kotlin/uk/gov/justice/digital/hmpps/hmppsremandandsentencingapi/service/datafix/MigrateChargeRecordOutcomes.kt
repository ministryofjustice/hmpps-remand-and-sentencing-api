package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.datafix

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.ChargeOutcomeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.ChargeRepository

@Service
class MigrateChargeRecordOutcomes(private val chargeRepository: ChargeRepository) {

  @Async
  @Transactional
  fun migrateChargeRecordsToOutcome(chargeOutcomeEntity: ChargeOutcomeEntity) {
    log.info("starting migrating charge records with nomis code ${chargeOutcomeEntity.nomisCode}")
    chargeRepository.updateToSupportedChargeOutcome(chargeOutcomeEntity.id, chargeOutcomeEntity.nomisCode)
    log.info("finished migrating charge records with nomis code ${chargeOutcomeEntity.nomisCode}")
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
