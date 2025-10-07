package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.service

import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.AppearanceChargeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.ChargeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.CourtAppearanceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.NextCourtAppearanceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.audit.AppearanceChargeHistoryEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.audit.ChargeHistoryEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.audit.CourtAppearanceHistoryEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityChangeStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityStatus
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
    val courtCase = courtCaseRepository.findByCaseUniqueIdentifier(courtAppearance.courtCaseUuid)?.takeUnless { entity -> entity.statusId == EntityStatus.DELETED } ?: throw EntityNotFoundException("No court case found at ${courtAppearance.courtCaseUuid}")
    val dpsOutcome = courtAppearance.legacyData.nomisOutcomeCode?.let { nomisCode -> appearanceOutcomeRepository.findByNomisCode(nomisCode) }
    val createdCourtAppearance = courtAppearanceRepository.save(
      CourtAppearanceEntity.from(courtAppearance, dpsOutcome, courtCase, serviceUserService.getUsername()),
    )
    courtAppearanceHistoryRepository.save(CourtAppearanceHistoryEntity.from(createdCourtAppearance))
    courtCase.latestCourtAppearance = CourtAppearanceEntity.getLatestCourtAppearance(courtCase.appearances + createdCourtAppearance)
    if (createdCourtAppearance.statusId == EntityStatus.FUTURE) {
      (
        courtAppearanceRepository.findByNextEventDateTime(courtCase.id, courtAppearance.appearanceDate) ?: courtAppearanceRepository.findFirstByCourtCaseAndStatusIdOrderByAppearanceDateDesc(
          courtCase,
          EntityStatus.ACTIVE,
        )
        )?.let { nextEventAppearance ->
        val appearanceType = appearanceTypeRepository.findByAppearanceTypeUuid(courtAppearance.appearanceTypeUuid) ?: throw EntityNotFoundException("No appearance type at ${courtAppearance.appearanceTypeUuid}")
        nextEventAppearance.updateNextCourtAppearance(serviceUserService.getUsername(), nextCourtAppearanceRepository.save(NextCourtAppearanceEntity.from(courtAppearance, createdCourtAppearance, appearanceType)))
        courtAppearanceHistoryRepository.save(CourtAppearanceHistoryEntity.from(nextEventAppearance))
      }
    }
    return LegacyCourtAppearanceCreatedResponse(createdCourtAppearance.appearanceUuid, courtCase.caseUniqueIdentifier, courtCase.prisonerId)
  }

  @Transactional
  fun update(lifetimeUuid: UUID, courtAppearance: LegacyCreateCourtAppearance): Pair<EntityChangeStatus, LegacyCourtAppearanceCreatedResponse> {
    var entityChangeStatus = EntityChangeStatus.NO_CHANGE
    val existingCourtAppearance = getUnlessDeleted(lifetimeUuid)
    val dpsOutcome = courtAppearance.legacyData.nomisOutcomeCode?.let { nomisCode -> appearanceOutcomeRepository.findByNomisCode(nomisCode) }
    val updatedCourtAppearance = existingCourtAppearance.copyFrom(courtAppearance, dpsOutcome, serviceUserService.getUsername())
    if (!existingCourtAppearance.isSame(updatedCourtAppearance)) {
      existingCourtAppearance.updateFrom(updatedCourtAppearance)
      courtAppearanceHistoryRepository.save(CourtAppearanceHistoryEntity.from(existingCourtAppearance))

      if (existingCourtAppearance.statusId == EntityStatus.FUTURE) {
        (
          courtAppearanceRepository.findByNextEventDateTime(existingCourtAppearance.courtCase.id, courtAppearance.appearanceDate) ?: courtAppearanceRepository.findFirstByCourtCaseAndStatusIdOrderByAppearanceDateDesc(
            existingCourtAppearance.courtCase,
            EntityStatus.ACTIVE,
          )
          )?.let { nextEventAppearance ->
          val appearanceType = appearanceTypeRepository.findByAppearanceTypeUuid(courtAppearance.appearanceTypeUuid) ?: throw EntityNotFoundException("No appearance type at ${courtAppearance.appearanceTypeUuid}")
          val toSaveNextCourtAppearance = nextEventAppearance.nextCourtAppearance?.copyFrom(courtAppearance, existingCourtAppearance, appearanceType)
            ?: NextCourtAppearanceEntity.from(courtAppearance, existingCourtAppearance, appearanceType)
          nextEventAppearance.updateNextCourtAppearance(serviceUserService.getUsername(), nextCourtAppearanceRepository.save(toSaveNextCourtAppearance))
          courtAppearanceHistoryRepository.save(CourtAppearanceHistoryEntity.from(nextEventAppearance))
        }
      }
      existingCourtAppearance.courtCase.latestCourtAppearance = CourtAppearanceEntity.getLatestCourtAppearance(existingCourtAppearance.courtCase.appearances + existingCourtAppearance)
      entityChangeStatus = EntityChangeStatus.EDITED
    }
    return entityChangeStatus to LegacyCourtAppearanceCreatedResponse(lifetimeUuid, updatedCourtAppearance.courtCase.caseUniqueIdentifier, updatedCourtAppearance.courtCase.prisonerId)
  }

  @Transactional
  fun get(lifetimeUuid: UUID): LegacyCourtAppearance {
    val courtAppearance = getUnlessDeleted(lifetimeUuid)
    val associatedNextCourtAppearance = nextCourtAppearanceRepository.findFirstByFutureSkeletonAppearance(courtAppearance)
    val appearanceTypeUuid = associatedNextCourtAppearance?.appearanceType?.appearanceTypeUuid ?: DEFAULT_APPEARANCE_TYPE_UUD
    return LegacyCourtAppearance.from(courtAppearance, appearanceTypeUuid)
  }

  @Transactional
  fun delete(lifetimeUuid: UUID) {
    val existingCourtAppearance = getUnlessDeleted(lifetimeUuid)
    existingCourtAppearance.delete(serviceUserService.getUsername())
    courtAppearanceHistoryRepository.save(CourtAppearanceHistoryEntity.from(existingCourtAppearance))
    existingCourtAppearance.courtCase.latestCourtAppearance = CourtAppearanceEntity.getLatestCourtAppearance(existingCourtAppearance.courtCase.appearances)
    nextCourtAppearanceRepository.deleteByFutureSkeletonAppearance(existingCourtAppearance)
  }

  @Transactional
  fun linkAppearanceWithCharge(lifetimeUuid: UUID, lifetimeChargeUuid: UUID): EntityChangeStatus {
    val existingCourtAppearance = getUnlessDeleted(lifetimeUuid)
    val existingCharge = getChargeUnlessDelete(lifetimeChargeUuid)
    var entityChangeStatus = EntityChangeStatus.NO_CHANGE
    if (existingCourtAppearance.appearanceCharges.none { it.charge!!.chargeUuid == lifetimeChargeUuid }) {
      val appearanceCharge = AppearanceChargeEntity(
        existingCourtAppearance,
        existingCharge,
        serviceUserService.getUsername(),
        null,
      )
      existingCourtAppearance.appearanceCharges.add(appearanceCharge)
      existingCharge.appearanceCharges.add(appearanceCharge)
      appearanceChargeHistoryRepository.save(AppearanceChargeHistoryEntity.from(appearanceCharge))
      entityChangeStatus = EntityChangeStatus.EDITED
    }
    return entityChangeStatus
  }

  @Transactional
  fun unlinkAppearanceWithCharge(lifetimeUuid: UUID, lifetimeChargeUuid: UUID): Pair<EntityChangeStatus, EntityChangeStatus> {
    val existingCourtAppearance = getUnlessDeleted(lifetimeUuid)
    val existingCharge = getChargeAtAppearanceUnlessDeleted(lifetimeUuid, lifetimeChargeUuid)
    var appearanceEntityChangeStatus = EntityChangeStatus.NO_CHANGE
    var chargeEntityStatus = EntityChangeStatus.NO_CHANGE
    if (existingCharge != null) {
      val appearanceCharge = existingCourtAppearance.appearanceCharges.first { it.charge == existingCharge }
      existingCourtAppearance.appearanceCharges.remove(appearanceCharge)
      existingCharge.appearanceCharges.remove(appearanceCharge)
      appearanceEntityChangeStatus = EntityChangeStatus.EDITED
      if (existingCharge.hasNoActiveCourtAppearances()) {
        existingCharge.delete(serviceUserService.getUsername())
        chargeHistoryRepository.save(ChargeHistoryEntity.from(existingCharge))
        chargeEntityStatus = EntityChangeStatus.DELETED
      }
      appearanceChargeHistoryRepository.save(
        AppearanceChargeHistoryEntity.removedFrom(
          appearanceCharge = appearanceCharge,
          removedBy = serviceUserService.getUsername(),
          removedPrison = null,
        ),
      )
    }

    return appearanceEntityChangeStatus to chargeEntityStatus
  }

  private fun getUnlessDeleted(appearanceUuid: UUID): CourtAppearanceEntity = courtAppearanceRepository.findByAppearanceUuid(appearanceUuid)
    ?.takeUnless { entity -> entity.statusId == EntityStatus.DELETED } ?: throw EntityNotFoundException("No court appearance found at $appearanceUuid")

  private fun getChargeUnlessDelete(lifetimeChargeUuid: UUID): ChargeEntity = chargeRepository.findFirstByChargeUuidAndStatusIdNotOrderByUpdatedAtDesc(lifetimeChargeUuid) ?: throw EntityNotFoundException("No charge found at $lifetimeChargeUuid")

  private fun getChargeAtAppearanceUnlessDeleted(appearanceUuid: UUID, chargeUuid: UUID): ChargeEntity? = chargeRepository.findFirstByAppearanceChargesAppearanceAppearanceUuidAndChargeUuidAndStatusIdNotOrderByCreatedAtDesc(
    appearanceUuid,
    chargeUuid,
  )

  companion object {
    val DEFAULT_APPEARANCE_TYPE_UUD = UUID.fromString("63e8fce0-033c-46ad-9edf-391b802d547a") // Court appearance
  }
}
