package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.service

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.ChargeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityChangeStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.ChargeOutcomeRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.ChargeRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.CourtAppearanceRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacyCharge
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacyChargeCreatedResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacyCreateCharge
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacyUpdateCharge
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacyUpdateWholeCharge
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.ServiceUserService
import java.util.UUID

@Service
class LegacyChargeService(private val chargeRepository: ChargeRepository, private val courtAppearanceRepository: CourtAppearanceRepository, private val chargeOutcomeRepository: ChargeOutcomeRepository, private val serviceUserService: ServiceUserService, private val objectMapper: ObjectMapper) {

  @Transactional
  fun create(charge: LegacyCreateCharge): LegacyChargeCreatedResponse {
    val courtAppearance = courtAppearanceRepository.findFirstByLifetimeUuidOrderByCreatedAtDesc(charge.appearanceLifetimeUuid)?.takeUnless { entity -> entity.statusId == EntityStatus.DELETED } ?: throw EntityNotFoundException("No court appearance found at ${charge.appearanceLifetimeUuid}")
    val dpsOutcome = charge.legacyData.nomisOutcomeCode?.let { nomisCode -> chargeOutcomeRepository.findByNomisCode(nomisCode) }
    val chargeLegacyData = dpsOutcome?.let { charge.legacyData.copy(nomisOutcomeCode = null, outcomeDescription = null, outcomeDispositionCode = null) } ?: charge.legacyData
    val legacyData = objectMapper.valueToTree<JsonNode>(chargeLegacyData)
    val createdCharge = chargeRepository.save(ChargeEntity.from(charge, dpsOutcome, legacyData, serviceUserService.getUsername()))
    courtAppearance.charges.add(createdCharge)
    createdCharge.courtAppearances.add(courtAppearance)
    return LegacyChargeCreatedResponse(createdCharge.lifetimeChargeUuid, courtAppearance.courtCase.caseUniqueIdentifier, courtAppearance.courtCase.prisonerId)
  }

  @Transactional
  fun updateInAllAppearances(lifetimeUUID: UUID, charge: LegacyUpdateWholeCharge) {
    val existingChargeRecords = chargeRepository.findByLifetimeChargeUuidAndStatusId(lifetimeUUID, EntityStatus.ACTIVE)
    if (existingChargeRecords.isEmpty()) {
      throw EntityNotFoundException("No charge found at $lifetimeUUID")
    }
    existingChargeRecords.forEach { existingCharge ->
      val updatedCharge = existingCharge.copyFrom(charge, serviceUserService.getUsername())
      if (!existingCharge.isSame(updatedCharge)) {
        existingCharge.statusId = EntityStatus.EDITED
        val savedCharge = chargeRepository.save(updatedCharge)
        existingCharge.courtAppearances.filter { it.statusId == EntityStatus.ACTIVE }.forEach { courtAppearance ->
          courtAppearance.charges.add(savedCharge)
        }
      }
    }
  }

  @Transactional
  fun updateInAppearance(lifetimeUuid: UUID, appearanceLifetimeUuid: UUID, charge: LegacyUpdateCharge): Pair<EntityChangeStatus, LegacyChargeCreatedResponse> {
    var entityChangeStatus = EntityChangeStatus.NO_CHANGE
    val existingCharge = chargeRepository.findFirstByCourtAppearancesLifetimeUuidAndLifetimeChargeUuidOrderByCreatedAtDesc(appearanceLifetimeUuid, lifetimeUuid)?.takeUnless { entity -> entity.statusId == EntityStatus.DELETED } ?: throw EntityNotFoundException("No charge found at $lifetimeUuid")
    val appearance = existingCharge.courtAppearances.first { it.lifetimeUuid == appearanceLifetimeUuid }
    val dpsOutcome = charge.legacyData.nomisOutcomeCode?.let { nomisCode -> chargeOutcomeRepository.findByNomisCode(nomisCode) }
    val chargeLegacyData = dpsOutcome?.let { charge.legacyData.copy(nomisOutcomeCode = null, outcomeDescription = null, outcomeDispositionCode = null) } ?: charge.legacyData
    val legacyData = objectMapper.valueToTree<JsonNode>(chargeLegacyData)
    val updatedCharge = existingCharge.copyFrom(charge, dpsOutcome, serviceUserService.getUsername(), legacyData)
    if (!existingCharge.isSame(updatedCharge)) {
      if (existingCharge.hasTwoOrMoreActiveCourtAppearance(appearance)) {
        existingCharge.courtAppearances.remove(appearance)
        appearance.charges.remove(existingCharge)
      } else {
        existingCharge.statusId = EntityStatus.EDITED
      }
      val savedCharge = chargeRepository.save(updatedCharge)
      appearance.charges.add(savedCharge)
      entityChangeStatus = EntityChangeStatus.EDITED
    }
    return entityChangeStatus to LegacyChargeCreatedResponse(lifetimeUuid, existingCharge.courtAppearances.first().courtCase.caseUniqueIdentifier, existingCharge.courtAppearances.first().courtCase.prisonerId)
  }

  @Transactional(readOnly = true)
  fun get(lifetimeUUID: UUID): LegacyCharge = LegacyCharge.from(getUnlessDeleted(lifetimeUUID), objectMapper)

  @Transactional(readOnly = true)
  fun getChargeAtAppearance(appearanceLifetimeUuid: UUID, lifetimeUUID: UUID): LegacyCharge = chargeRepository.findFirstByCourtAppearancesLifetimeUuidAndLifetimeChargeUuidOrderByCreatedAtDesc(appearanceLifetimeUuid, lifetimeUUID)
    ?.takeUnless { entity -> entity.statusId == EntityStatus.DELETED }?.let { chargeEntity -> LegacyCharge.from(chargeEntity, objectMapper) } ?: throw EntityNotFoundException("No charge found at $lifetimeUUID for appearance $appearanceLifetimeUuid")

  @Transactional
  fun delete(lifetimeUUID: UUID) {
    val charge = getUnlessDeleted(lifetimeUUID)
    charge.statusId = EntityStatus.DELETED
  }

  private fun getUnlessDeleted(lifetimeUUID: UUID): ChargeEntity = chargeRepository.findFirstByLifetimeChargeUuidOrderByCreatedAtDesc(lifetimeUUID)
    ?.takeUnless { entity -> entity.statusId == EntityStatus.DELETED } ?: throw EntityNotFoundException("No charge found at $lifetimeUUID")
}
