package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.datafix

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.AppearanceOutcomeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.CourtAppearanceRepository

@Service
class MigrateCourtAppearanceRecordOutcomes(private val courtAppearanceRepository: CourtAppearanceRepository) {

  @Async
  @Transactional
  fun migrateCourtAppearanceRecordsToOutcome(appearanceOutcomeEntity: AppearanceOutcomeEntity) {
    log.info("starting migrating court appearance records with nomis code ${appearanceOutcomeEntity.nomisCode}")
    courtAppearanceRepository.updateToSupportedAppearanceOutcome(appearanceOutcomeEntity.id, appearanceOutcomeEntity.warrantType, appearanceOutcomeEntity.nomisCode)
    log.info("finished migrating court appearance records with nomis code ${appearanceOutcomeEntity.nomisCode}")
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
