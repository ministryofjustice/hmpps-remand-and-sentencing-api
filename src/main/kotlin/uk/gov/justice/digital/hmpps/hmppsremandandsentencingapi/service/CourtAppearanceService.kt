package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CourtAppearance
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateCourtAppearance
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.error.ImmutableCourtAppearanceException
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.AppearanceOutcomeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.CourtAppearanceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.CourtCaseEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.NextCourtAppearanceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.AppearanceOutcomeRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.CourtAppearanceRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.CourtCaseRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.NextCourtAppearanceRepository
import java.util.UUID

@Service
class CourtAppearanceService(
  private val courtAppearanceRepository: CourtAppearanceRepository,
  private val nextCourtAppearanceRepository: NextCourtAppearanceRepository,
  private val appearanceOutcomeRepository: AppearanceOutcomeRepository,
  private val chargeService: ChargeService,
  private val serviceUserService: ServiceUserService,
  private val courtCaseRepository: CourtCaseRepository,
) {

  @Transactional
  fun createCourtAppearance(createCourtAppearance: CreateCourtAppearance): CourtAppearanceEntity? {
    return courtCaseRepository.findByCaseUniqueIdentifier(createCourtAppearance.courtCaseUuid!!)?.let { courtCaseEntity ->
      val courtAppearance = createCourtAppearance(createCourtAppearance, courtCaseEntity)
      courtCaseEntity.latestCourtAppearance = CourtAppearanceEntity.getLatestCourtAppearance(courtCaseEntity.appearances + courtAppearance)
      return courtAppearance
    }
  }

  @Transactional
  fun createCourtAppearance(courtAppearance: CreateCourtAppearance, courtCaseEntity: CourtCaseEntity): CourtAppearanceEntity {
    val appearanceOutcome = appearanceOutcomeRepository.findByOutcomeName(courtAppearance.outcome) ?: appearanceOutcomeRepository.save(AppearanceOutcomeEntity(outcomeName = courtAppearance.outcome))
    val charges = courtAppearance.charges.map { chargeService.createCharge(it) }
    val toCreateAppearance = courtAppearance.appearanceUuid?.let { courtAppearanceRepository.findByAppearanceUuid(it) }
      ?.let { courtAppearanceEntity ->
        if (courtAppearanceEntity.statusId == EntityStatus.EDITED) {
          throw ImmutableCourtAppearanceException("Cannot edit an already edited court appearance")
        }
        val compareAppearance = CourtAppearanceEntity.from(courtAppearance, appearanceOutcome, courtCaseEntity, serviceUserService.getUsername(), charges.toSet())
        if (courtAppearanceEntity.isSame(compareAppearance)) {
          return@let courtAppearanceEntity
        }
        courtAppearanceEntity.statusId = EntityStatus.EDITED
        compareAppearance.previousAppearance = courtAppearanceEntity
        compareAppearance.appearanceUuid = UUID.randomUUID()
        compareAppearance
      } ?: CourtAppearanceEntity.from(courtAppearance, appearanceOutcome, courtCaseEntity, serviceUserService.getUsername(), charges.toSet())

    val nextCourtAppearance = courtAppearance.nextCourtAppearance?.let { NextCourtAppearanceEntity.from(it) }
    if (toCreateAppearance.nextCourtAppearance?.isSame(nextCourtAppearance) == false) {
      val toSaveNextCourtAppearance = nextCourtAppearance?.let { nextCourtAppearanceRepository.save(it) }
      toCreateAppearance.nextCourtAppearance = toSaveNextCourtAppearance
    }
    return courtAppearanceRepository.save(toCreateAppearance)
  }

  @Transactional
  fun deleteCourtAppearance(courtAppearanceEntity: CourtAppearanceEntity) {
    courtAppearanceEntity.statusId = EntityStatus.DELETED
  }

  @Transactional(readOnly = true)
  fun findAppearanceByUuid(appearanceUuid: UUID): CourtAppearance? = courtAppearanceRepository.findByAppearanceUuid(appearanceUuid)?.let { CourtAppearance.from(it) }
}
