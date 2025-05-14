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
  private val legacySentenceService: LegacySentenceService,
) {

  @Transactional
  fun create(charge: LegacyCreateCharge): LegacyChargeCreatedResponse {
    val courtAppearance = courtAppearanceRepository.findByAppearanceUuid(charge.appearanceLifetimeUuid)?.takeUnless { entity -> entity.statusId == EntityStatus.DELETED } ?: throw EntityNotFoundException("No court appearance found at ${charge.appearanceLifetimeUuid}")
    val dpsOutcome = charge.legacyData.nomisOutcomeCode?.let { nomisCode -> chargeOutcomeRepository.findByNomisCode(nomisCode) }
    charge.legacyData = dpsOutcome?.let { charge.legacyData.copy(nomisOutcomeCode = null, outcomeDescription = null, outcomeDispositionCode = null) } ?: charge.legacyData
    val createdCharge = chargeRepository.save(ChargeEntity.from(charge, dpsOutcome, serviceUserService.getUsername()))
    chargeHistoryRepository.save(ChargeHistoryEntity.from(createdCharge))
    val appearanceCharge = AppearanceChargeEntity(
      courtAppearance,
      createdCharge,
      serviceUserService.getUsername(),
      null,
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
    val existingCharge = chargeRepository.findFirstByAppearanceChargesAppearanceAppearanceUuidAndChargeUuidOrderByCreatedAtDesc(
      appearanceUuid,
      lifetimeUuid,
    )?.takeUnless { entity -> entity.statusId == EntityStatus.DELETED }
      ?: throw EntityNotFoundException("No charge found at $lifetimeUuid")
    val appearance = existingCharge.appearanceCharges.first { it.appearance!!.appearanceUuid == appearanceUuid }.appearance!!
    val dpsOutcome = charge.legacyData.nomisOutcomeCode?.let { nomisCode -> chargeOutcomeRepository.findByNomisCode(nomisCode) }
    charge.legacyData = dpsOutcome?.let { charge.legacyData.copy(nomisOutcomeCode = null, outcomeDescription = null, outcomeDispositionCode = null) } ?: charge.legacyData
    val updatedCharge = existingCharge.copyFrom(charge, dpsOutcome, serviceUserService.getUsername())
    if (!existingCharge.isSame(updatedCharge)) {
      var chargeRecord = existingCharge
      if (existingCharge.hasTwoOrMoreActiveCourtAppearance(appearance)) {
        existingCharge.appearanceCharges.firstOrNull { it.appearance == appearance }
          ?.let { appearanceCharge ->
            appearanceChargeHistoryRepository.save(
              AppearanceChargeHistoryEntity.removedFrom(
                appearanceCharge = appearanceCharge,
                removedBy = serviceUserService.getUsername(),
                removedPrison = null,
              ),
            )
            existingCharge.appearanceCharges.remove(appearanceCharge)
            chargeRecord.appearanceCharges.remove(appearanceCharge)
            appearance.appearanceCharges.remove(appearanceCharge)
            updatedCharge.appearanceCharges.remove(appearanceCharge)
            appearanceCharge.charge = null
            appearanceCharge.appearance = null
          }

        chargeRecord = chargeRepository.save(updatedCharge)
        val appearanceCharge = AppearanceChargeEntity(
          appearance,
          chargeRecord,
          serviceUserService.getUsername(),
          null,
        )
        appearance.appearanceCharges.add(appearanceCharge)
        chargeRecord.appearanceCharges.add(appearanceCharge)
        appearanceChargeHistoryRepository.save(AppearanceChargeHistoryEntity.from(appearanceCharge))
      } else {
        existingCharge.updateFrom(updatedCharge)
      }
      chargeHistoryRepository.save(ChargeHistoryEntity.from(chargeRecord))

      entityChangeStatus = EntityChangeStatus.EDITED
    }
    val courtCase = existingCharge.appearanceCharges.first().appearance!!.courtCase
    return entityChangeStatus to LegacyChargeCreatedResponse(lifetimeUuid, courtCase.caseUniqueIdentifier, courtCase.prisonerId)
  }

  @Transactional(readOnly = true)
  fun get(lifetimeUUID: UUID): LegacyCharge = LegacyCharge.from(getUnlessDeleted(lifetimeUUID))

  @Transactional(readOnly = true)
  fun getChargeAtAppearance(appearanceLifetimeUuid: UUID, lifetimeUUID: UUID): LegacyCharge = chargeRepository.findFirstByAppearanceChargesAppearanceAppearanceUuidAndChargeUuidOrderByCreatedAtDesc(
    appearanceLifetimeUuid,
    lifetimeUUID,
  )
    ?.takeUnless { entity -> entity.statusId == EntityStatus.DELETED }
    ?.let { chargeEntity -> LegacyCharge.from(chargeEntity) }
    ?: throw EntityNotFoundException("No charge found at $lifetimeUUID for appearance $appearanceLifetimeUuid")

  @Transactional
  fun delete(chargeUUID: UUID): LegacyCharge? = chargeRepository.findByChargeUuid(chargeUUID)
    .filter { it.statusId != EntityStatus.DELETED }
    .map { charge ->
      charge.delete(serviceUserService.getUsername())
      chargeHistoryRepository.save(ChargeHistoryEntity.from(charge))
      val deletedManyChargesSentence = charge.sentences.filter { it.statusId == EntityStatus.MANY_CHARGES_DATA_FIX }.map {
        legacySentenceService.delete(it)
        it
      }.firstOrNull()
      deletedManyChargesSentence to LegacyCharge.from(charge)
    }.firstOrNull()?.let { (deletedManyChargesSentence, legacyCharge) ->
      if (deletedManyChargesSentence != null) {
        legacySentenceService.handleManyChargesSentenceDeleted(deletedManyChargesSentence.sentenceUuid)
      }
      legacyCharge
    }

  private fun getUnlessDeleted(lifetimeUUID: UUID): ChargeEntity = chargeRepository.findFirstByChargeUuidAndStatusIdNotOrderByUpdatedAtDesc(lifetimeUUID) ?: throw EntityNotFoundException("No charge found at $lifetimeUUID")
}
