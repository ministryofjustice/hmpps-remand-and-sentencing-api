package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.service

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.AppearanceOutcomeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.ChargeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.ChargeOutcomeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.CourtAppearanceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.CourtCaseEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.FineAmountEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.NextCourtAppearanceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.PeriodLengthEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.SentenceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.SentenceTypeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.AppearanceOutcomeRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.AppearanceTypeRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.ChargeOutcomeRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.ChargeRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.CourtAppearanceRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.CourtCaseRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.FineAmountRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.NextCourtAppearanceRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.PeriodLengthRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.SentenceRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.SentenceTypeRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.MigrationCreateCharge
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.MigrationCreateChargeResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.MigrationCreateCourtAppearance
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.MigrationCreateCourtAppearanceResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.MigrationCreateCourtCase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.MigrationCreateCourtCaseResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.MigrationCreateSentence
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.MigrationCreateSentenceResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.MigrationSentenceId
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.ServiceUserService
import kotlin.collections.filter

@Service
class MigrationService(private val courtCaseRepository: CourtCaseRepository, private val courtAppearanceRepository: CourtAppearanceRepository, private val chargeRepository: ChargeRepository, private val appearanceOutcomeRepository: AppearanceOutcomeRepository, private val chargeOutcomeRepository: ChargeOutcomeRepository, private val serviceUserService: ServiceUserService, private val objectMapper: ObjectMapper, private val nextCourtAppearanceRepository: NextCourtAppearanceRepository, private val appearanceTypeRepository: AppearanceTypeRepository, private val sentenceTypeRepository: SentenceTypeRepository, private val sentenceRepository: SentenceRepository, private val fineAmountRepository: FineAmountRepository, private val periodLengthRepository: PeriodLengthRepository) {

  @Transactional
  fun create(migrationCreateCourtCase: MigrationCreateCourtCase): MigrationCreateCourtCaseResponse {
    val createdByUsername = serviceUserService.getUsername()
    val createdCourtCase = courtCaseRepository.save(CourtCaseEntity.from(migrationCreateCourtCase, createdByUsername))
    val latestCourtCaseReference = migrationCreateCourtCase.courtCaseLegacyData.caseReferences.maxByOrNull { caseReferenceLegacyData -> caseReferenceLegacyData.updatedDate }?.offenderCaseReference
    val createdChargesMap: MutableMap<String, ChargeEntity> = HashMap()
    val createdSentencesMap: MutableMap<MigrationSentenceId, SentenceEntity> = HashMap()
    val createdAppearances = createAppearances(migrationCreateCourtCase.appearances, createdByUsername, createdCourtCase, latestCourtCaseReference, createdChargesMap, createdSentencesMap)
    manageMatchedDpsNextCourtAppearances(migrationCreateCourtCase, createdAppearances)
    val latestCourtAppearance = createdAppearances.values.filter { courtAppearanceEntity -> courtAppearanceEntity.statusId == EntityStatus.ACTIVE }.maxByOrNull { courtAppearanceEntity -> courtAppearanceEntity.appearanceDate }
    createdCourtCase.latestCourtAppearance = latestCourtAppearance
    latestCourtAppearance?.let { managedNoMatchedDpsNextCourtAppearance(it, migrationCreateCourtCase, createdAppearances) }

    return MigrationCreateCourtCaseResponse(
      createdCourtCase.caseUniqueIdentifier,
      createdAppearances.map { (eventId, createdAppearance) -> MigrationCreateCourtAppearanceResponse(createdAppearance.lifetimeUuid, eventId) },
      createdChargesMap.map { (chargeNOMISId, createdCharge) -> MigrationCreateChargeResponse(createdCharge.lifetimeChargeUuid, chargeNOMISId) },
      createdSentencesMap.map { (id, createdSentence) -> MigrationCreateSentenceResponse(createdSentence.lifetimeSentenceUuid, id) },
    )
  }

  fun manageMatchedDpsNextCourtAppearances(migrationCreateCourtCase: MigrationCreateCourtCase, createdAppearances: Map<String, CourtAppearanceEntity>) {
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

  fun managedNoMatchedDpsNextCourtAppearance(latestCourtAppearance: CourtAppearanceEntity, migrationCreateCourtCase: MigrationCreateCourtCase, createdAppearances: Map<String, CourtAppearanceEntity>) {
    if (latestCourtAppearance.nextCourtAppearance == null && createdAppearances.values.any { it.statusId == EntityStatus.FUTURE }) {
      val (nextFutureDatedEventId, nextFutureDatedAppearance) = createdAppearances.filter { (_, courtAppearanceEntity) -> courtAppearanceEntity.statusId == EntityStatus.FUTURE }.minBy { (_, courtAppearanceEntity) -> courtAppearanceEntity.appearanceDate }
      val nomisNextFutureDatedAppearance = migrationCreateCourtCase.appearances.first { it.legacyData.eventId == nextFutureDatedEventId }
      val nextAppearanceType = appearanceTypeRepository.findByAppearanceTypeUuid(nomisNextFutureDatedAppearance.appearanceTypeUuid)!!
      latestCourtAppearance.nextCourtAppearance = nextCourtAppearanceRepository.save(
        NextCourtAppearanceEntity.from(nextFutureDatedAppearance, nextAppearanceType),
      )
    }
  }

  fun createAppearances(migrationCreateAppearances: List<MigrationCreateCourtAppearance>, createdByUsername: String, createdCourtCase: CourtCaseEntity, courtCaseReference: String?, createdChargesMap: MutableMap<String, ChargeEntity>, createdSentencesMap: MutableMap<MigrationSentenceId, SentenceEntity>): Map<String, CourtAppearanceEntity> {
    val nomisAppearanceOutcomeIds = migrationCreateAppearances.filter { appearance -> appearance.legacyData.nomisOutcomeCode != null }.map { appearance -> appearance.legacyData.nomisOutcomeCode!! }.distinct()
    val dpsAppearanceOutcomes = appearanceOutcomeRepository.findByNomisCodeIn(nomisAppearanceOutcomeIds).associate { entity -> entity.nomisCode to entity }
    val nomisChargeOutcomeIds = migrationCreateAppearances.flatMap { courtAppearance -> courtAppearance.charges }.filter { charge -> charge.legacyData.nomisOutcomeCode != null }.map { charge -> charge.legacyData.nomisOutcomeCode!! }.distinct()
    val dpsChargeOutcomes = chargeOutcomeRepository.findByNomisCodeIn(nomisChargeOutcomeIds).associate { entity -> entity.nomisCode to entity }
    val dpsSentenceTypes = getDpsSentenceTypesMap(migrationCreateAppearances)
    return migrationCreateAppearances.sortedBy { courtAppearance -> courtAppearance.appearanceDate }.associate { appearance -> appearance.legacyData.eventId!! to createAppearance(appearance, createdByUsername, createdCourtCase, courtCaseReference, dpsAppearanceOutcomes, dpsChargeOutcomes, createdChargesMap, dpsSentenceTypes, createdSentencesMap) }
  }

  fun createAppearance(migrationCreateCourtAppearance: MigrationCreateCourtAppearance, createdByUsername: String, createdCourtCase: CourtCaseEntity, courtCaseReference: String?, dpsAppearanceOutcomes: Map<String, AppearanceOutcomeEntity>, dpsChargeOutcomes: Map<String, ChargeOutcomeEntity>, createdChargesMap: MutableMap<String, ChargeEntity>, dpsSentenceTypes: Map<Pair<String, String>, SentenceTypeEntity>, createdSentencesMap: MutableMap<MigrationSentenceId, SentenceEntity>): CourtAppearanceEntity {
    val dpsAppearanceOutcome = migrationCreateCourtAppearance.legacyData.nomisOutcomeCode?.let { dpsAppearanceOutcomes[it] }
    val legacyData = migrationCreateCourtAppearance.legacyData.let { legacyData -> objectMapper.valueToTree<JsonNode>(legacyData) }
    val createdAppearance = courtAppearanceRepository.save(CourtAppearanceEntity.from(migrationCreateCourtAppearance, dpsAppearanceOutcome, createdCourtCase, createdByUsername, legacyData, courtCaseReference))

    val charges = migrationCreateCourtAppearance.charges.sortedWith(this::chargesByConsecutiveToLast).map { charge -> createCharge(charge, createdByUsername, dpsChargeOutcomes, createdChargesMap, dpsSentenceTypes, createdSentencesMap) }
    createdAppearance.charges.addAll(charges)
    return createdAppearance
  }

  private fun chargesByConsecutiveToLast(first: MigrationCreateCharge, second: MigrationCreateCharge): Int {
    if (first.sentence?.consecutiveToSentenceId == null) {
      return -1
    }
    if (first.sentence.consecutiveToSentenceId == second.sentence?.sentenceId) {
      return 1
    }
    return 0
  }

  fun createCharge(migrationCreateCharge: MigrationCreateCharge, createdByUsername: String, dpsChargeOutcomes: Map<String, ChargeOutcomeEntity>, createdChargesMap: MutableMap<String, ChargeEntity>, dpsSentenceTypes: Map<Pair<String, String>, SentenceTypeEntity>, createdSentencesMap: MutableMap<MigrationSentenceId, SentenceEntity>): ChargeEntity {
    val dpsChargeOutcome = migrationCreateCharge.legacyData.nomisOutcomeCode?.let { dpsChargeOutcomes[it] }
    val chargeLegacyData = dpsChargeOutcome?.let { migrationCreateCharge.legacyData.copy(nomisOutcomeCode = null, outcomeDescription = null, outcomeDispositionCode = null) } ?: migrationCreateCharge.legacyData
    val legacyData = objectMapper.valueToTree<JsonNode>(chargeLegacyData)
    val toCreateCharge = createdChargesMap[migrationCreateCharge.chargeNOMISId]?.let { existingCharge ->
      val chargeInAppearance = existingCharge.copyFrom(migrationCreateCharge, dpsChargeOutcome, legacyData, createdByUsername)
      if (existingCharge.isSame(chargeInAppearance)) existingCharge else chargeInAppearance
    } ?: ChargeEntity.from(migrationCreateCharge, dpsChargeOutcome, legacyData, createdByUsername)
    val createdCharge = chargeRepository.save(toCreateCharge)
    migrationCreateCharge.sentence?.let { migrationSentence -> createdCharge.sentences.add(createSentence(migrationSentence, createdCharge, createdByUsername, dpsSentenceTypes, createdSentencesMap)) }
    createdChargesMap.put(migrationCreateCharge.chargeNOMISId, createdCharge)
    return createdCharge
  }

  private fun getDpsSentenceTypesMap(migrationCreateAppearances: List<MigrationCreateCourtAppearance>): Map<Pair<String, String>, SentenceTypeEntity> {
    val (sentenceCalcTypes, sentenceCategories) = migrationCreateAppearances.flatMap { it.charges }.filter { charge -> charge.sentence != null && charge.sentence.legacyData.sentenceCalcType != null && charge.sentence.legacyData.sentenceCategory != null }.map { charge -> charge.sentence!!.legacyData.sentenceCalcType!! to charge.sentence.legacyData.sentenceCategory!! }.unzip()
    return sentenceTypeRepository.findByNomisCjaCodeInAndNomisSentenceCalcTypeIn(sentenceCategories, sentenceCalcTypes).associateBy { sentenceType -> sentenceType.nomisSentenceCalcType to sentenceType.nomisCjaCode }
  }

  fun createSentence(migrationCreateSentence: MigrationCreateSentence, chargeEntity: ChargeEntity, createdByUsername: String, dpsSentenceTypes: Map<Pair<String, String>, SentenceTypeEntity>, createdSentencesMap: MutableMap<MigrationSentenceId, SentenceEntity>): SentenceEntity {
    val dpsSentenceType = (migrationCreateSentence.legacyData.sentenceCalcType to migrationCreateSentence.legacyData.sentenceCategory).takeIf { (sentenceCalcType, sentenceCategory) -> sentenceCalcType != null && sentenceCategory != null }?.let { dpsSentenceTypes[it] }
    val sentenceLegacyData = dpsSentenceType?.let { migrationCreateSentence.legacyData.copy(sentenceCalcType = null, sentenceCategory = null, sentenceTypeDesc = null) } ?: migrationCreateSentence.legacyData
    val legacyData = objectMapper.valueToTree<JsonNode>(sentenceLegacyData)
    var consecutiveToSentence: SentenceEntity? = null
    if (migrationCreateSentence.consecutiveToSentenceId != null) {
      consecutiveToSentence = createdSentencesMap[migrationCreateSentence.consecutiveToSentenceId]
        ?: throw EntityNotFoundException("Cannot find sentence with booking id ${migrationCreateSentence.consecutiveToSentenceId.offenderBookingId} and sequence ${migrationCreateSentence.consecutiveToSentenceId.sequence}")
    } else if (migrationCreateSentence.consecutiveToSentenceLifetimeUuid != null) {
      consecutiveToSentence = sentenceRepository.findFirstByLifetimeSentenceUuidOrderByCreatedAtDesc(migrationCreateSentence.consecutiveToSentenceLifetimeUuid) ?: throw EntityNotFoundException("Cannot find sentence with lifetime uuid ${migrationCreateSentence.consecutiveToSentenceLifetimeUuid}")
    }

    val toCreateSentence = SentenceEntity.from(migrationCreateSentence, createdByUsername, chargeEntity, dpsSentenceType, legacyData, consecutiveToSentence)
    val createdSentence = sentenceRepository.save(toCreateSentence)
    createdSentence.fineAmountEntity = migrationCreateSentence.fine?.let { fineAmountRepository.save(FineAmountEntity.from(it)) }
    createdSentence.periodLengths = migrationCreateSentence.periodLengths.map {
      val createdPeriodLength = periodLengthRepository.save(PeriodLengthEntity.from(it, dpsSentenceType?.nomisSentenceCalcType ?: migrationCreateSentence.legacyData.sentenceCalcType!!))
      createdPeriodLength.sentenceEntity = createdSentence
      createdPeriodLength
    }
    createdSentencesMap.put(migrationCreateSentence.sentenceId, createdSentence)
    return createdSentence
  }
}
