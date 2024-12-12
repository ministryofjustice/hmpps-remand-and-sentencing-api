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
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.NextCourtAppearanceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.AppearanceOutcomeRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.AppearanceTypeRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.ChargeOutcomeRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.ChargeRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.CourtAppearanceRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.CourtCaseRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.NextCourtAppearanceRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.MigrationCreateCharge
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.MigrationCreateChargeResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.MigrationCreateCourtAppearance
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.MigrationCreateCourtAppearanceResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.MigrationCreateCourtCase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.MigrationCreateCourtCaseResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.ServiceUserService
import java.time.LocalDate

@Service
class MigrationService(private val courtCaseRepository: CourtCaseRepository, private val courtAppearanceRepository: CourtAppearanceRepository, private val chargeRepository: ChargeRepository, private val appearanceOutcomeRepository: AppearanceOutcomeRepository, private val chargeOutcomeRepository: ChargeOutcomeRepository, private val serviceUserService: ServiceUserService, private val objectMapper: ObjectMapper, private val nextCourtAppearanceRepository: NextCourtAppearanceRepository, private val appearanceTypeRepository: AppearanceTypeRepository) {

  @Transactional
  fun create(migrationCreateCourtCase: MigrationCreateCourtCase): MigrationCreateCourtCaseResponse {
    val currentDate = LocalDate.now()
    val createdByUsername = serviceUserService.getUsername()
    val createdCourtCase = courtCaseRepository.save(CourtCaseEntity.from(migrationCreateCourtCase, createdByUsername))
    val latestCourtCaseReference = migrationCreateCourtCase.courtCaseLegacyData.caseReferences.maxByOrNull { caseReferenceLegacyData -> caseReferenceLegacyData.updatedDate }?.offenderCaseReference
    val createdChargesMap: MutableMap<String, ChargeEntity> = HashMap()
    val createdAppearances = createAppearances(migrationCreateCourtCase.appearances, createdByUsername, createdCourtCase, latestCourtCaseReference, createdChargesMap)
    createdCourtCase.latestCourtAppearance = createdAppearances.values.filter { courtAppearanceEntity -> currentDate.isAfter(courtAppearanceEntity.appearanceDate) || currentDate.isEqual(courtAppearanceEntity.appearanceDate) }.maxByOrNull { courtAppearanceEntity -> courtAppearanceEntity.appearanceDate }
    manageDpsNextCourtAppearances(migrationCreateCourtCase, createdAppearances)
    return MigrationCreateCourtCaseResponse(
      createdCourtCase.caseUniqueIdentifier,
      createdAppearances.map { (eventId, createdAppearance) -> MigrationCreateCourtAppearanceResponse(createdAppearance.lifetimeUuid, eventId) },
      createdChargesMap.map { (chargeNOMISId, createdCharge) -> MigrationCreateChargeResponse(createdCharge.lifetimeChargeUuid, chargeNOMISId) },
    )
  }

  fun manageDpsNextCourtAppearances(migrationCreateCourtCase: MigrationCreateCourtCase, createdAppearances: Map<String, CourtAppearanceEntity>) {
    val nomisAppearances = migrationCreateCourtCase.appearances.associateBy { appearance -> appearance.legacyData.eventId!! }
    val matchedNomisAppearances = migrationCreateCourtCase.appearances.filter { appearance -> appearance.legacyData.nextEventDateTime != null }.map { appearance ->
      appearance.legacyData.eventId!! to migrationCreateCourtCase.appearances.firstOrNull { potentialAppearance ->
        appearance.legacyData.nextEventDateTime!!.toLocalDate().isEqual(potentialAppearance.appearanceDate)
      }?.legacyData?.eventId
    }.filter { matchedNomisAppearance -> matchedNomisAppearance.second != null }

    matchedNomisAppearances.forEach { (appearanceId, nextAppearanceId) ->
      val createdAppearance = createdAppearances[appearanceId]!!
      val createdNextAppearance = createdAppearances[nextAppearanceId]!!
      val nomisAppearance = nomisAppearances[appearanceId]!!
      val nomisNextAppearance = nomisAppearances[nextAppearanceId]!!
      val nextAppearanceType = appearanceTypeRepository.findByAppearanceTypeUuid(nomisNextAppearance.appearanceTypeUuid)!!
      createdAppearance.nextCourtAppearance = nextCourtAppearanceRepository.save(
        NextCourtAppearanceEntity.from(nomisAppearance, nomisNextAppearance, createdNextAppearance, nextAppearanceType),
      )
    }
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
