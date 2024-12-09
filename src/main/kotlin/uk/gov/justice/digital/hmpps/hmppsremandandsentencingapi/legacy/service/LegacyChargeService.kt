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
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.ServiceUserService
import java.util.UUID

@Service
class LegacyChargeService(private val chargeRepository: ChargeRepository, private val courtAppearanceRepository: CourtAppearanceRepository, private val chargeOutcomeRepository: ChargeOutcomeRepository, private val serviceUserService: ServiceUserService, private val objectMapper: ObjectMapper) {

  @Transactional
  fun create(charge: LegacyCreateCharge): LegacyChargeCreatedResponse {
    val courtAppearance = courtAppearanceRepository.findFirstByLifetimeUuidOrderByCreatedAtDesc(charge.appearanceLifetimeUuid)?.takeUnless { entity -> entity.statusId == EntityStatus.DELETED } ?: throw EntityNotFoundException("No court appearance found at ${charge.appearanceLifetimeUuid}")
    val dpsOutcome = charge.legacyData.nomisOutcomeCode?.let { nomisCode -> chargeOutcomeRepository.findByNomisCode(nomisCode) }
    val legacyData = objectMapper.valueToTree<JsonNode>(charge.legacyData)
    val createdCharge = chargeRepository.save(ChargeEntity.from(charge, dpsOutcome, legacyData, serviceUserService.getUsername()))
    courtAppearance.charges.add(createdCharge)
    createdCharge.courtAppearances.add(courtAppearance)
    return LegacyChargeCreatedResponse(createdCharge.lifetimeChargeUuid, courtAppearance.courtCase.caseUniqueIdentifier, courtAppearance.courtCase.prisonerId)
  }

  @Transactional
  fun update(lifetimeUUID: UUID, charge: LegacyCreateCharge): Pair<EntityChangeStatus, LegacyChargeCreatedResponse> {
    var entityChangeStatus = EntityChangeStatus.NO_CHANGE
    val existingCharge = getUnlessDeleted(lifetimeUUID)
    val dpsOutcome = charge.legacyData.nomisOutcomeCode?.let { nomisCode -> chargeOutcomeRepository.findByNomisCode(nomisCode) }
    val legacyData = objectMapper.valueToTree<JsonNode>(charge.legacyData)
    val updatedCharge = existingCharge.copyFrom(charge, dpsOutcome, serviceUserService.getUsername(), legacyData)
    if (!existingCharge.isSame(updatedCharge)) {
      existingCharge.statusId = EntityStatus.EDITED
      chargeRepository.save(updatedCharge)
      entityChangeStatus = EntityChangeStatus.EDITED
    }
    return entityChangeStatus to LegacyChargeCreatedResponse(lifetimeUUID, existingCharge.courtAppearances.first().courtCase.caseUniqueIdentifier, existingCharge.courtAppearances.first().courtCase.prisonerId)
  }

  @Transactional(readOnly = true)
  fun get(lifetimeUUID: UUID): LegacyCharge {
    return LegacyCharge.from(getUnlessDeleted(lifetimeUUID), objectMapper)
  }

  @Transactional(readOnly = true)
  fun getChargeAtAppearance(appearanceLifetimeUuid: UUID, lifetimeUUID: UUID): LegacyCharge {
    return chargeRepository.findFirstByCourtAppearancesLifetimeUuidAndLifetimeChargeUuidOrderByCreatedAtDesc(appearanceLifetimeUuid, lifetimeUUID)
      ?.takeUnless { entity -> entity.statusId == EntityStatus.DELETED }?.let { chargeEntity -> LegacyCharge.from(chargeEntity, objectMapper) } ?: throw EntityNotFoundException("No charge found at $lifetimeUUID for appearance $appearanceLifetimeUuid")
  }

  @Transactional
  fun delete(lifetimeUUID: UUID) {
    val charge = getUnlessDeleted(lifetimeUUID)
    charge.statusId = EntityStatus.DELETED
  }

  private fun getUnlessDeleted(lifetimeUUID: UUID): ChargeEntity {
    return chargeRepository.findFirstByLifetimeChargeUuidOrderByCreatedAtDesc(lifetimeUUID)
      ?.takeUnless { entity -> entity.statusId == EntityStatus.DELETED } ?: throw EntityNotFoundException("No charge found at $lifetimeUUID")
  }
}
