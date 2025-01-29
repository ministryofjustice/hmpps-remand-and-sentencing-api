package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.service

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.ChargeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.CourtAppearanceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.NextCourtAppearanceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityChangeStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.AppearanceOutcomeRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.AppearanceTypeRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.ChargeRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.CourtAppearanceRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.CourtCaseRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.NextCourtAppearanceRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacyCourtAppearance
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacyCourtAppearanceCreatedResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacyCreateCourtAppearance
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.ServiceUserService
import java.util.UUID
import kotlin.collections.plus

@Service
class LegacyCourtAppearanceService(private val courtAppearanceRepository: CourtAppearanceRepository, private val courtCaseRepository: CourtCaseRepository, private val appearanceOutcomeRepository: AppearanceOutcomeRepository, private val serviceUserService: ServiceUserService, private val objectMapper: ObjectMapper, private val chargeRepository: ChargeRepository, private val appearanceTypeRepository: AppearanceTypeRepository, private val nextCourtAppearanceRepository: NextCourtAppearanceRepository) {

  @Transactional
  fun create(courtAppearance: LegacyCreateCourtAppearance): LegacyCourtAppearanceCreatedResponse {
    val courtCase = courtCaseRepository.findByCaseUniqueIdentifier(courtAppearance.courtCaseUuid)?.takeUnless { entity -> entity.statusId == EntityStatus.DELETED } ?: throw EntityNotFoundException("No court case found at ${courtAppearance.courtCaseUuid}")
    val legacyData = objectMapper.valueToTree<JsonNode>(courtAppearance.legacyData)
    val dpsOutcome = courtAppearance.legacyData.nomisOutcomeCode?.let { nomisCode -> appearanceOutcomeRepository.findByNomisCode(nomisCode) }
    val createdCourtAppearance = courtAppearanceRepository.save(
      CourtAppearanceEntity.from(courtAppearance, dpsOutcome, courtCase, serviceUserService.getUsername(), legacyData),
    )
    courtCase.latestCourtAppearance = CourtAppearanceEntity.getLatestCourtAppearance(courtCase.appearances + createdCourtAppearance)
    if (createdCourtAppearance.statusId == EntityStatus.FUTURE) {
      (
        courtAppearanceRepository.findByNextEventDateTime(courtCase.id, courtAppearance.appearanceDate) ?: courtAppearanceRepository.findFirstByCourtCaseAndStatusIdOrderByAppearanceDateDesc(
          courtCase,
          EntityStatus.ACTIVE,
        )
        )?.let { nextEventAppearance ->
        val appearanceType = appearanceTypeRepository.findByAppearanceTypeUuid(courtAppearance.appearanceTypeUuid) ?: throw EntityNotFoundException("No appearance type at ${courtAppearance.appearanceTypeUuid}")
        nextEventAppearance.nextCourtAppearance = nextCourtAppearanceRepository.save(NextCourtAppearanceEntity.from(courtAppearance, createdCourtAppearance, appearanceType))
      }
    }
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
      val savedRecord = courtAppearanceRepository.save(updatedCourtAppearance)
      if (savedRecord.statusId == EntityStatus.FUTURE) {
        (
          courtAppearanceRepository.findByNextEventDateTime(savedRecord.courtCase.id, courtAppearance.appearanceDate) ?: courtAppearanceRepository.findFirstByCourtCaseAndStatusIdOrderByAppearanceDateDesc(
            savedRecord.courtCase,
            EntityStatus.ACTIVE,
          )
          )?.let { nextEventAppearance ->
          val appearanceType = appearanceTypeRepository.findByAppearanceTypeUuid(courtAppearance.appearanceTypeUuid) ?: throw EntityNotFoundException("No appearance type at ${courtAppearance.appearanceTypeUuid}")
          val toSaveNextCourtAppearance = nextEventAppearance.nextCourtAppearance?.copyFrom(courtAppearance, savedRecord, appearanceType)
            ?: NextCourtAppearanceEntity.from(courtAppearance, savedRecord, appearanceType)
          nextEventAppearance.nextCourtAppearance = nextCourtAppearanceRepository.save(toSaveNextCourtAppearance)
        }
      }
      existingCourtAppearance.courtCase.latestCourtAppearance = CourtAppearanceEntity.getLatestCourtAppearance(existingCourtAppearance.courtCase.appearances + savedRecord)
      entityChangeStatus = EntityChangeStatus.EDITED
    }
    return entityChangeStatus to LegacyCourtAppearanceCreatedResponse(lifetimeUuid, updatedCourtAppearance.courtCase.caseUniqueIdentifier, updatedCourtAppearance.courtCase.prisonerId)
  }

  @Transactional
  fun get(lifetimeUuid: UUID): LegacyCourtAppearance = LegacyCourtAppearance.from(getUnlessDeleted(lifetimeUuid), objectMapper)

  @Transactional
  fun delete(lifetimeUuid: UUID) {
    val existingCourtAppearance = getUnlessDeleted(lifetimeUuid)
    existingCourtAppearance.statusId = EntityStatus.DELETED
    existingCourtAppearance.courtCase.latestCourtAppearance = CourtAppearanceEntity.getLatestCourtAppearance(existingCourtAppearance.courtCase.appearances)
  }

  @Transactional
  fun linkAppearanceWithCharge(lifetimeUuid: UUID, lifetimeChargeUuid: UUID): EntityChangeStatus {
    val existingCourtAppearance = getUnlessDeleted(lifetimeUuid)
    val existingCharge = getChargeUnlessDelete(lifetimeChargeUuid)
    var entityChangeStatus = EntityChangeStatus.NO_CHANGE
    if (!existingCourtAppearance.charges.contains(existingCharge)) {
      existingCourtAppearance.charges.add(existingCharge)
      existingCharge.courtAppearances.add(existingCourtAppearance)
      entityChangeStatus = EntityChangeStatus.EDITED
    }
    return entityChangeStatus
  }

  @Transactional
  fun unlinkAppearanceWithCharge(lifetimeUuid: UUID, lifetimeChargeUuid: UUID): Pair<EntityChangeStatus, EntityChangeStatus> {
    val existingCourtAppearance = getUnlessDeleted(lifetimeUuid)
    val existingCharge = getChargeUnlessDelete(lifetimeChargeUuid)
    var appearanceEntityChangeStatus = EntityChangeStatus.NO_CHANGE
    var chargeEntityStatus = EntityChangeStatus.NO_CHANGE
    if (existingCourtAppearance.charges.contains(existingCharge)) {
      existingCourtAppearance.charges.remove(existingCharge)
      existingCharge.courtAppearances.remove(existingCourtAppearance)
      appearanceEntityChangeStatus = EntityChangeStatus.EDITED
      if (existingCharge.hasNoActiveCourtAppearances()) {
        existingCharge.statusId = EntityStatus.DELETED
        chargeEntityStatus = EntityChangeStatus.DELETED
      }
    }
    return appearanceEntityChangeStatus to chargeEntityStatus
  }

  private fun getUnlessDeleted(lifetimeUuid: UUID): CourtAppearanceEntity = courtAppearanceRepository.findFirstByLifetimeUuidOrderByCreatedAtDesc(lifetimeUuid)
    ?.takeUnless { entity -> entity.statusId == EntityStatus.DELETED } ?: throw EntityNotFoundException("No court appearance found at $lifetimeUuid")

  private fun getChargeUnlessDelete(lifetimeChargeUuid: UUID): ChargeEntity = chargeRepository.findFirstByLifetimeChargeUuidOrderByCreatedAtDesc(lifetimeChargeUuid)?.takeUnless { entity -> entity.statusId == EntityStatus.DELETED } ?: throw EntityNotFoundException("No charge found at $lifetimeChargeUuid")
}
