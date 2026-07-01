package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.service

import jakarta.persistence.EntityNotFoundException
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.config.FeaturesConfig
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.EventMetadata
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.EventType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.util.EventMetadataCreator
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.ChargeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.CourtAppearanceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.NextCourtAppearanceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.audit.AppearanceChargeHistoryEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.audit.ChargeHistoryEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.audit.CourtAppearanceHistoryEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.audit.ImmigrationDetentionHistoryEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ChangeSource
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.CourtAppearanceEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.CourtCaseEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityChangeStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.AppearanceOutcomeRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.ChargeRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.CourtAppearanceRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.CourtCaseRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.ImmigrationDetentionRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.NextCourtAppearanceRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.audit.AppearanceChargeHistoryRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.audit.ChargeHistoryRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.audit.CourtAppearanceHistoryRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.audit.ImmigrationDetentionHistoryRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacyCourtAppearance
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacyCourtAppearanceCreatedResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacyCreateCourtAppearance
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.util.CourtAppearanceUtils
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.CourtCaseService
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.ImmigrationDetentionService
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.NextCourtAppearanceService
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.ServiceUserService
import java.time.LocalDate
import java.util.*

@Service
class LegacyCourtAppearanceService(
  private val courtAppearanceRepository: CourtAppearanceRepository,
  private val courtCaseRepository: CourtCaseRepository,
  private val appearanceOutcomeRepository: AppearanceOutcomeRepository,
  private val serviceUserService: ServiceUserService,
  private val chargeRepository: ChargeRepository,
  private val nextCourtAppearanceRepository: NextCourtAppearanceRepository,
  private val courtAppearanceHistoryRepository: CourtAppearanceHistoryRepository,
  private val chargeHistoryRepository: ChargeHistoryRepository,
  private val appearanceChargeHistoryRepository: AppearanceChargeHistoryRepository,
  private val immigrationDetentionRepository: ImmigrationDetentionRepository,
  private val immigrationDetentionHistoryRepository: ImmigrationDetentionHistoryRepository,
  private val courtCaseService: CourtCaseService,
  private val immigrationDetentionService: ImmigrationDetentionService,
  private val nextCourtAppearanceService: NextCourtAppearanceService,
  private val featuresConfig: FeaturesConfig,
) {

  @Transactional
  fun create(courtAppearance: LegacyCreateCourtAppearance): LegacyCourtAppearanceCreatedResponse {
    val courtCase = courtCaseRepository.findByCaseUniqueIdentifier(courtAppearance.courtCaseUuid)?.takeUnless { entity -> entity.statusId == CourtCaseEntityStatus.DELETED } ?: throw EntityNotFoundException("No court case found at ${courtAppearance.courtCaseUuid}")
    val dpsOutcome = courtAppearance.legacyData.nomisOutcomeCode?.let { nomisCode -> appearanceOutcomeRepository.findByNomisCode(nomisCode) }
    val createdCourtAppearance = courtAppearanceRepository.save(
      CourtAppearanceEntity.from(courtAppearance, dpsOutcome, courtCase, getPerformedByUsername(courtAppearance), featuresConfig.appeals.enabled),
    )
    courtAppearanceHistoryRepository.save(
      CourtAppearanceHistoryEntity.from(
        createdCourtAppearance,
        ChangeSource.NOMIS,
      ),
    )
    courtCaseService.handleUpdatingLatestCourtAppearanceInCourtCase(courtCase, createdCourtAppearance, getPerformedByUsername(courtAppearance))
    nextCourtAppearanceService.handleNextCourtAppearance(createdCourtAppearance, courtAppearance.legacyData.nomisAppearanceTypeCode, getPerformedByUsername(courtAppearance), courtAppearance.getAppearanceDateTime()) {
      NextCourtAppearanceEntity.from(courtAppearance, createdCourtAppearance, it)
    }
    return LegacyCourtAppearanceCreatedResponse(createdCourtAppearance.appearanceUuid, courtCase.caseUniqueIdentifier, courtCase.prisonerId)
  }

  @Transactional
  fun update(lifetimeUuid: UUID, courtAppearance: LegacyCreateCourtAppearance): Pair<EntityChangeStatus, LegacyCourtAppearanceCreatedResponse> {
    var entityChangeStatus = EntityChangeStatus.NO_CHANGE
    val existingCourtAppearance = getUnlessDeleted(lifetimeUuid)
    val dpsOutcome = courtAppearance.legacyData.nomisOutcomeCode?.let { nomisCode -> appearanceOutcomeRepository.findByNomisCode(nomisCode) }
    val performedByUser = getPerformedByUsername(courtAppearance)
    val updatedCourtAppearance = existingCourtAppearance.copyFrom(courtAppearance, dpsOutcome, performedByUser, featuresConfig.appeals.enabled)
    if (!existingCourtAppearance.isSame(updatedCourtAppearance) && !isOvernightRun(existingCourtAppearance, updatedCourtAppearance)) {
      existingCourtAppearance.updateFrom(updatedCourtAppearance)
      courtAppearanceHistoryRepository.save(
        CourtAppearanceHistoryEntity.from(
          existingCourtAppearance,
          ChangeSource.NOMIS,
        ),
      )
      courtCaseService.handleUpdatingLatestCourtAppearanceInCourtCase(existingCourtAppearance.courtCase, existingCourtAppearance, performedByUser)
      entityChangeStatus = EntityChangeStatus.EDITED
    }
    nextCourtAppearanceService.handleNextCourtAppearance(existingCourtAppearance, courtAppearance.legacyData.nomisAppearanceTypeCode, getPerformedByUsername(courtAppearance), courtAppearance.getAppearanceDateTime()) {
      NextCourtAppearanceEntity.from(courtAppearance, existingCourtAppearance, it)
    }
    immigrationDetentionService.handleImmigrationDetention(existingCourtAppearance, performedByUser) {
      it.copyFrom(existingCourtAppearance, courtAppearance, performedByUser)
    }
    return entityChangeStatus to LegacyCourtAppearanceCreatedResponse(lifetimeUuid, updatedCourtAppearance.courtCase.caseUniqueIdentifier, updatedCourtAppearance.courtCase.prisonerId)
  }

  fun isOvernightRun(existingCourtAppearance: CourtAppearanceEntity, updatedCourtAppearance: CourtAppearanceEntity): Boolean = existingCourtAppearance.appearanceDate.isEqual(LocalDate.now().minusDays(1)) &&
    existingCourtAppearance.statusId == CourtAppearanceEntityStatus.FUTURE &&
    updatedCourtAppearance.statusId == CourtAppearanceEntityStatus.ACTIVE &&
    existingCourtAppearance.isSameOtherThanStatusId(updatedCourtAppearance)

  private fun getPerformedByUsername(courtAppearance: LegacyCreateCourtAppearance): String = courtAppearance.performedByUser ?: serviceUserService.getUsername()

  @Transactional
  fun get(lifetimeUuid: UUID): LegacyCourtAppearance {
    val courtAppearance = getUnlessDeleted(lifetimeUuid)
    val associatedNextCourtAppearance = nextCourtAppearanceRepository.findFirstByFutureSkeletonAppearance(courtAppearance)
    val appearanceTypeUuid = associatedNextCourtAppearance?.appearanceType?.appearanceTypeUuid ?: LegacyAppearanceTypeService.DEFAULT_APPEARANCE_TYPE_UUID
    val nomisAppearanceTypeCode = CourtAppearanceUtils.getNOMISAppearanceTypeCode(courtAppearance, associatedNextCourtAppearance)
    val startTime = CourtAppearanceUtils.getStartTime(courtAppearance, associatedNextCourtAppearance)
    return LegacyCourtAppearance.from(courtAppearance, appearanceTypeUuid, nomisAppearanceTypeCode, startTime)
  }

  @Retryable(maxAttempts = 3, retryFor = [OptimisticLockingFailureException::class])
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  fun delete(lifetimeUuid: UUID, performedByUser: String?): MutableSet<EventMetadata> {
    val eventsToEmit: MutableSet<EventMetadata> = mutableSetOf()
    val existingCourtAppearance = getUnlessDeleted(lifetimeUuid)
    val performedByUsername = performedByUser ?: serviceUserService.getUsername()
    existingCourtAppearance.delete(performedByUsername)
    courtAppearanceHistoryRepository.save(
      CourtAppearanceHistoryEntity.from(
        existingCourtAppearance,
        ChangeSource.NOMIS,
      ),
    )
    existingCourtAppearance.appearanceCharges.removeAll { appearanceCharge ->

      if (appearanceCharge.charge!!.hasNoLiveCourtAppearances()) {
        appearanceCharge.charge!!.delete(performedByUsername)
        chargeHistoryRepository.save(
          ChargeHistoryEntity.from(
            appearanceCharge.charge!!,
            ChangeSource.NOMIS,
          ),
        )
        eventsToEmit.add(
          EventMetadataCreator.chargeEventMetadata(
            existingCourtAppearance.courtCase.prisonerId,
            existingCourtAppearance.courtCase.caseUniqueIdentifier,
            existingCourtAppearance.appearanceUuid.toString(),
            appearanceCharge.charge!!.chargeUuid.toString(),
            EventType.CHARGE_DELETED,
            existingCourtAppearance.statusId == CourtAppearanceEntityStatus.FUTURE,
          ),
        )
      }
      appearanceChargeHistoryRepository.save(
        AppearanceChargeHistoryEntity.removedFrom(
          appearanceCharge,
          performedByUsername,
          null,
          ChangeSource.NOMIS,
        ),
      )
      appearanceCharge.charge!!.appearanceCharges.remove(appearanceCharge)
      true
    }
    courtCaseService.handleUpdatingLatestCourtAppearanceInCourtCase(existingCourtAppearance.courtCase, existingCourtAppearance, performedByUsername)
    nextCourtAppearanceRepository.deleteByFutureSkeletonAppearance(existingCourtAppearance)
    eventsToEmit.add(
      EventMetadataCreator.courtAppearanceEventMetadata(
        existingCourtAppearance.courtCase.prisonerId,
        existingCourtAppearance.courtCase.caseUniqueIdentifier,
        existingCourtAppearance.appearanceUuid.toString(),
        EventType.COURT_APPEARANCE_DELETED,
      ),
    )
    immigrationDetentionRepository.findByCourtAppearanceUuidAndStatusId(lifetimeUuid).forEach { immigrationDetentionEntity ->
      immigrationDetentionEntity.delete(performedByUsername)
      immigrationDetentionHistoryRepository.save(ImmigrationDetentionHistoryEntity.from(immigrationDetentionEntity))
    }
    return eventsToEmit
  }

  @Retryable(maxAttempts = 3, retryFor = [OptimisticLockingFailureException::class])
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  fun unlinkAppearanceWithCharge(lifetimeUuid: UUID, lifetimeChargeUuid: UUID, performedByUser: String?): MutableSet<EventMetadata> {
    val eventsToEmit: MutableSet<EventMetadata> = mutableSetOf()
    val existingCourtAppearance = getUnlessDeleted(lifetimeUuid)
    val existingCharge = getChargeAtAppearanceUnlessDeleted(lifetimeUuid, lifetimeChargeUuid)
    val performedByUsername = performedByUser ?: serviceUserService.getUsername()
    if (existingCharge != null) {
      existingCourtAppearance.appearanceCharges.firstOrNull { it.charge == existingCharge }?.let { appearanceCharge ->
        existingCourtAppearance.appearanceCharges.remove(appearanceCharge)
        existingCharge.appearanceCharges.remove(appearanceCharge)
        appearanceChargeHistoryRepository.save(
          AppearanceChargeHistoryEntity.removedFrom(
            appearanceCharge = appearanceCharge,
            removedBy = performedByUsername,
            removedPrison = null,
            ChangeSource.NOMIS,
          ),
        )
        eventsToEmit.add(
          EventMetadataCreator.courtAppearanceEventMetadata(
            existingCourtAppearance.courtCase.prisonerId,
            existingCourtAppearance.courtCase.caseUniqueIdentifier,
            existingCourtAppearance.appearanceUuid.toString(),
            EventType.COURT_APPEARANCE_UPDATED,
          ),
        )
      }

      if (existingCharge.hasNoLiveCourtAppearances()) {
        existingCharge.delete(performedByUsername)
        chargeHistoryRepository.save(
          ChargeHistoryEntity.from(
            existingCharge,
            ChangeSource.NOMIS,
          ),
        )
        eventsToEmit.add(
          EventMetadataCreator.chargeEventMetadata(
            existingCourtAppearance.courtCase.prisonerId,
            existingCourtAppearance.courtCase.caseUniqueIdentifier,
            existingCourtAppearance.appearanceUuid.toString(),
            existingCharge.chargeUuid.toString(),
            EventType.CHARGE_DELETED,
            existingCourtAppearance.statusId == CourtAppearanceEntityStatus.FUTURE,
          ),
        )
      }
    }

    return eventsToEmit
  }

  private fun getUnlessDeleted(appearanceUuid: UUID): CourtAppearanceEntity = courtAppearanceRepository.findByAppearanceUuid(appearanceUuid)
    ?.takeUnless { entity -> entity.statusId == CourtAppearanceEntityStatus.DELETED } ?: throw EntityNotFoundException("No court appearance found at $appearanceUuid")

  private fun getChargeAtAppearanceUnlessDeleted(appearanceUuid: UUID, chargeUuid: UUID): ChargeEntity? = chargeRepository.findFirstByAppearanceChargesAppearanceAppearanceUuidAndChargeUuidAndStatusIdNotOrderByCreatedAtDesc(
    appearanceUuid,
    chargeUuid,
  )
}
