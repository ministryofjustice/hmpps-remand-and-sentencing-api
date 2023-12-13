package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

import jakarta.transaction.Transactional
import jakarta.transaction.Transactional.TxType
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateCourtAppearance
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.AppearanceOutcomeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.CourtAppearanceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.CourtCaseEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.NextCourtAppearanceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.AppearanceOutcomeRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.CourtAppearanceRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.NextCourtAppearanceRepository
import java.util.UUID

@Service
class CourtAppearanceService(
  private val courtAppearanceRepository: CourtAppearanceRepository,
  private val nextCourtAppearanceRepository: NextCourtAppearanceRepository,
  private val appearanceOutcomeRepository: AppearanceOutcomeRepository,
  private val chargeService: ChargeService,
  private val serviceUserService: ServiceUserService,
) {

  @Transactional(TxType.REQUIRED)
  fun createCourtAppearance(courtAppearance: CreateCourtAppearance, courtCaseEntity: CourtCaseEntity): CourtAppearanceEntity {
    val appearanceOutcome = appearanceOutcomeRepository.findByOutcomeName(courtAppearance.outcome) ?: appearanceOutcomeRepository.save(AppearanceOutcomeEntity(outcomeName = courtAppearance.outcome))
    val charges = courtAppearance.charges.map { chargeService.createCharge(it) }
    val toCreateAppearance = courtAppearance.appearanceUuid?.let { courtAppearanceRepository.findByAppearanceUuid(it) }
      ?.let { courtAppearanceEntity ->
        val compareAppearance = CourtAppearanceEntity.from(courtAppearance, appearanceOutcome, courtCaseEntity, serviceUserService.getUsername(), charges.toSet())
        if (courtAppearanceEntity.isSame(compareAppearance)) {
          return courtAppearanceEntity
        }
        compareAppearance.previousAppearance = courtAppearanceEntity
        compareAppearance.appearanceUuid = UUID.randomUUID()
        return compareAppearance
      } ?: CourtAppearanceEntity.from(courtAppearance, appearanceOutcome, courtCaseEntity, serviceUserService.getUsername(), charges.toSet())
    val nextCourtAppearance = courtAppearance.nextCourtAppearance?.let { NextCourtAppearanceEntity.from(it) }
    if (toCreateAppearance.nextCourtAppearance?.isSame(nextCourtAppearance) == false) {
      val toSaveNextCourtAppearance = nextCourtAppearance?.let { nextCourtAppearanceRepository.save(it) }
      toCreateAppearance.nextCourtAppearance = toSaveNextCourtAppearance
    }
    return courtAppearanceRepository.save(toCreateAppearance)
  }
}
