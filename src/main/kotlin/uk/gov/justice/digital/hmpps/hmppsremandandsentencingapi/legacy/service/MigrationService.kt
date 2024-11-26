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
    val createdChargesMap = createCharges(migrationCreateCourtCase.appearances.flatMap { appearance -> appearance.charges }, createdByUsername)
    val latestCourtCaseReference = migrationCreateCourtCase.courtCaseLegacyData.caseReferences.maxByOrNull { caseReferenceLegacyData -> caseReferenceLegacyData.updatedDate }?.offenderCaseReference
    val createdAppearances = createAppearances(migrationCreateCourtCase.appearances, createdByUsername, createdChargesMap, createdCourtCase, latestCourtCaseReference)

    return MigrationCreateCourtCaseResponse(
      createdCourtCase.caseUniqueIdentifier,
      createdAppearances.map { (eventId, createdAppearance) -> MigrationCreateCourtAppearanceResponse(createdAppearance.lifetimeUuid, eventId) },
      createdChargesMap.map { (chargeNOMISId, createdCharge) -> MigrationCreateChargeResponse(createdCharge.lifetimeChargeUuid, chargeNOMISId) },
    )
  }

  fun createAppearances(migrationCreateAppearances: List<MigrationCreateCourtAppearance>, createdByUsername: String, createdChargesMap: Map<String, ChargeEntity>, createdCourtCase: CourtCaseEntity, courtCaseReference: String?): Map<String, CourtAppearanceEntity> {
    val nomisAppearanceOutcomeIds = migrationCreateAppearances.filter { appearance -> appearance.legacyData.nomisOutcomeCode != null }.map { appearance -> appearance.legacyData.nomisOutcomeCode!! }.distinct()
    val dpsAppearanceOutcomes = appearanceOutcomeRepository.findByNomisCodeIn(nomisAppearanceOutcomeIds).associate { entity -> entity.nomisCode to entity }
    return migrationCreateAppearances.associate { appearance -> appearance.legacyData.eventId!! to createAppearance(appearance, createdByUsername, createdChargesMap, createdCourtCase, courtCaseReference, dpsAppearanceOutcomes) }
  }

  fun createAppearance(migrationCreateCourtAppearance: MigrationCreateCourtAppearance, createdByUsername: String, createdChargesMap: Map<String, ChargeEntity>, createdCourtCase: CourtCaseEntity, courtCaseReference: String?, dpsAppearanceOutcomes: Map<String, AppearanceOutcomeEntity>): CourtAppearanceEntity {
    val dpsAppearanceOutcome = migrationCreateCourtAppearance.legacyData.nomisOutcomeCode?.let { dpsAppearanceOutcomes[it] }
    val legacyData = migrationCreateCourtAppearance.legacyData.let { legacyData -> objectMapper.valueToTree<JsonNode>(legacyData) }
    val createdAppearance = courtAppearanceRepository.save(CourtAppearanceEntity.from(migrationCreateCourtAppearance, dpsAppearanceOutcome, createdCourtCase, createdByUsername, legacyData, courtCaseReference))
    createdAppearance.charges.addAll(migrationCreateCourtAppearance.charges.map { charge -> createdChargesMap[charge.chargeNOMISId]!! })
    return createdAppearance
  }

  fun createCharges(migrationCreateCharges: List<MigrationCreateCharge>, createdByUsername: String): Map<String, ChargeEntity> {
    val nomisChargeOutcomeIds = migrationCreateCharges.filter { charge -> charge.legacyData.nomisOutcomeCode != null }.map { charge -> charge.legacyData.nomisOutcomeCode!! }.distinct()
    val dpsChargeOutcomes = chargeOutcomeRepository.findByNomisCodeIn(nomisChargeOutcomeIds).associate { entity -> entity.nomisCode to entity }
    return migrationCreateCharges
      .associate { charge -> charge.chargeNOMISId to charge }
      .mapValues { (_, value) ->
        createCharge(value, createdByUsername, dpsChargeOutcomes)
      }
  }

  fun createCharge(migrationCreateCharge: MigrationCreateCharge, createdByUsername: String, dpsChargeOutcomes: Map<String, ChargeOutcomeEntity>): ChargeEntity {
    val dpsChargeOutcome = migrationCreateCharge.legacyData.nomisOutcomeCode?.let { dpsChargeOutcomes[it] }
    val legacyData = migrationCreateCharge.legacyData.let { legacyData -> objectMapper.valueToTree<JsonNode>(legacyData) }
    return chargeRepository.save(ChargeEntity.from(migrationCreateCharge, dpsChargeOutcome, legacyData, createdByUsername))
  }
}
