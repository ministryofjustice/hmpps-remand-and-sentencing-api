package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.service

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.CourtAppearanceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityChangeStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.AppearanceOutcomeRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.CourtAppearanceRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.CourtCaseRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacyCourtAppearance
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacyCourtAppearanceCreatedResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacyCreateCourtAppearance
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.ServiceUserService
import java.util.UUID
import kotlin.collections.plus

@Service
class LegacyCourtAppearanceService(private val courtAppearanceRepository: CourtAppearanceRepository, private val courtCaseRepository: CourtCaseRepository, private val appearanceOutcomeRepository: AppearanceOutcomeRepository, private val serviceUserService: ServiceUserService, private val objectMapper: ObjectMapper) {

  @Transactional
  fun create(courtAppearance: LegacyCreateCourtAppearance): LegacyCourtAppearanceCreatedResponse {
    val courtCase = courtCaseRepository.findByCaseUniqueIdentifier(courtAppearance.courtCaseUuid)?.takeUnless { entity -> entity.statusId == EntityStatus.DELETED } ?: throw EntityNotFoundException("No court case found at ${courtAppearance.courtCaseUuid}")
    val legacyData = objectMapper.valueToTree<JsonNode>(courtAppearance.legacyData)
    val dpsOutcome = courtAppearance.legacyData.nomisOutcomeCode?.let { nomisCode -> appearanceOutcomeRepository.findByNomisCode(nomisCode) }
    val createdCourtAppearance = courtAppearanceRepository.save(
      CourtAppearanceEntity.from(courtAppearance, dpsOutcome, courtCase, serviceUserService.getUsername(), legacyData),
    )
    courtCase.latestCourtAppearance = CourtAppearanceEntity.getLatestCourtAppearance(courtCase.appearances + createdCourtAppearance)
    return LegacyCourtAppearanceCreatedResponse(createdCourtAppearance.lifetimeUuid, courtCase.caseUniqueIdentifier, courtCase.prisonerId)
  }

  @Transactional
  fun update(lifetimeUuid: UUID, courtAppearance: LegacyCreateCourtAppearance): Pair<EntityChangeStatus, LegacyCourtAppearanceCreatedResponse> {
    var entityChangeStatus = EntityChangeStatus.NO_CHANGE
    val existingCourtAppearance = getUnlessDeleted(lifetimeUuid)
    val legacyData = objectMapper.valueToTree<JsonNode>(courtAppearance.legacyData)
    val dpsOutcome = courtAppearance.legacyData.nomisOutcomeCode?.let { nomisCode -> appearanceOutcomeRepository.findByNomisCode(nomisCode) }
    val updatedCourtAppearance = existingCourtAppearance.copyFrom(courtAppearance, dpsOutcome, serviceUserService.getUsername(), legacyData)
    if (!existingCourtAppearance.isSame(updatedCourtAppearance)) {
      existingCourtAppearance.statusId = EntityStatus.EDITED
      courtAppearanceRepository.save(updatedCourtAppearance)
      entityChangeStatus = EntityChangeStatus.EDITED
    }
    return entityChangeStatus to LegacyCourtAppearanceCreatedResponse(lifetimeUuid, updatedCourtAppearance.courtCase.caseUniqueIdentifier, updatedCourtAppearance.courtCase.prisonerId)
  }

  @Transactional
  fun get(lifetimeUuid: UUID): LegacyCourtAppearance {
    return LegacyCourtAppearance.from(getUnlessDeleted(lifetimeUuid), objectMapper)
  }

  @Transactional
  fun delete(lifetimeUuid: UUID) {
    val existingCourtAppearance = getUnlessDeleted(lifetimeUuid)
    existingCourtAppearance.statusId = EntityStatus.DELETED
  }

  private fun getUnlessDeleted(lifetimeUuid: UUID): CourtAppearanceEntity {
    return courtAppearanceRepository.findFirstByLifetimeUuidOrderByCreatedAtDesc(lifetimeUuid)
      ?.takeUnless { entity -> entity.statusId == EntityStatus.DELETED } ?: throw EntityNotFoundException("No court appearance found at $lifetimeUuid")
  }
}
