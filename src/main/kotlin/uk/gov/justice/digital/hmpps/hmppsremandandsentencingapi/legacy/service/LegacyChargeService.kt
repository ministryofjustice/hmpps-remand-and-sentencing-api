package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.service

import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.AppearanceChargeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.ChargeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.audit.AppearanceChargeHistoryEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.audit.ChargeHistoryEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityChangeStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.ChargeOutcomeRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.ChargeRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.CourtAppearanceRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.audit.AppearanceChargeHistoryRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.audit.ChargeHistoryRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacyCharge
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacyChargeCreatedResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacyCreateCharge
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacyUpdateCharge
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacyUpdateWholeCharge
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.ServiceUserService
import java.util.UUID

@Service
class LegacyChargeService(
  private val chargeRepository: ChargeRepository,
  private val courtAppearanceRepository: CourtAppearanceRepository,
  private val chargeOutcomeRepository: ChargeOutcomeRepository,
  private val serviceUserService: ServiceUserService,
  private val chargeHistoryRepository: ChargeHistoryRepository,
  private val appearanceChargeHistoryRepository: AppearanceChargeHistoryRepository,
) {

  @Transactional
  fun create(charge: LegacyCreateCharge): LegacyChargeCreatedResponse {
    val courtAppearance = courtAppearanceRepository.findByAppearanceUuid(charge.appearanceLifetimeUuid)?.takeUnless { entity -> entity.statusId == EntityStatus.DELETED } ?: throw EntityNotFoundException("No court appearance found at ${charge.appearanceLifetimeUuid}")
    val dpsOutcome = charge.legacyData.nomisOutcomeCode?.let { nomisCode -> chargeOutcomeRepository.findByNomisCode(nomisCode) }
    charge.legacyData = dpsOutcome?.let { charge.legacyData.copy(nomisOutcomeCode = null, outcomeDescription = null, outcomeDispositionCode = null) } ?: charge.legacyData
    val createdCharge = chargeRepository.save(ChargeEntity.from(charge, dpsOutcome, serviceUserService.getUsername()))
    chargeHistoryRepository.save(ChargeHistoryEntity.from(createdCharge))
    val appearanceCharge = AppearanceChargeEntity(
      courtAppearance = courtAppearance,
      charge = createdCharge,
      createdBy = serviceUserService.getUsername(),
      createdPrison = null,
    )
    courtAppearance.appearanceCharges.add(appearanceCharge)
    createdCharge.appearanceCharges.add(appearanceCharge)
    appearanceChargeHistoryRepository.save(AppearanceChargeHistoryEntity.from(appearanceCharge))
    return LegacyChargeCreatedResponse(createdCharge.chargeUuid, courtAppearance.courtCase.caseUniqueIdentifier, courtAppearance.courtCase.prisonerId)
  }

  @Transactional
  fun updateInAllAppearances(chargeUuid: UUID, charge: LegacyUpdateWholeCharge) {
    val existingChargeRecords = chargeRepository.findByChargeUuidAndStatusId(chargeUuid, EntityStatus.ACTIVE)
    if (existingChargeRecords.isEmpty()) {
      throw EntityNotFoundException("No charge found at $chargeUuid")
    }
    existingChargeRecords.forEach { existingCharge ->
      val updatedCharge = existingCharge.copyFrom(charge, serviceUserService.getUsername())
      if (!existingCharge.isSame(updatedCharge)) {
        existingCharge.updateFrom(updatedCharge)
        chargeHistoryRepository.save(ChargeHistoryEntity.from(existingCharge))
      }
    }
  }

  @Transactional
  fun updateInAppearance(lifetimeUuid: UUID, appearanceUuid: UUID, charge: LegacyUpdateCharge): Pair<EntityChangeStatus, LegacyChargeCreatedResponse> {
    var entityChangeStatus = EntityChangeStatus.NO_CHANGE
    val existingCharge = chargeRepository.findFirstByAppearanceChargesCourtAppearanceAppearanceUuidAndChargeUuidOrderByCreatedAtDesc(
      appearanceUuid,
      lifetimeUuid,
    )?.takeUnless { entity -> entity.statusId == EntityStatus.DELETED }
      ?: throw EntityNotFoundException("No charge found at $lifetimeUuid")
    val appearance = existingCharge.appearanceCharges.first { it.courtAppearance.appearanceUuid == appearanceUuid }.courtAppearance
    val dpsOutcome = charge.legacyData.nomisOutcomeCode?.let { nomisCode -> chargeOutcomeRepository.findByNomisCode(nomisCode) }
    charge.legacyData = dpsOutcome?.let { charge.legacyData.copy(nomisOutcomeCode = null, outcomeDescription = null, outcomeDispositionCode = null) } ?: charge.legacyData
    val updatedCharge = existingCharge.copyFrom(charge, dpsOutcome, serviceUserService.getUsername())
    if (!existingCharge.isSame(updatedCharge)) {
      var chargeRecord = existingCharge
      if (existingCharge.hasTwoOrMoreActiveCourtAppearance(appearance)) {
        val removedCharges = (existingCharge.appearanceCharges.filter { it.courtAppearance == appearance } +
            appearance.appearanceCharges.filter { it.charge == existingCharge }).toSet()

        if (removedCharges.isNotEmpty()) {
          existingCharge.appearanceCharges.removeIf { it.courtAppearance == appearance }
          appearance.appearanceCharges.removeIf { it.charge == existingCharge }

          removedCharges.forEach { removedCharge ->
            appearanceChargeHistoryRepository.save(
              AppearanceChargeHistoryEntity.removedFrom(
                appearanceCharge = removedCharge,
                removedBy = serviceUserService.getUsername(),
                removedPrison = null,
              )
            )
          }
        }

        chargeRecord = chargeRepository.save(updatedCharge)
        val appearanceCharge = AppearanceChargeEntity(
          courtAppearance = appearance,
          charge = chargeRecord,
          createdBy = serviceUserService.getUsername(),
          createdPrison = null,
        )
        appearance.appearanceCharges.add(appearanceCharge)
        appearanceChargeHistoryRepository.save(AppearanceChargeHistoryEntity.from(appearanceCharge))
      } else {
        existingCharge.updateFrom(updatedCharge)
      }
      chargeHistoryRepository.save(ChargeHistoryEntity.from(chargeRecord))

      entityChangeStatus = EntityChangeStatus.EDITED
    }
    val courtCase = existingCharge.appearanceCharges.first().courtAppearance.courtCase
    return entityChangeStatus to LegacyChargeCreatedResponse(lifetimeUuid, courtCase.caseUniqueIdentifier, courtCase.prisonerId)
  }

  @Transactional(readOnly = true)
  fun get(lifetimeUUID: UUID): LegacyCharge = LegacyCharge.from(getUnlessDeleted(lifetimeUUID))

  @Transactional(readOnly = true)
  fun getChargeAtAppearance(appearanceLifetimeUuid: UUID, lifetimeUUID: UUID): LegacyCharge = chargeRepository.findFirstByAppearanceChargesCourtAppearanceAppearanceUuidAndChargeUuidOrderByCreatedAtDesc(
    appearanceLifetimeUuid,
    lifetimeUUID,
  )
    ?.takeUnless { entity -> entity.statusId == EntityStatus.DELETED }
    ?.let { chargeEntity -> LegacyCharge.from(chargeEntity) }
    ?: throw EntityNotFoundException("No charge found at $lifetimeUUID for appearance $appearanceLifetimeUuid")

  @Transactional
  fun delete(lifetimeUUID: UUID) {
    val charge = getUnlessDeleted(lifetimeUUID)
    charge.delete(serviceUserService.getUsername())
    chargeHistoryRepository.save(ChargeHistoryEntity.from(charge))
  }

  private fun getUnlessDeleted(lifetimeUUID: UUID): ChargeEntity = chargeRepository.findFirstByChargeUuidOrderByCreatedAtDesc(lifetimeUUID)
    ?.takeUnless { entity -> entity.statusId == EntityStatus.DELETED } ?: throw EntityNotFoundException("No charge found at $lifetimeUUID")
}
