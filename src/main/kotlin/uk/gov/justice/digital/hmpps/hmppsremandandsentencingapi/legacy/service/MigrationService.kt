package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.AppearanceChargeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.AppearanceOutcomeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.ChargeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.ChargeOutcomeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.CourtAppearanceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.CourtCaseEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.NextCourtAppearanceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.PeriodLengthEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.SentenceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.SentenceTypeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.audit.AppearanceChargeHistoryEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.audit.ChargeHistoryEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.audit.CourtAppearanceHistoryEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.audit.PeriodLengthHistoryEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.audit.SentenceHistoryEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityChangeStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.AppearanceOutcomeRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.AppearanceTypeRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.ChargeOutcomeRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.ChargeRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.CourtAppearanceRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.CourtCaseRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.NextCourtAppearanceRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.PeriodLengthRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.SentenceRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.SentenceTypeRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.audit.AppearanceChargeHistoryRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.audit.ChargeHistoryRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.audit.CourtAppearanceHistoryRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.audit.PeriodLengthHistoryRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.audit.SentenceHistoryRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.MigrationCreateCharge
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.MigrationCreateChargeResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.MigrationCreateCourtAppearance
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.MigrationCreateCourtAppearanceResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.MigrationCreateCourtCase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.MigrationCreateCourtCaseResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.MigrationCreateCourtCases
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.MigrationCreateCourtCasesResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.MigrationCreatePeriodLengthResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.MigrationCreateSentence
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.MigrationCreateSentenceResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.MigrationSentenceId
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.NomisPeriodLengthId
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.ServiceUserService
import kotlin.collections.filter

@Service
class MigrationService(
  private val courtCaseRepository: CourtCaseRepository,
  private val courtAppearanceRepository: CourtAppearanceRepository,
  private val chargeRepository: ChargeRepository,
  private val appearanceOutcomeRepository: AppearanceOutcomeRepository,
  private val chargeOutcomeRepository: ChargeOutcomeRepository,
  private val serviceUserService: ServiceUserService,
  private val nextCourtAppearanceRepository: NextCourtAppearanceRepository,
  private val appearanceTypeRepository: AppearanceTypeRepository,
  private val sentenceTypeRepository: SentenceTypeRepository,
  private val sentenceRepository: SentenceRepository,
  private val periodLengthRepository: PeriodLengthRepository,
  private val sentenceHistoryRepository: SentenceHistoryRepository,
  private val courtAppearanceHistoryRepository: CourtAppearanceHistoryRepository,
  private val chargeHistoryRepository: ChargeHistoryRepository,
  private val periodLengthHistoryRepository: PeriodLengthHistoryRepository,
  private val appearanceChargeHistoryRepository: AppearanceChargeHistoryRepository,
) {

  @Transactional
  fun create(migrationCreateCourtCases: MigrationCreateCourtCases): MigrationCreateCourtCasesResponse {
    val createdCourtCasesMap: MutableMap<Long, CourtCaseEntity> = HashMap()
    val createdCourtAppearancesMap: MutableMap<Long, CourtAppearanceEntity> = HashMap()
    val createdChargesMap: MutableMap<Long, ChargeEntity> = HashMap()
    val createdSentencesMap: MutableMap<MigrationSentenceId, SentenceEntity> = HashMap()
    val createdPeriodLengthMap: MutableMap<NomisPeriodLengthId, PeriodLengthEntity> = HashMap()

    migrationCreateCourtCases.courtCases.forEach { migrationCreateCourtCase ->
      createCourtCase(migrationCreateCourtCase, migrationCreateCourtCases.prisonerId, serviceUserService.getUsername(), createdCourtCasesMap, createdCourtAppearancesMap, createdChargesMap, createdSentencesMap, createdPeriodLengthMap)
    }

    return MigrationCreateCourtCasesResponse(
      createdCourtCasesMap.map { (caseId, createdCourtCase) -> MigrationCreateCourtCaseResponse(createdCourtCase.caseUniqueIdentifier, caseId) },
      createdCourtAppearancesMap.map { (eventId, createdAppearance) -> MigrationCreateCourtAppearanceResponse(createdAppearance.appearanceUuid, eventId) },
      createdChargesMap.map { (chargeNOMISId, createdCharge) -> MigrationCreateChargeResponse(createdCharge.chargeUuid, chargeNOMISId) },
      createdSentencesMap.map { (id, createdSentence) -> MigrationCreateSentenceResponse(createdSentence.sentenceUuid, id) },
      createdPeriodLengthMap.map { (id, createdPeriodLength) -> MigrationCreatePeriodLengthResponse(createdPeriodLength.periodLengthUuid, id) },
    )
  }

  fun manageMatchedDpsNextCourtAppearances(migrationCreateCourtCase: MigrationCreateCourtCase, createdAppearances: Map<Long, CourtAppearanceEntity>) {
    val nomisAppearances = migrationCreateCourtCase.appearances.associateBy { appearance -> appearance.eventId }
    val matchedNomisAppearances = migrationCreateCourtCase.appearances.filter { appearance -> appearance.legacyData.nextEventDateTime != null }.map { appearance ->
      appearance.eventId to migrationCreateCourtCase.appearances.firstOrNull { potentialAppearance ->
        appearance.legacyData.nextEventDateTime!!.toLocalDate().isEqual(potentialAppearance.appearanceDate)
      }?.eventId
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

  fun managedNoMatchedDpsNextCourtAppearance(latestCourtAppearance: CourtAppearanceEntity, migrationCreateCourtCase: MigrationCreateCourtCase, createdAppearances: Map<Long, CourtAppearanceEntity>) {
    if (latestCourtAppearance.nextCourtAppearance == null && createdAppearances.values.any { it.statusId == EntityStatus.FUTURE }) {
      val (nextFutureDatedEventId, nextFutureDatedAppearance) = createdAppearances.filter { (_, courtAppearanceEntity) -> courtAppearanceEntity.statusId == EntityStatus.FUTURE }.minBy { (_, courtAppearanceEntity) -> courtAppearanceEntity.appearanceDate }
      val nomisNextFutureDatedAppearance = migrationCreateCourtCase.appearances.first { it.eventId == nextFutureDatedEventId }
      val nextAppearanceType = appearanceTypeRepository.findByAppearanceTypeUuid(nomisNextFutureDatedAppearance.appearanceTypeUuid)!!
      latestCourtAppearance.nextCourtAppearance = nextCourtAppearanceRepository.save(
        NextCourtAppearanceEntity.from(nextFutureDatedAppearance, nextAppearanceType),
      )
    }
  }

  fun createCourtCase(migrationCreateCourtCase: MigrationCreateCourtCase, prisonerId: String, createdByUsername: String, createdCourtCaseMap: MutableMap<Long, CourtCaseEntity>, createdCourtAppearancesMap: MutableMap<Long, CourtAppearanceEntity>, createdChargesMap: MutableMap<Long, ChargeEntity>, createdSentencesMap: MutableMap<MigrationSentenceId, SentenceEntity>, createdPeriodLengthsMap: MutableMap<NomisPeriodLengthId, PeriodLengthEntity>) {
    val createdCourtCase = courtCaseRepository.save(CourtCaseEntity.from(migrationCreateCourtCase, createdByUsername, prisonerId))
    val latestCourtCaseReference = migrationCreateCourtCase.courtCaseLegacyData.caseReferences.maxByOrNull { caseReferenceLegacyData -> caseReferenceLegacyData.updatedDate }?.offenderCaseReference
    val createdAppearances = createAppearances(migrationCreateCourtCase.appearances, createdByUsername, createdCourtCase, latestCourtCaseReference, createdChargesMap, createdSentencesMap, createdPeriodLengthsMap)
    createdCourtAppearancesMap.putAll(createdAppearances)
    manageMatchedDpsNextCourtAppearances(migrationCreateCourtCase, createdAppearances)
    val latestCourtAppearance = createdAppearances.values.filter { courtAppearanceEntity -> courtAppearanceEntity.statusId == EntityStatus.ACTIVE }.maxByOrNull { courtAppearanceEntity -> courtAppearanceEntity.appearanceDate }
    createdCourtCase.latestCourtAppearance = latestCourtAppearance
    latestCourtAppearance?.let { managedNoMatchedDpsNextCourtAppearance(it, migrationCreateCourtCase, createdAppearances) }
    createdCourtCaseMap.put(migrationCreateCourtCase.caseId, createdCourtCase)
  }

  fun createAppearances(migrationCreateAppearances: List<MigrationCreateCourtAppearance>, createdByUsername: String, createdCourtCase: CourtCaseEntity, courtCaseReference: String?, createdChargesMap: MutableMap<Long, ChargeEntity>, createdSentencesMap: MutableMap<MigrationSentenceId, SentenceEntity>, createdPeriodLengthsMap: MutableMap<NomisPeriodLengthId, PeriodLengthEntity>): Map<Long, CourtAppearanceEntity> {
    val nomisAppearanceOutcomeIds = migrationCreateAppearances.filter { appearance -> appearance.legacyData.nomisOutcomeCode != null }.map { appearance -> appearance.legacyData.nomisOutcomeCode!! }.distinct()
    val dpsAppearanceOutcomes = appearanceOutcomeRepository.findByNomisCodeIn(nomisAppearanceOutcomeIds).associate { entity -> entity.nomisCode to entity }
    val nomisChargeOutcomeIds = migrationCreateAppearances.flatMap { courtAppearance -> courtAppearance.charges }.filter { charge -> charge.legacyData.nomisOutcomeCode != null }.map { charge -> charge.legacyData.nomisOutcomeCode!! }.distinct()
    val dpsChargeOutcomes = chargeOutcomeRepository.findByNomisCodeIn(nomisChargeOutcomeIds).associate { entity -> entity.nomisCode to entity }
    val dpsSentenceTypes = getDpsSentenceTypesMap(migrationCreateAppearances)
    return migrationCreateAppearances.sortedBy { courtAppearance -> courtAppearance.appearanceDate }.associate { appearance -> appearance.eventId to createAppearance(appearance, createdByUsername, createdCourtCase, courtCaseReference, dpsAppearanceOutcomes, dpsChargeOutcomes, createdChargesMap, dpsSentenceTypes, createdSentencesMap, createdPeriodLengthsMap) }
  }

  fun createAppearance(migrationCreateCourtAppearance: MigrationCreateCourtAppearance, createdByUsername: String, createdCourtCase: CourtCaseEntity, courtCaseReference: String?, dpsAppearanceOutcomes: Map<String, AppearanceOutcomeEntity>, dpsChargeOutcomes: Map<String, ChargeOutcomeEntity>, createdChargesMap: MutableMap<Long, ChargeEntity>, dpsSentenceTypes: Map<Pair<String, String?>, SentenceTypeEntity>, createdSentencesMap: MutableMap<MigrationSentenceId, SentenceEntity>, createdPeriodLengthsMap: MutableMap<NomisPeriodLengthId, PeriodLengthEntity>): CourtAppearanceEntity {
    val dpsAppearanceOutcome = migrationCreateCourtAppearance.legacyData.nomisOutcomeCode?.let { dpsAppearanceOutcomes[it] }
    val createdAppearance = courtAppearanceRepository.save(CourtAppearanceEntity.from(migrationCreateCourtAppearance, dpsAppearanceOutcome, createdCourtCase, createdByUsername, courtCaseReference))
    courtAppearanceHistoryRepository.save(CourtAppearanceHistoryEntity.from(createdAppearance))
    val charges = migrationCreateCourtAppearance.charges.map { charge -> createCharge(charge, createdByUsername, dpsChargeOutcomes, createdChargesMap, dpsSentenceTypes, createdSentencesMap, createdPeriodLengthsMap) }
    charges.forEach { charge ->
      val appearanceChargeEntity = AppearanceChargeEntity(
        courtAppearance = createdAppearance,
        charge = charge,
        createdBy = createdByUsername,
        createdPrison = null,
      )
      createdAppearance.appearanceCharges.add(appearanceChargeEntity)
      appearanceChargeHistoryRepository.save(AppearanceChargeHistoryEntity.from(appearanceChargeEntity))
    }
    return createdAppearance
  }

  fun createCharge(migrationCreateCharge: MigrationCreateCharge, createdByUsername: String, dpsChargeOutcomes: Map<String, ChargeOutcomeEntity>, createdChargesMap: MutableMap<Long, ChargeEntity>, dpsSentenceTypes: Map<Pair<String, String?>, SentenceTypeEntity>, createdSentencesMap: MutableMap<MigrationSentenceId, SentenceEntity>, createdPeriodLengthsMap: MutableMap<NomisPeriodLengthId, PeriodLengthEntity>): ChargeEntity {
    val dpsChargeOutcome = migrationCreateCharge.legacyData.nomisOutcomeCode?.let { dpsChargeOutcomes[it] }
    migrationCreateCharge.legacyData = dpsChargeOutcome?.let { migrationCreateCharge.legacyData.copy(nomisOutcomeCode = null, outcomeDescription = null, outcomeDispositionCode = null) } ?: migrationCreateCharge.legacyData

    val existingCharge = createdChargesMap[migrationCreateCharge.chargeNOMISId]
    val (toCreateCharge, toCreateChangeStatus) = if (existingCharge != null) {
      val chargeInAppearance = existingCharge.copyFrom(migrationCreateCharge, dpsChargeOutcome, createdByUsername)
      if (existingCharge.isSame(chargeInAppearance)) existingCharge to EntityChangeStatus.NO_CHANGE else chargeInAppearance to EntityChangeStatus.EDITED
    } else {
      ChargeEntity.from(migrationCreateCharge, dpsChargeOutcome, createdByUsername) to EntityChangeStatus.CREATED
    }
    val createdCharge = chargeRepository.save(toCreateCharge)
    if (toCreateChangeStatus != EntityChangeStatus.NO_CHANGE) {
      chargeHistoryRepository.save(ChargeHistoryEntity.from(createdCharge))
    }
    migrationCreateCharge.sentence?.let { migrationSentence -> createdCharge.sentences.add(createSentence(migrationSentence, createdCharge, createdByUsername, dpsSentenceTypes, createdSentencesMap, createdPeriodLengthsMap)) }
    createdChargesMap.put(migrationCreateCharge.chargeNOMISId, createdCharge)
    return createdCharge
  }

  private fun getDpsSentenceTypesMap(migrationCreateAppearances: List<MigrationCreateCourtAppearance>): Map<Pair<String, String?>, SentenceTypeEntity> {
    val (sentenceCalcTypes, sentenceCategories) = migrationCreateAppearances.flatMap { it.charges }.filter { charge -> charge.sentence != null && charge.sentence.legacyData.sentenceCalcType != null && charge.sentence.legacyData.sentenceCategory != null }.map { charge -> charge.sentence!!.legacyData.sentenceCalcType!! to charge.sentence.legacyData.sentenceCategory!! }.unzip()
    val dpsSentenceTypes: MutableMap<Pair<String, String?>, SentenceTypeEntity> = sentenceTypeRepository.findByNomisCjaCodeInAndNomisSentenceCalcTypeIn(sentenceCategories, sentenceCalcTypes).associateBy { sentenceType -> sentenceType.nomisSentenceCalcType to sentenceType.nomisCjaCode }.toMutableMap()
    val allRecallSentenceCalcTypes = LegacySentenceService.recallNomisSentenceCalcTypes.filter { sentenceCalcTypes.contains(it) }
    if (allRecallSentenceCalcTypes.isNotEmpty()) {
      val recallSentenceTypeBucket = sentenceTypeRepository.findBySentenceTypeUuid(LegacySentenceService.recallSentenceTypeBucketUuid)!!
      allRecallSentenceCalcTypes.forEach { recallSentenceCalcType ->
        dpsSentenceTypes.put(recallSentenceCalcType to null, recallSentenceTypeBucket)
      }
    }
    return dpsSentenceTypes
  }

  private fun getDpsSentenceType(dpsSentenceTypes: Map<Pair<String, String?>, SentenceTypeEntity>, sentenceTypeIdentifier: Pair<String?, String?>): SentenceTypeEntity? {
    val (sentenceCalcType, sentenceCategory) = sentenceTypeIdentifier
    if (LegacySentenceService.recallNomisSentenceCalcTypes.contains(sentenceCalcType)) {
      return dpsSentenceTypes[sentenceCalcType!! to null]
    }
    return dpsSentenceTypes[sentenceCalcType to sentenceCategory]
  }

  fun createSentence(migrationCreateSentence: MigrationCreateSentence, chargeEntity: ChargeEntity, createdByUsername: String, dpsSentenceTypes: Map<Pair<String, String?>, SentenceTypeEntity>, createdSentencesMap: MutableMap<MigrationSentenceId, SentenceEntity>, createdPeriodLengthsMap: MutableMap<NomisPeriodLengthId, PeriodLengthEntity>): SentenceEntity {
    val dpsSentenceType = (migrationCreateSentence.legacyData.sentenceCalcType to migrationCreateSentence.legacyData.sentenceCategory).takeIf { (sentenceCalcType, sentenceCategory) -> sentenceCalcType != null && sentenceCategory != null }?.let { getDpsSentenceType(dpsSentenceTypes, it) }
    migrationCreateSentence.legacyData = dpsSentenceType?.let { migrationCreateSentence.legacyData.copy(sentenceCalcType = null, sentenceCategory = null, sentenceTypeDesc = null) } ?: migrationCreateSentence.legacyData

    val toCreateSentence = SentenceEntity.from(migrationCreateSentence, createdByUsername, chargeEntity, dpsSentenceType)
    val createdSentence = sentenceRepository.save(toCreateSentence)
    sentenceHistoryRepository.save(SentenceHistoryEntity.from(createdSentence))
    createdSentence.periodLengths = migrationCreateSentence.periodLengths.map {
      val createdPeriodLength = periodLengthRepository.save(PeriodLengthEntity.from(it, dpsSentenceType?.nomisSentenceCalcType ?: migrationCreateSentence.legacyData.sentenceCalcType!!, serviceUserService.getUsername()))
      createdPeriodLength.sentenceEntity = createdSentence
      periodLengthHistoryRepository.save(PeriodLengthHistoryEntity.from(createdPeriodLength))
      createdPeriodLengthsMap.put(it.periodLengthId, createdPeriodLength)
      createdPeriodLength
    }.toMutableList()
    createdSentencesMap.put(migrationCreateSentence.sentenceId, createdSentence)
    return createdSentence
  }
}
