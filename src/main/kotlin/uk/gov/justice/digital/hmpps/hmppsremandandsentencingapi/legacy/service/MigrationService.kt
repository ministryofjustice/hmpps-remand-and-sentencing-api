package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.service

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.AppearanceChargeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.AppearanceOutcomeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.ChargeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.ChargeOutcomeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.CourtAppearanceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.CourtCaseEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.LegacySentenceTypeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.NextCourtAppearanceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.PeriodLengthEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.RecallEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.RecallSentenceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.SentenceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.SentenceTypeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.audit.AppearanceChargeHistoryEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.audit.ChargeHistoryEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.audit.CourtAppearanceHistoryEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.audit.PeriodLengthHistoryEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.audit.SentenceHistoryEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.RecallType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.AppearanceOutcomeRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.AppearanceTypeRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.ChargeOutcomeRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.ChargeRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.CourtAppearanceRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.CourtCaseRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.LegacySentenceTypeRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.NextCourtAppearanceRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.PeriodLengthRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.RecallRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.RecallSentenceRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.RecallTypeRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.SentenceRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.SentenceTypeRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.audit.AppearanceChargeHistoryRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.audit.ChargeHistoryRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.audit.CourtAppearanceHistoryRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.audit.PeriodLengthHistoryRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.audit.SentenceHistoryRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.custom.CustomPrisonerDataRepository
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
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.RecallSentenceLegacyData
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.ServiceUserService

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
  private val legacySentenceTypeRepository: LegacySentenceTypeRepository,
  private val recallTypeRepository: RecallTypeRepository,
  private val recallRepository: RecallRepository,
  private val recallSentenceRepository: RecallSentenceRepository,
  private val customPrisonerDataRepository: CustomPrisonerDataRepository,
) {

  @Transactional
  fun create(migrationCreateCourtCases: MigrationCreateCourtCases, deleteExisting: Boolean): MigrationCreateCourtCasesResponse {
    if (deleteExisting) {
      deletePrisonerData(migrationCreateCourtCases.prisonerId)
    }

    val tracking = MigrationDataTracking(
      migrationCreateCourtCases.prisonerId,
      serviceUserService.getUsername(),
    )
    migrationCreateCourtCases.courtCases.forEach { migrationCreateCourtCase ->
      createCourtCase(migrationCreateCourtCase, tracking)
    }
    linkMergedCases(migrationCreateCourtCases, tracking)
    linkConsecutiveToSentences(migrationCreateCourtCases, tracking)
    auditCreatedRecords(
      tracking.createdCourtAppearancesMap.values,
      tracking.createdChargesMap.values.flatMap { it.map { it.second } }.distinct(),
      tracking.createdSentencesMap.values.flatMap { it }.distinct(),
      tracking.createdPeriodLengthMap.values.flatMap { it }.distinct(),
    )

    return MigrationCreateCourtCasesResponse(
      tracking.createdCourtCasesMap.map { (caseId, createdCourtCase) -> MigrationCreateCourtCaseResponse(createdCourtCase.caseUniqueIdentifier, caseId) },
      tracking.createdCourtAppearancesMap.map { (eventId, createdAppearance) -> MigrationCreateCourtAppearanceResponse(createdAppearance.appearanceUuid, eventId) },
      tracking.createdChargesMap.map { (chargeNOMISId, createdCharges) -> MigrationCreateChargeResponse(createdCharges.first().second.chargeUuid, chargeNOMISId) },
      tracking.createdSentencesMap.map { (id, createdSentences) -> MigrationCreateSentenceResponse(createdSentences.first().sentenceUuid, id) },
      tracking.createdPeriodLengthMap.map { (id, createdPeriodLengths) -> MigrationCreatePeriodLengthResponse(createdPeriodLengths.first().periodLengthUuid, id) },
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

  fun linkMergedCases(migrationCreateCourtCases: MigrationCreateCourtCases, tracking: MigrationDataTracking) {
    val targetCourtCases = migrationCreateCourtCases.courtCases.filter { it.appearances.flatMap { it.charges }.any { it.mergedFromCaseId != null && it.mergedFromEventId != null } }
    targetCourtCases.forEach { targetCourtCase ->
      targetCourtCase.appearances.forEach { appearance ->
        appearance.charges
          .filter { it.mergedFromCaseId != null && it.mergedFromEventId != null }
          .forEach { targetNomisCharge ->
            val sourceCourtCase = tracking.createdCourtCasesMap[targetNomisCharge.mergedFromCaseId]!!
            val targetCourtCase = tracking.createdCourtCasesMap[targetCourtCase.caseId]!!
            val (_, sourceCharge) = tracking.createdChargesMap[targetNomisCharge.chargeNOMISId]!!.first { it.first == targetNomisCharge.mergedFromEventId }
            val (_, targetCharge) = tracking.createdChargesMap[targetNomisCharge.chargeNOMISId]!!.first { it.first == appearance.eventId }
            targetCharge.mergedFromCourtCase = sourceCourtCase
            targetCharge.mergedFromDate = targetNomisCharge.mergedFromDate
            targetCharge.supersedingCharge = sourceCharge
            if (sourceCourtCase.mergedToCase == null) {
              sourceCourtCase.mergedToCase = targetCourtCase
            }
          }
      }
    }
  }

  fun linkConsecutiveToSentences(migrationCreateCourtCases: MigrationCreateCourtCases, tracking: MigrationDataTracking) {
    migrationCreateCourtCases.courtCases.flatMap { it.appearances }.flatMap { it.charges }.filter { it.sentence?.consecutiveToSentenceId != null }
      .map { it.sentence!! }
      .forEach { nomisSentence ->
        if (tracking.createdSentencesMap.contains(nomisSentence.consecutiveToSentenceId)) {
          val consecutiveToSentence = tracking.createdSentencesMap[nomisSentence.consecutiveToSentenceId]!!.minBy { it.id }
          val sentences = tracking.createdSentencesMap[nomisSentence.sentenceId]!!
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

  fun createCourtCase(migrationCreateCourtCase: MigrationCreateCourtCase, tracking: MigrationDataTracking) {
    val createdCourtCase = courtCaseRepository.save(CourtCaseEntity.from(migrationCreateCourtCase, tracking.createdByUsername, tracking.prisonerId))
    val latestCourtCaseReference = migrationCreateCourtCase.courtCaseLegacyData.caseReferences.maxByOrNull { caseReferenceLegacyData -> caseReferenceLegacyData.updatedDate }?.offenderCaseReference
    val createdAppearances = createAppearances(migrationCreateCourtCase.appearances, createdCourtCase, latestCourtCaseReference, tracking)
    tracking.createdCourtAppearancesMap.putAll(createdAppearances)
    manageMatchedDpsNextCourtAppearances(migrationCreateCourtCase, createdAppearances)
    val latestCourtAppearance = createdAppearances.values.filter { courtAppearanceEntity -> courtAppearanceEntity.statusId == EntityStatus.ACTIVE }.maxByOrNull { courtAppearanceEntity -> courtAppearanceEntity.appearanceDate }
    createdCourtCase.latestCourtAppearance = latestCourtAppearance
    latestCourtAppearance?.let { managedNoMatchedDpsNextCourtAppearance(it, migrationCreateCourtCase, createdAppearances) }
    tracking.createdCourtCasesMap.put(migrationCreateCourtCase.caseId, createdCourtCase)
  }

  fun createAppearances(migrationCreateAppearances: List<MigrationCreateCourtAppearance>, createdCourtCase: CourtCaseEntity, courtCaseReference: String?, tracking: MigrationDataTracking): Map<Long, CourtAppearanceEntity> {
    val nomisAppearanceOutcomeIds = migrationCreateAppearances.filter { appearance -> appearance.legacyData.nomisOutcomeCode != null }.map { appearance -> appearance.legacyData.nomisOutcomeCode!! }.distinct()
    val dpsAppearanceOutcomes = appearanceOutcomeRepository.findByNomisCodeIn(nomisAppearanceOutcomeIds).associate { entity -> entity.nomisCode to entity }
    val nomisChargeOutcomeIds = migrationCreateAppearances.flatMap { courtAppearance -> courtAppearance.charges }.filter { charge -> charge.legacyData.nomisOutcomeCode != null }.map { charge -> charge.legacyData.nomisOutcomeCode!! }.distinct()
    val dpsChargeOutcomes = chargeOutcomeRepository.findByNomisCodeIn(nomisChargeOutcomeIds).associate { entity -> entity.nomisCode to entity }
    val dpsSentenceTypes = getDpsSentenceTypesMap(migrationCreateAppearances)
    val legacySentenceTypes = getLegacySentenceTypesMap(migrationCreateAppearances)
    return migrationCreateAppearances.sortedBy { courtAppearance -> courtAppearance.appearanceDate }.associate { appearance -> appearance.eventId to createAppearance(appearance, createdCourtCase, courtCaseReference, tracking, MigrationReferenceData(dpsAppearanceOutcomes, dpsChargeOutcomes, dpsSentenceTypes, legacySentenceTypes)) }
  }

  fun createAppearance(migrationCreateCourtAppearance: MigrationCreateCourtAppearance, createdCourtCase: CourtCaseEntity, courtCaseReference: String?, tracking: MigrationDataTracking, referenceData: MigrationReferenceData): CourtAppearanceEntity {
    val dpsAppearanceOutcome = migrationCreateCourtAppearance.legacyData.nomisOutcomeCode?.let { referenceData.dpsAppearanceOutcomes[it] }
    val createdAppearance = courtAppearanceRepository.save(CourtAppearanceEntity.from(migrationCreateCourtAppearance, dpsAppearanceOutcome, createdCourtCase, tracking.createdByUsername, courtCaseReference))
    val charges = migrationCreateCourtAppearance.charges.map { charge -> createCharge(charge, tracking, referenceData, migrationCreateCourtAppearance.eventId) }
    charges.forEach { charge ->
      val appearanceChargeEntity = AppearanceChargeEntity(
        createdAppearance,
        charge,
        tracking.createdByUsername,
        null,
      )
      createdAppearance.appearanceCharges.add(appearanceChargeEntity)
    }
    return createdAppearance
  }

  fun createCharge(migrationCreateCharge: MigrationCreateCharge, tracking: MigrationDataTracking, referenceData: MigrationReferenceData, eventId: Long): ChargeEntity {
    val dpsChargeOutcome = migrationCreateCharge.legacyData.nomisOutcomeCode?.let { referenceData.dpsChargeOutcomes[it] }
    migrationCreateCharge.legacyData = dpsChargeOutcome?.let { migrationCreateCharge.legacyData.copy(nomisOutcomeCode = null, outcomeDescription = null, outcomeDispositionCode = null) } ?: migrationCreateCharge.legacyData
    val existingChangeRecords = tracking.createdChargesMap[migrationCreateCharge.chargeNOMISId] ?: mutableListOf()
    val existingCharge = existingChangeRecords.lastOrNull()?.second
    val toCreateCharge = if (existingCharge != null) {
      val chargeInAppearance = existingCharge.copyFrom(migrationCreateCharge, dpsChargeOutcome, tracking.createdByUsername)
      if (existingCharge.isSame(chargeInAppearance, migrationCreateCharge.sentence != null)) existingCharge else chargeInAppearance
    } else {
      ChargeEntity.from(migrationCreateCharge, dpsChargeOutcome, tracking.createdByUsername)
    }
    val createdCharge = chargeRepository.save(toCreateCharge)
    migrationCreateCharge.sentence?.let { migrationSentence -> createdCharge.sentences.add(createSentence(migrationSentence, createdCharge, tracking, referenceData)) }
    existingChangeRecords.add(eventId to createdCharge)
    tracking.createdChargesMap.put(migrationCreateCharge.chargeNOMISId, existingChangeRecords)
    return createdCharge
  }

  private fun getDpsSentenceTypesMap(migrationCreateAppearances: List<MigrationCreateCourtAppearance>): Map<Pair<String, String?>, SentenceTypeEntity> {
    val (sentenceCalcTypes, sentenceCategories) = migrationCreateAppearances.flatMap { it.charges }.filter { charge -> charge.sentence != null && charge.sentence.legacyData.sentenceCalcType != null && charge.sentence.legacyData.sentenceCategory != null }.map { charge -> charge.sentence!!.legacyData.sentenceCalcType!! to charge.sentence.legacyData.sentenceCategory!! }.unzip()
    val dpsSentenceTypes: MutableMap<Pair<String, String?>, SentenceTypeEntity> = sentenceTypeRepository.findByNomisCjaCodeInAndNomisSentenceCalcTypeIn(sentenceCategories.distinct(), sentenceCalcTypes.distinct()).associateBy { sentenceType -> sentenceType.nomisSentenceCalcType to sentenceType.nomisCjaCode }.toMutableMap()
    val allRecallSentenceCalcTypes = LegacySentenceService.recallNomisSentenceCalcTypes.filter { sentenceCalcTypes.contains(it) }
    if (allRecallSentenceCalcTypes.isNotEmpty()) {
      val recallSentenceTypeBucket = sentenceTypeRepository.findBySentenceTypeUuid(LegacySentenceService.recallSentenceTypeBucketUuid)!!
      allRecallSentenceCalcTypes.forEach { recallSentenceCalcType ->
        dpsSentenceTypes.put(recallSentenceCalcType to null, recallSentenceTypeBucket)
      }
    }
    return dpsSentenceTypes
  }

  private fun getLegacySentenceTypesMap(migrationCreateAppearances: List<MigrationCreateCourtAppearance>): Map<Pair<String, Int>, LegacySentenceTypeEntity> {
    val (sentenceCalcTypes, sentenceCategories) = migrationCreateAppearances.flatMap { it.charges }.filter { charge -> charge.sentence != null && charge.sentence.legacyData.sentenceCalcType != null && charge.sentence.legacyData.sentenceCategory != null && charge.sentence.legacyData.sentenceCategory!!.toIntOrNull() != null }.map { charge -> charge.sentence!!.legacyData.sentenceCalcType!! to charge.sentence.legacyData.sentenceCategory!!.toInt() }.unzip()
    val legacySentenceTypes: MutableMap<Pair<String, Int>, LegacySentenceTypeEntity> = legacySentenceTypeRepository.findByNomisSentenceTypeReferenceInAndSentencingActIn(sentenceCalcTypes.distinct(), sentenceCategories.distinct()).associateBy { sentenceType -> sentenceType.nomisSentenceTypeReference to sentenceType.sentencingAct }.toMutableMap()
    return legacySentenceTypes
  }

  private fun getDpsSentenceType(dpsSentenceTypes: Map<Pair<String, String?>, SentenceTypeEntity>, sentenceTypeIdentifier: Pair<String?, String?>): SentenceTypeEntity? {
    val (sentenceCalcType, sentenceCategory) = sentenceTypeIdentifier
    if (LegacySentenceService.recallNomisSentenceCalcTypes.contains(sentenceCalcType)) {
      return dpsSentenceTypes[sentenceCalcType!! to null]
    }
    return dpsSentenceTypes[sentenceCalcType to sentenceCategory]
  }

  fun createSentence(migrationCreateSentence: MigrationCreateSentence, chargeEntity: ChargeEntity, tracking: MigrationDataTracking, referenceData: MigrationReferenceData): SentenceEntity {
    val dpsSentenceType = (migrationCreateSentence.legacyData.sentenceCalcType to migrationCreateSentence.legacyData.sentenceCategory).takeIf { (sentenceCalcType, sentenceCategory) -> sentenceCalcType != null && sentenceCategory != null }?.let { getDpsSentenceType(referenceData.dpsSentenceTypes, it) }
    val legacyData = migrationCreateSentence.legacyData
    migrationCreateSentence.legacyData = dpsSentenceType?.let { if (it.sentenceTypeUuid == LegacySentenceService.recallSentenceTypeBucketUuid) migrationCreateSentence.legacyData else migrationCreateSentence.legacyData.copy(sentenceCalcType = null, sentenceCategory = null, sentenceTypeDesc = null) } ?: migrationCreateSentence.legacyData

    val existingSentences = tracking.createdSentencesMap[migrationCreateSentence.sentenceId] ?: mutableListOf()
    val toCreateSentence = existingSentences.firstOrNull()?.let { existingSentence ->
      existingSentence.statusId = EntityStatus.MANY_CHARGES_DATA_FIX
      existingSentence.copyFrom(migrationCreateSentence, tracking.createdByUsername, chargeEntity, dpsSentenceType)
    } ?: SentenceEntity.from(migrationCreateSentence, tracking.createdByUsername, chargeEntity, dpsSentenceType)
    val createdSentence = sentenceRepository.save(toCreateSentence)
    existingSentences.add(createdSentence)

    if (dpsSentenceType?.sentenceTypeUuid == LegacySentenceService.recallSentenceTypeBucketUuid) {
      createRecall(migrationCreateSentence, createdSentence, tracking, referenceData, RecallSentenceLegacyData.from(legacyData))
    }

    createdSentence.periodLengths = migrationCreateSentence.periodLengths.map {
      val existingPeriodLengths = tracking.createdPeriodLengthMap[it.periodLengthId] ?: mutableListOf()
      val toCreatePeriodLength = existingPeriodLengths.firstOrNull()?.let { existingPeriodLength ->
        existingPeriodLength.statusId = EntityStatus.MANY_CHARGES_DATA_FIX
        val copiedPeriodLength = existingPeriodLength.copy()
        copiedPeriodLength.sentenceEntity = createdSentence
        copiedPeriodLength
      } ?: PeriodLengthEntity.from(it, dpsSentenceType?.nomisSentenceCalcType ?: migrationCreateSentence.legacyData.sentenceCalcType!!, serviceUserService.getUsername())
      val createdPeriodLength = periodLengthRepository.save(toCreatePeriodLength)
      createdPeriodLength.sentenceEntity = createdSentence
      existingPeriodLengths.add(createdPeriodLength)
      tracking.createdPeriodLengthMap.put(it.periodLengthId, existingPeriodLengths)
      createdPeriodLength
    }.toMutableSet()
    tracking.createdSentencesMap.put(migrationCreateSentence.sentenceId, existingSentences)
    return createdSentence
  }

  private fun createRecall(migrationCreateSentence: MigrationCreateSentence, createdSentence: SentenceEntity, tracking: MigrationDataTracking, referenceData: MigrationReferenceData, recallSentenceLegacyData: RecallSentenceLegacyData) {
    val legacySentenceType = (migrationCreateSentence.legacyData.sentenceCalcType to migrationCreateSentence.legacyData.sentenceCategory)
      .takeIf { (sentenceCalcType, sentenceCategory) -> sentenceCalcType != null && sentenceCategory != null && sentenceCategory.toIntOrNull() != null }
      ?.let { referenceData.legacySentenceTypes[it.first to it.second!!.toInt()] }
    val defaultRecallType = recallTypeRepository.findOneByCode(RecallType.LR)!!
    val recall = recallRepository.save(RecallEntity.fromMigration(migrationCreateSentence, tracking.prisonerId, tracking.createdByUsername, legacySentenceType?.recallType ?: defaultRecallType))
    recallSentenceRepository.save(RecallSentenceEntity.fromMigration(createdSentence, recall, tracking.createdByUsername, recallSentenceLegacyData))
  }

  fun deletePrisonerData(prisonerId: String) {
    log.info("Starting delete of prisoner data for $prisonerId")
    customPrisonerDataRepository.deletePrisonerData(prisonerId)
    log.info("Finished delete of prisoner data for $prisonerId")
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  data class MigrationDataTracking(
    val prisonerId: String,
    val createdByUsername: String,
    val createdCourtCasesMap: MutableMap<Long, CourtCaseEntity> = HashMap(),
    val createdCourtAppearancesMap: MutableMap<Long, CourtAppearanceEntity> = HashMap(),
    val createdChargesMap: MutableMap<Long, MutableList<Pair<Long, ChargeEntity>>> = HashMap(),
    val createdSentencesMap: MutableMap<MigrationSentenceId, MutableList<SentenceEntity>> = HashMap(),
    val createdPeriodLengthMap: MutableMap<NomisPeriodLengthId, MutableList<PeriodLengthEntity>> = HashMap(),
  )

  data class MigrationReferenceData(
    val dpsAppearanceOutcomes: Map<String, AppearanceOutcomeEntity>,
    val dpsChargeOutcomes: Map<String, ChargeOutcomeEntity>,
    val dpsSentenceTypes: Map<Pair<String, String?>, SentenceTypeEntity>,
    val legacySentenceTypes: Map<Pair<String, Int>, LegacySentenceTypeEntity>,
  )
}
