package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.service

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.AppearanceOutcomeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.ChargeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.ChargeOutcomeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.CourtAppearanceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.CourtCaseEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.AppearanceOutcomeRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.ChargeOutcomeRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.ChargeRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.CourtAppearanceRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.CourtCaseRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.MigrationCreateCharge
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.MigrationCreateChargeResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.MigrationCreateCourtAppearance
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.MigrationCreateCourtAppearanceResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.MigrationCreateCourtCase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.MigrationCreateCourtCaseResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.ServiceUserService

@Service
class MigrationService(private val courtCaseRepository: CourtCaseRepository, private val courtAppearanceRepository: CourtAppearanceRepository, private val chargeRepository: ChargeRepository, private val appearanceOutcomeRepository: AppearanceOutcomeRepository, private val chargeOutcomeRepository: ChargeOutcomeRepository, private val serviceUserService: ServiceUserService, private val objectMapper: ObjectMapper) {

  @Transactional
  fun create(migrationCreateCourtCase: MigrationCreateCourtCase): MigrationCreateCourtCaseResponse {
    val createdByUsername = serviceUserService.getUsername()
    val createdCourtCase = courtCaseRepository.save(CourtCaseEntity.from(migrationCreateCourtCase, createdByUsername))
    val latestCourtCaseReference = migrationCreateCourtCase.courtCaseLegacyData.caseReferences.maxByOrNull { caseReferenceLegacyData -> caseReferenceLegacyData.updatedDate }?.offenderCaseReference
    val createdChargesMap: MutableMap<String, ChargeEntity> = HashMap()
    val createdAppearances = createAppearances(migrationCreateCourtCase.appearances, createdByUsername, createdCourtCase, latestCourtCaseReference, createdChargesMap)

    return MigrationCreateCourtCaseResponse(
      createdCourtCase.caseUniqueIdentifier,
      createdAppearances.map { (eventId, createdAppearance) -> MigrationCreateCourtAppearanceResponse(createdAppearance.lifetimeUuid, eventId) },
      createdChargesMap.map { (chargeNOMISId, createdCharge) -> MigrationCreateChargeResponse(createdCharge.lifetimeChargeUuid, chargeNOMISId) },
    )
  }

  fun createAppearances(migrationCreateAppearances: List<MigrationCreateCourtAppearance>, createdByUsername: String, createdCourtCase: CourtCaseEntity, courtCaseReference: String?, createdChargesMap: MutableMap<String, ChargeEntity>): Map<String, CourtAppearanceEntity> {
    val nomisAppearanceOutcomeIds = migrationCreateAppearances.filter { appearance -> appearance.legacyData.nomisOutcomeCode != null }.map { appearance -> appearance.legacyData.nomisOutcomeCode!! }.distinct()
    val dpsAppearanceOutcomes = appearanceOutcomeRepository.findByNomisCodeIn(nomisAppearanceOutcomeIds).associate { entity -> entity.nomisCode to entity }
    val nomisChargeOutcomeIds = migrationCreateAppearances.flatMap { courtAppearance -> courtAppearance.charges }.filter { charge -> charge.legacyData.nomisOutcomeCode != null }.map { charge -> charge.legacyData.nomisOutcomeCode!! }.distinct()
    val dpsChargeOutcomes = chargeOutcomeRepository.findByNomisCodeIn(nomisChargeOutcomeIds).associate { entity -> entity.nomisCode to entity }
    return migrationCreateAppearances.sortedBy { courtAppearance -> courtAppearance.appearanceDate }.associate { appearance -> appearance.legacyData.eventId!! to createAppearance(appearance, createdByUsername, createdCourtCase, courtCaseReference, dpsAppearanceOutcomes, dpsChargeOutcomes, createdChargesMap) }
  }

  fun createAppearance(migrationCreateCourtAppearance: MigrationCreateCourtAppearance, createdByUsername: String, createdCourtCase: CourtCaseEntity, courtCaseReference: String?, dpsAppearanceOutcomes: Map<String, AppearanceOutcomeEntity>, dpsChargeOutcomes: Map<String, ChargeOutcomeEntity>, createdChargesMap: MutableMap<String, ChargeEntity>): CourtAppearanceEntity {
    val dpsAppearanceOutcome = migrationCreateCourtAppearance.legacyData.nomisOutcomeCode?.let { dpsAppearanceOutcomes[it] }
    val legacyData = migrationCreateCourtAppearance.legacyData.let { legacyData -> objectMapper.valueToTree<JsonNode>(legacyData) }
    val createdAppearance = courtAppearanceRepository.save(CourtAppearanceEntity.from(migrationCreateCourtAppearance, dpsAppearanceOutcome, createdCourtCase, createdByUsername, legacyData, courtCaseReference))
    val charges = migrationCreateCourtAppearance.charges.map { charge -> createCharge(charge, createdByUsername, dpsChargeOutcomes, createdChargesMap) }
    createdAppearance.charges.addAll(charges)
    return createdAppearance
  }

  fun createCharge(migrationCreateCharge: MigrationCreateCharge, createdByUsername: String, dpsChargeOutcomes: Map<String, ChargeOutcomeEntity>, createdChargesMap: MutableMap<String, ChargeEntity>): ChargeEntity {
    val dpsChargeOutcome = migrationCreateCharge.legacyData.nomisOutcomeCode?.let { dpsChargeOutcomes[it] }
    val legacyData = migrationCreateCharge.legacyData.let { legacyData -> objectMapper.valueToTree<JsonNode>(legacyData) }
    val toCreateCharge = createdChargesMap[migrationCreateCharge.chargeNOMISId]?.let { existingCharge ->
      val chargeInAppearance = existingCharge.copyFrom(migrationCreateCharge, dpsChargeOutcome, legacyData, createdByUsername)
      if (existingCharge.isSame(chargeInAppearance)) existingCharge else chargeInAppearance
    } ?: ChargeEntity.from(migrationCreateCharge, dpsChargeOutcome, legacyData, createdByUsername)
    val createdCharge = chargeRepository.save(toCreateCharge)
    createdChargesMap.put(migrationCreateCharge.chargeNOMISId, createdCharge)
    return createdCharge
  }
}
