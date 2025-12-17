package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.service

import jakarta.persistence.EntityNotFoundException
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.EventMetadata
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.EventType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.util.EventMetadataCreator
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.ChargeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.CourtAppearanceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.CourtCaseEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.NextCourtAppearanceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.audit.AppearanceChargeHistoryEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.audit.ChargeHistoryEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.audit.CourtAppearanceHistoryEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ChangeSource
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.CourtAppearanceEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.CourtCaseEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityChangeStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.AppearanceOutcomeRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.AppearanceTypeRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.ChargeRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.CourtAppearanceRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.CourtCaseRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.NextCourtAppearanceRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.audit.AppearanceChargeHistoryRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.audit.ChargeHistoryRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.audit.CourtAppearanceHistoryRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacyCourtAppearance
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacyCourtAppearanceCreatedResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacyCreateCourtAppearance
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.ServiceUserService
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.*

@Service
class LegacyCourtAppearanceService(
  private val courtAppearanceRepository: CourtAppearanceRepository,
  private val courtCaseRepository: CourtCaseRepository,
  private val appearanceOutcomeRepository: AppearanceOutcomeRepository,
  private val serviceUserService: ServiceUserService,
  private val chargeRepository: ChargeRepository,
  private val appearanceTypeRepository: AppearanceTypeRepository,
  private val nextCourtAppearanceRepository: NextCourtAppearanceRepository,
  private val courtAppearanceHistoryRepository: CourtAppearanceHistoryRepository,
  private val chargeHistoryRepository: ChargeHistoryRepository,
  private val appearanceChargeHistoryRepository: AppearanceChargeHistoryRepository,
) {

  @Transactional
  fun create(courtAppearance: LegacyCreateCourtAppearance): LegacyCourtAppearanceCreatedResponse {
    val courtCase = courtCaseRepository.findByCaseUniqueIdentifier(courtAppearance.courtCaseUuid)?.takeUnless { entity -> entity.statusId == CourtCaseEntityStatus.DELETED } ?: throw EntityNotFoundException("No court case found at ${courtAppearance.courtCaseUuid}")
    val dpsOutcome = courtAppearance.legacyData.nomisOutcomeCode?.let { nomisCode -> appearanceOutcomeRepository.findByNomisCode(nomisCode) }
    val createdCourtAppearance = courtAppearanceRepository.save(
      CourtAppearanceEntity.from(courtAppearance, dpsOutcome, courtCase, getPerformedByUsername(courtAppearance)),
    )
    courtAppearanceHistoryRepository.save(
      CourtAppearanceHistoryEntity.from(
        createdCourtAppearance,
        ChangeSource.NOMIS,
      ),
    )
    courtCase.latestCourtAppearance = CourtAppearanceEntity.getLatestCourtAppearance(courtCase.appearances + createdCourtAppearance)
    handleMatchingNextCourtAppearance(createdCourtAppearance, courtAppearance)
    return LegacyCourtAppearanceCreatedResponse(createdCourtAppearance.appearanceUuid, courtCase.caseUniqueIdentifier, courtCase.prisonerId)
  }

  @Transactional
  fun update(lifetimeUuid: UUID, courtAppearance: LegacyCreateCourtAppearance): Pair<EntityChangeStatus, LegacyCourtAppearanceCreatedResponse> {
    var entityChangeStatus = EntityChangeStatus.NO_CHANGE
    val existingCourtAppearance = getUnlessDeleted(lifetimeUuid)
    val dpsOutcome = courtAppearance.legacyData.nomisOutcomeCode?.let { nomisCode -> appearanceOutcomeRepository.findByNomisCode(nomisCode) }
    val updatedCourtAppearance = existingCourtAppearance.copyFrom(courtAppearance, dpsOutcome, getPerformedByUsername(courtAppearance))
    if (!existingCourtAppearance.isSame(updatedCourtAppearance)) {
      existingCourtAppearance.updateFrom(updatedCourtAppearance)
      courtAppearanceHistoryRepository.save(
        CourtAppearanceHistoryEntity.from(
          existingCourtAppearance,
          ChangeSource.NOMIS,
        ),
      )
      existingCourtAppearance.courtCase.latestCourtAppearance = CourtAppearanceEntity.getLatestCourtAppearance(existingCourtAppearance.courtCase.appearances + existingCourtAppearance)
      entityChangeStatus = EntityChangeStatus.EDITED
    }
    handleNextCourtAppearance(existingCourtAppearance, courtAppearance)

    return entityChangeStatus to LegacyCourtAppearanceCreatedResponse(lifetimeUuid, updatedCourtAppearance.courtCase.caseUniqueIdentifier, updatedCourtAppearance.courtCase.prisonerId)
  }

  private fun handleNextCourtAppearance(courtAppearance: CourtAppearanceEntity, updateRequest: LegacyCreateCourtAppearance) {
    nextCourtAppearanceRepository.findFirstByFutureSkeletonAppearance(courtAppearance)?.let { existingNextCourtAppearance ->
      val appearanceType = appearanceTypeRepository.findByAppearanceTypeUuid(updateRequest.appearanceTypeUuid) ?: throw EntityNotFoundException("No appearance type at ${updateRequest.appearanceTypeUuid}")
      val toUpdate = NextCourtAppearanceEntity.from(updateRequest, courtAppearance, appearanceType)
      existingNextCourtAppearance.updateFrom(toUpdate)
    } ?: handleMatchingNextCourtAppearance(courtAppearance, updateRequest)
  }

  private fun handleMatchingNextCourtAppearance(courtAppearance: CourtAppearanceEntity, legacyRequest: LegacyCreateCourtAppearance) {
    courtAppearance.takeIf { it.statusId == CourtAppearanceEntityStatus.FUTURE }?.let { getMatchedNextCourtAppearanceOrLatest(it.courtCase, legacyRequest.appearanceDate) }?.let { matchedCourtAppearance ->
      val appearanceType = appearanceTypeRepository.findByAppearanceTypeUuid(legacyRequest.appearanceTypeUuid) ?: throw EntityNotFoundException("No appearance type at ${legacyRequest.appearanceTypeUuid}")
      matchedCourtAppearance.nextCourtAppearance?.let { matchedNextCourtAppearance ->
        val toUpdate = NextCourtAppearanceEntity.from(legacyRequest, courtAppearance, appearanceType)
        matchedNextCourtAppearance.updateFrom(toUpdate)
      } ?: NextCourtAppearanceEntity.from(legacyRequest, courtAppearance, appearanceType).let { toCreateNextCourtAppearance ->
        val savedNextCourtAppearance = nextCourtAppearanceRepository.save(toCreateNextCourtAppearance)
        courtAppearanceRepository.updateNextCourtAppearance(
          savedNextCourtAppearance,
          getPerformedByUsername(legacyRequest),
          ZonedDateTime.now(),
          matchedCourtAppearance,
        )
        courtAppearanceHistoryRepository.save(
          CourtAppearanceHistoryEntity.from(
            courtAppearanceRepository.findByIdOrNull(matchedCourtAppearance.id)!!,
            ChangeSource.NOMIS,
          ),
        )
      }
    }
  }

  private fun getPerformedByUsername(courtAppearance: LegacyCreateCourtAppearance): String = courtAppearance.performedByUser ?: serviceUserService.getUsername()

  private fun getMatchedNextCourtAppearanceOrLatest(courtCase: CourtCaseEntity, appearanceDate: LocalDate): CourtAppearanceEntity? = courtAppearanceRepository.findByNextEventDateTime(courtCase.id, appearanceDate) ?: courtAppearanceRepository.findFirstByCourtCaseAndStatusIdOrderByAppearanceDateDesc(
    courtCase,
    CourtAppearanceEntityStatus.ACTIVE,
  )

  @Transactional
  fun get(lifetimeUuid: UUID): LegacyCourtAppearance {
    val courtAppearance = getUnlessDeleted(lifetimeUuid)
    val associatedNextCourtAppearance = nextCourtAppearanceRepository.findFirstByFutureSkeletonAppearance(courtAppearance)
    val appearanceTypeUuid = associatedNextCourtAppearance?.appearanceType?.appearanceTypeUuid ?: DEFAULT_APPEARANCE_TYPE_UUD
    return LegacyCourtAppearance.from(courtAppearance, appearanceTypeUuid)
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
    existingCourtAppearance.courtCase.latestCourtAppearance = CourtAppearanceEntity.getLatestCourtAppearance(existingCourtAppearance.courtCase.appearances)
    nextCourtAppearanceRepository.deleteByFutureSkeletonAppearance(existingCourtAppearance)
    eventsToEmit.add(
      EventMetadataCreator.courtAppearanceEventMetadata(
        existingCourtAppearance.courtCase.prisonerId,
        existingCourtAppearance.courtCase.caseUniqueIdentifier,
        existingCourtAppearance.appearanceUuid.toString(),
        EventType.COURT_APPEARANCE_DELETED,
      ),
    )
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
      val appearanceCharge = existingCourtAppearance.appearanceCharges.first { it.charge == existingCharge }
      existingCourtAppearance.appearanceCharges.remove(appearanceCharge)
      existingCharge.appearanceCharges.remove(appearanceCharge)
      eventsToEmit.add(
        EventMetadataCreator.courtAppearanceEventMetadata(
          existingCourtAppearance.courtCase.prisonerId,
          existingCourtAppearance.courtCase.caseUniqueIdentifier,
          existingCourtAppearance.appearanceUuid.toString(),
          EventType.COURT_APPEARANCE_UPDATED,
        ),
      )
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
          ),
        )
      }
      appearanceChargeHistoryRepository.save(
        AppearanceChargeHistoryEntity.removedFrom(
          appearanceCharge = appearanceCharge,
          removedBy = performedByUsername,
          removedPrison = null,
          ChangeSource.NOMIS,
        ),
      )
    }

    return eventsToEmit
  }

  private fun getUnlessDeleted(appearanceUuid: UUID): CourtAppearanceEntity = courtAppearanceRepository.findByAppearanceUuid(appearanceUuid)
    ?.takeUnless { entity -> entity.statusId == CourtAppearanceEntityStatus.DELETED } ?: throw EntityNotFoundException("No court appearance found at $appearanceUuid")

  private fun getChargeAtAppearanceUnlessDeleted(appearanceUuid: UUID, chargeUuid: UUID): ChargeEntity? = chargeRepository.findFirstByAppearanceChargesAppearanceAppearanceUuidAndChargeUuidAndStatusIdNotOrderByCreatedAtDesc(
    appearanceUuid,
    chargeUuid,
  )

  companion object {
    val DEFAULT_APPEARANCE_TYPE_UUD: UUID = UUID.fromString("63e8fce0-033c-46ad-9edf-391b802d547a") // Court appearance
  }
}
