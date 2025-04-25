package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.service

import org.slf4j.LoggerFactory
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
    val createdChargesMap: MutableMap<Long, MutableList<Pair<Long, ChargeEntity>>> = HashMap()
    val createdSentencesMap: MutableMap<MigrationSentenceId, MutableSet<SentenceEntity>> = HashMap()
    val createdPeriodLengthMap: MutableMap<NomisPeriodLengthId, MutableSet<PeriodLengthEntity>> = HashMap()

    migrationCreateCourtCases.courtCases.forEach { migrationCreateCourtCase ->
      createCourtCase(migrationCreateCourtCase, migrationCreateCourtCases.prisonerId, serviceUserService.getUsername(), createdCourtCasesMap, createdCourtAppearancesMap, createdChargesMap, createdSentencesMap, createdPeriodLengthMap)
    }
    linkMergedCases(migrationCreateCourtCases, createdCourtCasesMap, createdChargesMap)
    linkConsecutiveToSentences(migrationCreateCourtCases, createdSentencesMap)
    auditCreatedRecords(
      createdCourtAppearancesMap.values,
      createdChargesMap.values.flatMap { it.map { it.second } }.distinct(),
      createdSentencesMap.values.flatMap { it }.distinct(),
      createdPeriodLengthMap.values.flatMap { it }.distinct(),
    )

    return MigrationCreateCourtCasesResponse(
      createdCourtCasesMap.map { (caseId, createdCourtCase) -> MigrationCreateCourtCaseResponse(createdCourtCase.caseUniqueIdentifier, caseId) },
      createdCourtAppearancesMap.map { (eventId, createdAppearance) -> MigrationCreateCourtAppearanceResponse(createdAppearance.appearanceUuid, eventId) },
      createdChargesMap.map { (chargeNOMISId, createdCharges) -> MigrationCreateChargeResponse(createdCharges.first().second.chargeUuid, chargeNOMISId) },
      createdSentencesMap.map { (id, createdSentences) -> MigrationCreateSentenceResponse(createdSentences.first().sentenceUuid, id) },
      createdPeriodLengthMap.map { (id, createdPeriodLengths) -> MigrationCreatePeriodLengthResponse(createdPeriodLengths.first().periodLengthUuid, id) },
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

  fun linkMergedCases(migrationCreateCourtCases: MigrationCreateCourtCases, createdCourtCasesMap: MutableMap<Long, CourtCaseEntity>, createdChargesMap: MutableMap<Long, MutableList<Pair<Long, ChargeEntity>>>) {
    val targetCourtCases = migrationCreateCourtCases.courtCases.filter { it.appearances.flatMap { it.charges }.any { it.mergedFromCaseId != null && it.mergedFromEventId != null && it.mergedChargeNOMISId != null } }
    targetCourtCases.flatMap { it.appearances }
      .forEach { appearance ->
        appearance.charges
          .filter { it.mergedFromCaseId != null && it.mergedFromEventId != null && it.mergedChargeNOMISId != null }
          .forEach { targetNomisCharge ->
            val sourceCourtCase = createdCourtCasesMap[targetNomisCharge.mergedFromCaseId]!!
            val (_, sourceCharge) = createdChargesMap[targetNomisCharge.mergedChargeNOMISId]!!.first { it.first == targetNomisCharge.mergedFromEventId }
            val (_, targetCharge) = createdChargesMap[targetNomisCharge.chargeNOMISId]!!.first { it.first == appearance.eventId }
            targetCharge.mergedFromCourtCase = sourceCourtCase
            targetCharge.supersedingCharge = sourceCharge
          }
      }
  }

  fun linkConsecutiveToSentences(migrationCreateCourtCases: MigrationCreateCourtCases, createdSentencesMap: MutableMap<MigrationSentenceId, MutableSet<SentenceEntity>>) {
    migrationCreateCourtCases.courtCases.flatMap { it.appearances }.flatMap { it.charges }.filter { it.sentence?.consecutiveToSentenceId != null }
      .map { it.sentence!! }
      .forEach { nomisSentence ->
        if (createdSentencesMap.contains(nomisSentence.consecutiveToSentenceId)) {
          val consecutiveToSentence = createdSentencesMap[nomisSentence.consecutiveToSentenceId]!!.minBy { it.id }
          val sentences = createdSentencesMap[nomisSentence.sentenceId]!!
          sentences.forEach { sentence ->
            sentence.consecutiveTo = consecutiveToSentence
          }
        } else {
          log.info("sentence with id {} {} has a non existent consecutive to sentence at id {} {}", nomisSentence.sentenceId.offenderBookingId, nomisSentence.sentenceId.sequence, nomisSentence.consecutiveToSentenceId!!.offenderBookingId, nomisSentence.consecutiveToSentenceId.sequence)
        }
      }
  }

  private fun auditCreatedRecords(
    courtAppearances: MutableCollection<CourtAppearanceEntity>,
    charges: List<ChargeEntity>,
    sentences: List<SentenceEntity>,
    periodLengths: List<PeriodLengthEntity>,
  ) {
    courtAppearanceHistoryRepository.saveAll(courtAppearances.map { CourtAppearanceHistoryEntity.from(it) })
    appearanceChargeHistoryRepository.saveAll(courtAppearances.flatMap { it.appearanceCharges }.distinct().map { AppearanceChargeHistoryEntity.from(it) })
    chargeHistoryRepository.saveAll(charges.map { ChargeHistoryEntity.from(it) })
    sentenceHistoryRepository.saveAll(sentences.map { SentenceHistoryEntity.from(it) })
    periodLengthHistoryRepository.saveAll(periodLengths.map { PeriodLengthHistoryEntity.from(it) })
  }

  fun createCourtCase(migrationCreateCourtCase: MigrationCreateCourtCase, prisonerId: String, createdByUsername: String, createdCourtCaseMap: MutableMap<Long, CourtCaseEntity>, createdCourtAppearancesMap: MutableMap<Long, CourtAppearanceEntity>, createdChargesMap: MutableMap<Long, MutableList<Pair<Long, ChargeEntity>>>, createdSentencesMap: MutableMap<MigrationSentenceId, MutableSet<SentenceEntity>>, createdPeriodLengthsMap: MutableMap<NomisPeriodLengthId, MutableSet<PeriodLengthEntity>>) {
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

  fun createAppearances(migrationCreateAppearances: List<MigrationCreateCourtAppearance>, createdByUsername: String, createdCourtCase: CourtCaseEntity, courtCaseReference: String?, createdChargesMap: MutableMap<Long, MutableList<Pair<Long, ChargeEntity>>>, createdSentencesMap: MutableMap<MigrationSentenceId, MutableSet<SentenceEntity>>, createdPeriodLengthsMap: MutableMap<NomisPeriodLengthId, MutableSet<PeriodLengthEntity>>): Map<Long, CourtAppearanceEntity> {
    val nomisAppearanceOutcomeIds = migrationCreateAppearances.filter { appearance -> appearance.legacyData.nomisOutcomeCode != null }.map { appearance -> appearance.legacyData.nomisOutcomeCode!! }.distinct()
    val dpsAppearanceOutcomes = appearanceOutcomeRepository.findByNomisCodeIn(nomisAppearanceOutcomeIds).associate { entity -> entity.nomisCode to entity }
    val nomisChargeOutcomeIds = migrationCreateAppearances.flatMap { courtAppearance -> courtAppearance.charges }.filter { charge -> charge.legacyData.nomisOutcomeCode != null }.map { charge -> charge.legacyData.nomisOutcomeCode!! }.distinct()
    val dpsChargeOutcomes = chargeOutcomeRepository.findByNomisCodeIn(nomisChargeOutcomeIds).associate { entity -> entity.nomisCode to entity }
    val dpsSentenceTypes = getDpsSentenceTypesMap(migrationCreateAppearances)
    return migrationCreateAppearances.sortedBy { courtAppearance -> courtAppearance.appearanceDate }.associate { appearance -> appearance.eventId to createAppearance(appearance, createdByUsername, createdCourtCase, courtCaseReference, dpsAppearanceOutcomes, dpsChargeOutcomes, createdChargesMap, dpsSentenceTypes, createdSentencesMap, createdPeriodLengthsMap) }
  }

  fun createAppearance(migrationCreateCourtAppearance: MigrationCreateCourtAppearance, createdByUsername: String, createdCourtCase: CourtCaseEntity, courtCaseReference: String?, dpsAppearanceOutcomes: Map<String, AppearanceOutcomeEntity>, dpsChargeOutcomes: Map<String, ChargeOutcomeEntity>, createdChargesMap: MutableMap<Long, MutableList<Pair<Long, ChargeEntity>>>, dpsSentenceTypes: Map<Pair<String, String?>, SentenceTypeEntity>, createdSentencesMap: MutableMap<MigrationSentenceId, MutableSet<SentenceEntity>>, createdPeriodLengthsMap: MutableMap<NomisPeriodLengthId, MutableSet<PeriodLengthEntity>>): CourtAppearanceEntity {
    val dpsAppearanceOutcome = migrationCreateCourtAppearance.legacyData.nomisOutcomeCode?.let { dpsAppearanceOutcomes[it] }
    val createdAppearance = courtAppearanceRepository.save(CourtAppearanceEntity.from(migrationCreateCourtAppearance, dpsAppearanceOutcome, createdCourtCase, createdByUsername, courtCaseReference))
    val charges = migrationCreateCourtAppearance.charges.map { charge -> createCharge(charge, createdByUsername, dpsChargeOutcomes, createdChargesMap, dpsSentenceTypes, createdSentencesMap, createdPeriodLengthsMap, migrationCreateCourtAppearance.eventId) }
    charges.forEach { charge ->
      val appearanceChargeEntity = AppearanceChargeEntity(
        createdAppearance,
        charge,
        createdByUsername,
        null,
      )
      createdAppearance.appearanceCharges.add(appearanceChargeEntity)
    }
    return createdAppearance
  }

  fun createCharge(migrationCreateCharge: MigrationCreateCharge, createdByUsername: String, dpsChargeOutcomes: Map<String, ChargeOutcomeEntity>, createdChargesMap: MutableMap<Long, MutableList<Pair<Long, ChargeEntity>>>, dpsSentenceTypes: Map<Pair<String, String?>, SentenceTypeEntity>, createdSentencesMap: MutableMap<MigrationSentenceId, MutableSet<SentenceEntity>>, createdPeriodLengthsMap: MutableMap<NomisPeriodLengthId, MutableSet<PeriodLengthEntity>>, eventId: Long): ChargeEntity {
    val dpsChargeOutcome = migrationCreateCharge.legacyData.nomisOutcomeCode?.let { dpsChargeOutcomes[it] }
    migrationCreateCharge.legacyData = dpsChargeOutcome?.let { migrationCreateCharge.legacyData.copy(nomisOutcomeCode = null, outcomeDescription = null, outcomeDispositionCode = null) } ?: migrationCreateCharge.legacyData
    val existingChangeRecords = createdChargesMap[migrationCreateCharge.chargeNOMISId] ?: mutableListOf()
    val existingCharge = existingChangeRecords.lastOrNull()?.second
    val toCreateCharge = if (existingCharge != null) {
      val chargeInAppearance = existingCharge.copyFrom(migrationCreateCharge, dpsChargeOutcome, createdByUsername)
      if (existingCharge.isSame(chargeInAppearance)) existingCharge else chargeInAppearance
    } else {
      ChargeEntity.from(migrationCreateCharge, dpsChargeOutcome, createdByUsername)
    }
    val createdCharge = chargeRepository.save(toCreateCharge)
    migrationCreateCharge.sentence?.let { migrationSentence -> createdCharge.sentences.add(createSentence(migrationSentence, createdCharge, createdByUsername, dpsSentenceTypes, createdSentencesMap, createdPeriodLengthsMap)) }
    existingChangeRecords.add(eventId to createdCharge)
    createdChargesMap.put(migrationCreateCharge.chargeNOMISId, existingChangeRecords)
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

  fun createSentence(migrationCreateSentence: MigrationCreateSentence, chargeEntity: ChargeEntity, createdByUsername: String, dpsSentenceTypes: Map<Pair<String, String?>, SentenceTypeEntity>, createdSentencesMap: MutableMap<MigrationSentenceId, MutableSet<SentenceEntity>>, createdPeriodLengthsMap: MutableMap<NomisPeriodLengthId, MutableSet<PeriodLengthEntity>>): SentenceEntity {
    val dpsSentenceType = (migrationCreateSentence.legacyData.sentenceCalcType to migrationCreateSentence.legacyData.sentenceCategory).takeIf { (sentenceCalcType, sentenceCategory) -> sentenceCalcType != null && sentenceCategory != null }?.let { getDpsSentenceType(dpsSentenceTypes, it) }
    migrationCreateSentence.legacyData = dpsSentenceType?.let { migrationCreateSentence.legacyData.copy(sentenceCalcType = null, sentenceCategory = null, sentenceTypeDesc = null) } ?: migrationCreateSentence.legacyData

    val existingSentences = createdSentencesMap[migrationCreateSentence.sentenceId] ?: mutableSetOf()
    val toCreateSentence = existingSentences.firstOrNull()?.let { existingSentence ->
      existingSentence.statusId = EntityStatus.MANY_CHARGES_DATA_FIX
      existingSentence.copyFrom(migrationCreateSentence, createdByUsername, chargeEntity, dpsSentenceType)
    } ?: SentenceEntity.from(migrationCreateSentence, createdByUsername, chargeEntity, dpsSentenceType)
    val createdSentence = sentenceRepository.save(toCreateSentence)
    existingSentences.add(createdSentence)

    createdSentence.periodLengths = migrationCreateSentence.periodLengths.map {
      val existingPeriodLengths = createdPeriodLengthsMap[it.periodLengthId] ?: mutableSetOf()
      val toCreatePeriodLength = existingPeriodLengths.firstOrNull()?.let { existingPeriodLength ->
        existingPeriodLength.statusId = EntityStatus.MANY_CHARGES_DATA_FIX
        val copiedPeriodLength = existingPeriodLength.copy()
        copiedPeriodLength.sentenceEntity = createdSentence
        copiedPeriodLength
      } ?: PeriodLengthEntity.from(it, dpsSentenceType?.nomisSentenceCalcType ?: migrationCreateSentence.legacyData.sentenceCalcType!!, serviceUserService.getUsername())
      val createdPeriodLength = periodLengthRepository.save(toCreatePeriodLength)
      createdPeriodLength.sentenceEntity = createdSentence
      existingPeriodLengths.add(createdPeriodLength)
      createdPeriodLengthsMap.put(it.periodLengthId, existingPeriodLengths)
      createdPeriodLength
    }.toMutableSet()
    createdSentencesMap.put(migrationCreateSentence.sentenceId, existingSentences)
    return createdSentence
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
