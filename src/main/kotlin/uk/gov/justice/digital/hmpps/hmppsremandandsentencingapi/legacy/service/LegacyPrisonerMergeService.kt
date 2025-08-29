package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.EventMetadata
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.EventType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.RecordResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.util.EventMetadataCreator
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
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.NomisPeriodLengthId
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.RecallSentenceLegacyData
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.merge.DeactivatedCourtCase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.merge.DeactivatedSentence
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.merge.MergeCreateCharge
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.merge.MergeCreateChargeResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.merge.MergeCreateCourtAppearance
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.merge.MergeCreateCourtAppearanceResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.merge.MergeCreateCourtCase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.merge.MergeCreateCourtCaseResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.merge.MergeCreateCourtCasesResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.merge.MergeCreatePeriodLengthResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.merge.MergeCreateSentence
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.merge.MergeCreateSentenceResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.merge.MergePerson
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.merge.MergeSentenceId
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.ServiceUserService
import java.util.*

@Service
class LegacyPrisonerMergeService(
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
) {

  @Transactional
  fun process(mergePerson: MergePerson, retainedPrisonerNumber: String): RecordResponse<MergeCreateCourtCasesResponse> {
    val courtCases = courtCaseRepository.findAllByPrisonerId(mergePerson.removedPrisonerNumber)
    val deactivatedCourtCasesMap = mergePerson.casesDeactivated.associateBy { it.dpsCourtCaseUuid }
    val deactivatedSentencesMap = mergePerson.sentencesDeactivated.associateBy { it.dpsSentenceUuid }
    val trackingData = PrisonerMergeDataTracking(retainedPrisonerNumber, serviceUserService.getUsername())
    processExistingCourtCases(courtCases, deactivatedCourtCasesMap, trackingData)
    processExistingSentences(courtCases, deactivatedSentencesMap, trackingData)
    val createdResponse = processCreateCourtCases(mergePerson, trackingData)
    auditRecords(trackingData)
    return RecordResponse(createdResponse, trackingData.eventsToEmit)
  }

  fun processExistingCourtCases(courtCases: List<CourtCaseEntity>, deactivatedCourtCasesMap: Map<String, DeactivatedCourtCase>, trackingData: PrisonerMergeDataTracking) {
    courtCases.forEach { courtCase ->
      courtCase.prisonerId = trackingData.retainedPrisonerNumber
      deactivatedCourtCasesMap[courtCase.caseUniqueIdentifier]?.also {
        val newStatus = if (it.active) EntityStatus.ACTIVE else EntityStatus.INACTIVE
        courtCase.statusId = newStatus
      }
      trackingData.editedCourtCases.add(courtCase)
      trackingData.eventsToEmit.add(
        EventMetadataCreator.courtCaseEventMetadata(
          trackingData.retainedPrisonerNumber,
          courtCase.caseUniqueIdentifier,
          EventType.COURT_CASE_UPDATED,
        ),
      )
    }
  }

  fun processExistingSentences(courtCases: List<CourtCaseEntity>, deactivatedSentencesMap: Map<UUID, DeactivatedSentence>, trackingData: PrisonerMergeDataTracking) {
    courtCases.forEach { courtCase ->
      courtCase.appearances.forEach { courtAppearance ->
        courtAppearance.appearanceCharges.filter { appearanceCharge ->
          appearanceCharge.charge?.getActiveOrInactiveSentence() != null
        }.map { it.charge!!.getActiveOrInactiveSentence()!! }
          .filter { deactivatedSentencesMap.containsKey(it.sentenceUuid) }
          .forEach { sentenceEntity ->
            val active = deactivatedSentencesMap[sentenceEntity.sentenceUuid]!!.active
            var hasChanged = false
            if (sentenceEntity.statusId != EntityStatus.MANY_CHARGES_DATA_FIX) {
              val newStatus = if (active) EntityStatus.ACTIVE else EntityStatus.INACTIVE
              hasChanged = newStatus != sentenceEntity.statusId
              sentenceEntity.statusId = newStatus
            }
            hasChanged = hasChanged || active != sentenceEntity.legacyData?.active
            sentenceEntity.legacyData?.active = active
            if (hasChanged) {
              trackingData.editedSentences.add(sentenceEntity)
              trackingData.eventsToEmit.add(
                EventMetadataCreator.sentenceEventMetadata(
                  trackingData.retainedPrisonerNumber,
                  courtCase.caseUniqueIdentifier,
                  sentenceEntity.charge.chargeUuid.toString(),
                  sentenceEntity.sentenceUuid.toString(),
                  courtAppearance.appearanceUuid.toString(),
                  EventType.SENTENCE_UPDATED,
                ),
              )
            }
          }
      }
    }
  }

  fun processCreateCourtCases(mergePerson: MergePerson, trackingData: PrisonerMergeDataTracking): MergeCreateCourtCasesResponse {
    mergePerson.casesCreated.forEach { mergeCreateCourtCase ->
      createCourtCase(mergeCreateCourtCase, trackingData)
    }
    linkMergedCases(mergePerson, trackingData)
    linkConsecutiveToSentences(mergePerson, trackingData)
    return MergeCreateCourtCasesResponse(
      trackingData.createdCourtCasesMap.map { (caseId, createdCourtCase) -> MergeCreateCourtCaseResponse(createdCourtCase.record.caseUniqueIdentifier, caseId) },
      trackingData.createdCourtAppearancesMap.map { (eventId, createdAppearance) -> MergeCreateCourtAppearanceResponse(createdAppearance.appearanceUuid, eventId) },
      trackingData.createdChargesMap.map { (chargeNOMISId, createdCharges) -> MergeCreateChargeResponse(createdCharges.first().second.chargeUuid, chargeNOMISId) },
      trackingData.createdSentencesMap.map { (id, createdSentences) -> MergeCreateSentenceResponse(createdSentences.first().sentenceUuid, id) },
      trackingData.createdPeriodLengthMap.map { (id, createdPeriodLengths) -> MergeCreatePeriodLengthResponse(createdPeriodLengths.first().periodLengthUuid, id) },
    )
  }

  fun auditRecords(trackingData: PrisonerMergeDataTracking) {
    trackingData.editedSentences.forEach { sentenceEntity ->
      sentenceHistoryRepository.save(SentenceHistoryEntity.from(sentenceEntity))
    }
    courtAppearanceHistoryRepository.saveAll(trackingData.createdCourtAppearancesMap.values.map { CourtAppearanceHistoryEntity.from(it) })
    appearanceChargeHistoryRepository.saveAll(trackingData.createdCourtAppearancesMap.values.flatMap { it.appearanceCharges }.distinct().map { AppearanceChargeHistoryEntity.from(it) })
    chargeHistoryRepository.saveAll(trackingData.createdChargesMap.values.flatMap { it.map { it.second } }.distinct().map { ChargeHistoryEntity.from(it) })
    sentenceHistoryRepository.saveAll(trackingData.createdSentencesMap.values.flatMap { it }.distinct().map { SentenceHistoryEntity.from(it) })
    periodLengthHistoryRepository.saveAll(trackingData.createdPeriodLengthMap.values.flatMap { it }.distinct().map { PeriodLengthHistoryEntity.from(it) })
  }

  fun manageMatchedDpsNextCourtAppearances(mergeCreateCourtCase: MergeCreateCourtCase, createdAppearances: Map<Long, CourtAppearanceEntity>) {
    val nomisAppearances = mergeCreateCourtCase.appearances.associateBy { appearance -> appearance.eventId }
    val matchedNomisAppearances = mergeCreateCourtCase.appearances.filter { appearance -> appearance.legacyData.nextEventDateTime != null }.map { appearance ->
      appearance.eventId to mergeCreateCourtCase.appearances.firstOrNull { potentialAppearance ->
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

  fun managedNoMatchedDpsNextCourtAppearance(latestCourtAppearance: CourtAppearanceEntity, mergeCreateCourtCase: MergeCreateCourtCase, createdAppearances: Map<Long, CourtAppearanceEntity>) {
    if (latestCourtAppearance.nextCourtAppearance == null && createdAppearances.values.any { it.statusId == EntityStatus.FUTURE }) {
      val (nextFutureDatedEventId, nextFutureDatedAppearance) = createdAppearances.filter { (_, courtAppearanceEntity) -> courtAppearanceEntity.statusId == EntityStatus.FUTURE }.minBy { (_, courtAppearanceEntity) -> courtAppearanceEntity.appearanceDate }
      val nomisNextFutureDatedAppearance = mergeCreateCourtCase.appearances.first { it.eventId == nextFutureDatedEventId }
      val nextAppearanceType = appearanceTypeRepository.findByAppearanceTypeUuid(nomisNextFutureDatedAppearance.appearanceTypeUuid)!!
      latestCourtAppearance.nextCourtAppearance = nextCourtAppearanceRepository.save(
        NextCourtAppearanceEntity.from(nextFutureDatedAppearance, nextAppearanceType),
      )
    }
  }

  fun linkMergedCases(mergePerson: MergePerson, tracking: PrisonerMergeDataTracking) {
    val targetCourtCases = mergePerson.casesCreated.filter { it.appearances.flatMap { it.charges }.any { it.mergedFromCaseId != null } }
    targetCourtCases.forEach { targetCourtCase ->
      targetCourtCase.appearances.forEach { appearance ->
        appearance.charges
          .filter { it.mergedFromCaseId != null }
          .forEach { targetNomisCharge ->

            val sourceCourtCaseEntity = tracking.createdCourtCasesMap[targetNomisCharge.mergedFromCaseId]
            if (sourceCourtCaseEntity != null) {
              val targetCourtCaseEntity = tracking.createdCourtCasesMap[targetCourtCase.caseId]!!

              val (_, targetCharge) = tracking.createdChargesMap[targetNomisCharge.chargeNOMISId]!!.first { it.first == appearance.eventId }
              targetCharge.mergedFromCourtCase = sourceCourtCaseEntity.record
              targetCharge.mergedFromDate = targetNomisCharge.mergedFromDate
              val lastSourceAppearance =
                sourceCourtCaseEntity.request.appearances.filter { appearance -> appearance.charges.any { charge -> charge.chargeNOMISId == targetNomisCharge.chargeNOMISId } }
                  .maxByOrNull { it.appearanceDate }
              if (lastSourceAppearance != null) {
                val (_, sourceCharge) = tracking.createdChargesMap[targetNomisCharge.chargeNOMISId]!!.first { it.first == lastSourceAppearance.eventId }
                targetCharge.supersedingCharge = sourceCharge
                sourceCharge.statusId = EntityStatus.MERGED
              } else {
                log.info("charge ${targetNomisCharge.chargeNOMISId} is no longer associated with source case ${targetNomisCharge.mergedFromCaseId} but is on target ${targetCourtCase.caseId}")
              }
              if (sourceCourtCaseEntity.record.mergedToCase == null) {
                sourceCourtCaseEntity.record.mergedToCase = targetCourtCaseEntity.record
                sourceCourtCaseEntity.record.mergedToDate = targetNomisCharge.mergedFromDate
              }
            } else {
              log.info("charge ${targetNomisCharge.chargeNOMISId} is referencing a non existing source case at ${targetNomisCharge.mergedFromCaseId}")
            }
          }
      }
    }
  }

  fun linkConsecutiveToSentences(mergePerson: MergePerson, tracking: PrisonerMergeDataTracking) {
    mergePerson.casesCreated.flatMap { it.appearances }.flatMap { it.charges }.filter { it.sentence?.consecutiveToSentenceId != null }
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

  fun createCourtCase(mergeCreateCourtCase: MergeCreateCourtCase, tracking: PrisonerMergeDataTracking) {
    val createdCourtCase = courtCaseRepository.save(CourtCaseEntity.from(mergeCreateCourtCase, tracking.username, tracking.retainedPrisonerNumber))
    val latestCourtCaseReference = mergeCreateCourtCase.courtCaseLegacyData.caseReferences.maxByOrNull { caseReferenceLegacyData -> caseReferenceLegacyData.updatedDate }?.offenderCaseReference
    val createdAppearances = createAppearances(mergeCreateCourtCase.appearances, createdCourtCase, latestCourtCaseReference, tracking)
    tracking.createdCourtAppearancesMap.putAll(createdAppearances)
    manageMatchedDpsNextCourtAppearances(mergeCreateCourtCase, createdAppearances)
    val latestCourtAppearance = createdAppearances.values.filter { courtAppearanceEntity -> courtAppearanceEntity.statusId == EntityStatus.ACTIVE }.maxByOrNull { courtAppearanceEntity -> courtAppearanceEntity.appearanceDate }
    createdCourtCase.latestCourtAppearance = latestCourtAppearance
    latestCourtAppearance?.let { managedNoMatchedDpsNextCourtAppearance(it, mergeCreateCourtCase, createdAppearances) }
    tracking.createdCourtCasesMap[mergeCreateCourtCase.caseId] = RequestToRecord(mergeCreateCourtCase, createdCourtCase)
    tracking.eventsToEmit.add(EventMetadataCreator.courtCaseEventMetadata(createdCourtCase.prisonerId, createdCourtCase.caseUniqueIdentifier, EventType.COURT_CASE_INSERTED))
  }

  fun createAppearances(mergeCreateAppearances: List<MergeCreateCourtAppearance>, createdCourtCase: CourtCaseEntity, courtCaseReference: String?, tracking: PrisonerMergeDataTracking): Map<Long, CourtAppearanceEntity> {
    val nomisAppearanceOutcomeIds = mergeCreateAppearances.filter { appearance -> appearance.legacyData.nomisOutcomeCode != null }.map { appearance -> appearance.legacyData.nomisOutcomeCode!! }.distinct()
    val dpsAppearanceOutcomes = appearanceOutcomeRepository.findByNomisCodeIn(nomisAppearanceOutcomeIds).associate { entity -> entity.nomisCode to entity }
    val nomisChargeOutcomeIds = mergeCreateAppearances.flatMap { courtAppearance -> courtAppearance.charges }.filter { charge -> charge.legacyData.nomisOutcomeCode != null }.map { charge -> charge.legacyData.nomisOutcomeCode!! }.distinct()
    val dpsChargeOutcomes = chargeOutcomeRepository.findByNomisCodeIn(nomisChargeOutcomeIds).associate { entity -> entity.nomisCode to entity }
    val dpsSentenceTypes = getDpsSentenceTypesMap(mergeCreateAppearances)
    val legacySentenceTypes = getLegacySentenceTypesMap(mergeCreateAppearances)
    return mergeCreateAppearances.sortedBy { courtAppearance -> courtAppearance.appearanceDate }.associate { appearance -> appearance.eventId to createAppearance(appearance, createdCourtCase, courtCaseReference, tracking, PrisonerMergeReferenceData(dpsAppearanceOutcomes, dpsChargeOutcomes, dpsSentenceTypes, legacySentenceTypes)) }
  }

  fun createAppearance(mergeCreateCourtAppearance: MergeCreateCourtAppearance, createdCourtCase: CourtCaseEntity, courtCaseReference: String?, tracking: PrisonerMergeDataTracking, referenceData: PrisonerMergeReferenceData): CourtAppearanceEntity {
    val dpsAppearanceOutcome = mergeCreateCourtAppearance.legacyData.nomisOutcomeCode?.let { referenceData.dpsAppearanceOutcomes[it] }
    val createdAppearance = courtAppearanceRepository.save(CourtAppearanceEntity.from(mergeCreateCourtAppearance, dpsAppearanceOutcome, createdCourtCase, tracking.username, courtCaseReference))
    val charges = mergeCreateCourtAppearance.charges.map { charge ->
      createCharge(
        charge,
        tracking,
        referenceData,
        mergeCreateCourtAppearance.eventId,
        MergeHierarchyData(tracking.retainedPrisonerNumber, createdCourtCase.caseUniqueIdentifier, createdAppearance.appearanceUuid.toString()),
      )
    }
    charges.forEach { charge ->
      val appearanceChargeEntity = AppearanceChargeEntity(
        createdAppearance,
        charge,
        tracking.username,
        null,
      )
      createdAppearance.appearanceCharges.add(appearanceChargeEntity)
    }
    tracking.eventsToEmit.add(
      EventMetadataCreator.courtAppearanceEventMetadata(
        createdAppearance.courtCase.prisonerId,
        createdAppearance.courtCase.caseUniqueIdentifier,
        createdAppearance.appearanceUuid.toString(),
        EventType.COURT_APPEARANCE_INSERTED,
      ),
    )
    return createdAppearance
  }

  fun createCharge(mergeCreateCharge: MergeCreateCharge, tracking: PrisonerMergeDataTracking, referenceData: PrisonerMergeReferenceData, eventId: Long, mergeHierarchyData: MergeHierarchyData): ChargeEntity {
    val dpsChargeOutcome = mergeCreateCharge.legacyData.nomisOutcomeCode?.let { referenceData.dpsChargeOutcomes[it] }
    mergeCreateCharge.legacyData = dpsChargeOutcome?.let { mergeCreateCharge.legacyData.copy(nomisOutcomeCode = null, outcomeDescription = null, outcomeDispositionCode = null) } ?: mergeCreateCharge.legacyData
    val existingChangeRecords = tracking.createdChargesMap[mergeCreateCharge.chargeNOMISId] ?: mutableListOf()
    val existingCharge = existingChangeRecords.lastOrNull()?.second
    val toCreateCharge = if (existingCharge != null) {
      val chargeInAppearance = existingCharge.copyFrom(mergeCreateCharge, dpsChargeOutcome, tracking.username)
      if (existingCharge.isSame(chargeInAppearance, mergeCreateCharge.sentence != null)) existingCharge else chargeInAppearance
    } else {
      ChargeEntity.from(mergeCreateCharge, dpsChargeOutcome, tracking.username)
    }
    val createdCharge = chargeRepository.save(toCreateCharge)
    mergeHierarchyData.chargeId = createdCharge.chargeUuid.toString()
    mergeCreateCharge.sentence?.let { mergeSentence -> createdCharge.sentences.add(createSentence(mergeSentence, createdCharge, tracking, referenceData, mergeHierarchyData)) }
    existingChangeRecords.add(eventId to createdCharge)
    tracking.createdChargesMap[mergeCreateCharge.chargeNOMISId] = existingChangeRecords
    tracking.eventsToEmit.add(
      EventMetadataCreator.chargeEventMetadata(
        mergeHierarchyData.prisonerId,
        mergeHierarchyData.courtCaseId,
        null,
        createdCharge.chargeUuid.toString(),
        EventType.CHARGE_INSERTED,
      ),
    )
    return createdCharge
  }

  private fun getDpsSentenceTypesMap(mergeCreateAppearances: List<MergeCreateCourtAppearance>): Map<Pair<String, String?>, SentenceTypeEntity> {
    val (sentenceCalcTypes, sentenceCategories) = mergeCreateAppearances.flatMap { it.charges }.filter { charge -> charge.sentence != null && charge.sentence.legacyData.sentenceCalcType != null && charge.sentence.legacyData.sentenceCategory != null }.map { charge -> charge.sentence!!.legacyData.sentenceCalcType!! to charge.sentence.legacyData.sentenceCategory!! }.unzip()
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

  private fun getLegacySentenceTypesMap(mergeCreateAppearances: List<MergeCreateCourtAppearance>): Map<Pair<String, Int>, LegacySentenceTypeEntity> {
    val (sentenceCalcTypes, sentenceCategories) = mergeCreateAppearances.flatMap { it.charges }.filter { charge -> charge.sentence != null && charge.sentence.legacyData.sentenceCalcType != null && charge.sentence.legacyData.sentenceCategory != null && charge.sentence.legacyData.sentenceCategory!!.toIntOrNull() != null }.map { charge -> charge.sentence!!.legacyData.sentenceCalcType!! to charge.sentence.legacyData.sentenceCategory!!.toInt() }.unzip()
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

  fun createSentence(mergeCreateSentence: MergeCreateSentence, chargeEntity: ChargeEntity, tracking: PrisonerMergeDataTracking, referenceData: PrisonerMergeReferenceData, mergeHierarchyData: MergeHierarchyData): SentenceEntity {
    val dpsSentenceType = (mergeCreateSentence.legacyData.sentenceCalcType to mergeCreateSentence.legacyData.sentenceCategory).takeIf { (sentenceCalcType, sentenceCategory) -> sentenceCalcType != null && sentenceCategory != null }?.let { getDpsSentenceType(referenceData.dpsSentenceTypes, it) }
    val legacyData = mergeCreateSentence.legacyData
    mergeCreateSentence.legacyData = dpsSentenceType?.let { if (it.sentenceTypeUuid == LegacySentenceService.recallSentenceTypeBucketUuid) mergeCreateSentence.legacyData else mergeCreateSentence.legacyData.copy(sentenceCalcType = null, sentenceCategory = null, sentenceTypeDesc = null) } ?: mergeCreateSentence.legacyData

    val existingSentences = tracking.createdSentencesMap[mergeCreateSentence.sentenceId] ?: mutableListOf()
    val toCreateSentence = existingSentences.firstOrNull()?.let { existingSentence ->
      existingSentence.statusId = EntityStatus.MANY_CHARGES_DATA_FIX
      existingSentence.copyFrom(mergeCreateSentence, tracking.username, chargeEntity, dpsSentenceType)
    } ?: SentenceEntity.from(mergeCreateSentence, tracking.username, chargeEntity, dpsSentenceType)
    val createdSentence = sentenceRepository.save(toCreateSentence)
    existingSentences.add(createdSentence)

    if (dpsSentenceType?.sentenceTypeUuid == LegacySentenceService.recallSentenceTypeBucketUuid) {
      createRecall(mergeCreateSentence, createdSentence, tracking, referenceData, RecallSentenceLegacyData.from(legacyData))
    }

    createdSentence.periodLengths = mergeCreateSentence.periodLengths.map {
      val existingPeriodLengths = tracking.createdPeriodLengthMap[it.periodLengthId] ?: mutableListOf()
      val toCreatePeriodLength = existingPeriodLengths.firstOrNull()?.let { existingPeriodLength ->
        existingPeriodLength.statusId = EntityStatus.MANY_CHARGES_DATA_FIX
        val copiedPeriodLength = existingPeriodLength.copy()
        copiedPeriodLength.sentenceEntity = createdSentence
        copiedPeriodLength
      } ?: PeriodLengthEntity.from(it, dpsSentenceType?.nomisSentenceCalcType ?: mergeCreateSentence.legacyData.sentenceCalcType!!, serviceUserService.getUsername())
      val createdPeriodLength = periodLengthRepository.save(toCreatePeriodLength)
      createdPeriodLength.sentenceEntity = createdSentence
      existingPeriodLengths.add(createdPeriodLength)
      tracking.createdPeriodLengthMap[it.periodLengthId] = existingPeriodLengths
      tracking.eventsToEmit.add(
        EventMetadataCreator.periodLengthEventMetadata(
          prisonerId = mergeHierarchyData.prisonerId,
          courtCaseId = mergeHierarchyData.courtCaseId,
          courtAppearanceId = mergeHierarchyData.courtAppearanceId,
          chargeId = mergeHierarchyData.chargeId!!,
          sentenceId = createdSentence.sentenceUuid.toString(),
          periodLengthId = createdPeriodLength.periodLengthUuid.toString(),
          eventType = EventType.PERIOD_LENGTH_INSERTED,
        ),
      )
      createdPeriodLength
    }.toMutableSet()
    tracking.createdSentencesMap[mergeCreateSentence.sentenceId] = existingSentences
    tracking.eventsToEmit.add(
      EventMetadataCreator.sentenceEventMetadata(
        mergeHierarchyData.prisonerId,
        mergeHierarchyData.courtCaseId,
        mergeHierarchyData.chargeId!!,
        createdSentence.sentenceUuid.toString(),
        mergeHierarchyData.courtAppearanceId,
        EventType.SENTENCE_INSERTED,
      ),
    )
    return createdSentence
  }

  private fun createRecall(mergeCreateSentence: MergeCreateSentence, createdSentence: SentenceEntity, tracking: PrisonerMergeDataTracking, referenceData: PrisonerMergeReferenceData, recallSentenceLegacyData: RecallSentenceLegacyData) {
    val legacySentenceType = (mergeCreateSentence.legacyData.sentenceCalcType to mergeCreateSentence.legacyData.sentenceCategory)
      .takeIf { (sentenceCalcType, sentenceCategory) -> sentenceCalcType != null && sentenceCategory != null && sentenceCategory.toIntOrNull() != null }
      ?.let { referenceData.legacySentenceTypes[it.first to it.second!!.toInt()] }
    val defaultRecallType = recallTypeRepository.findOneByCode(RecallType.LR)!!
    val recall = recallRepository.save(RecallEntity.fromMerge(mergeCreateSentence, tracking.retainedPrisonerNumber, tracking.username, legacySentenceType?.recallType ?: defaultRecallType))
    tracking.eventsToEmit.add(
      EventMetadataCreator.recallEventMetadata(
        tracking.retainedPrisonerNumber,
        recall.recallUuid.toString(),
        listOf(createdSentence.sentenceUuid.toString()),
        emptyList(),
        null,
        EventType.RECALL_INSERTED,
      ),
    )
    recallSentenceRepository.save(RecallSentenceEntity.fromMerge(createdSentence, recall, tracking.username, recallSentenceLegacyData))
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  data class PrisonerMergeDataTracking(
    val retainedPrisonerNumber: String,
    val username: String,
    val editedCourtCases: MutableList<CourtCaseEntity> = mutableListOf(),
    val editedSentences: MutableList<SentenceEntity> = mutableListOf(),
    val createdCourtCasesMap: MutableMap<Long, RequestToRecord<MergeCreateCourtCase, CourtCaseEntity>> = HashMap(),
    val createdCourtAppearancesMap: MutableMap<Long, CourtAppearanceEntity> = HashMap(),
    val createdChargesMap: MutableMap<Long, MutableList<Pair<Long, ChargeEntity>>> = HashMap(),
    val createdSentencesMap: MutableMap<MergeSentenceId, MutableList<SentenceEntity>> = HashMap(),
    val createdPeriodLengthMap: MutableMap<NomisPeriodLengthId, MutableList<PeriodLengthEntity>> = HashMap(),
    val eventsToEmit: MutableSet<EventMetadata> = mutableSetOf(),
  )

  data class RequestToRecord<T, S>(
    val request: T,
    val record: S,
  )

  data class PrisonerMergeReferenceData(
    val dpsAppearanceOutcomes: Map<String, AppearanceOutcomeEntity>,
    val dpsChargeOutcomes: Map<String, ChargeOutcomeEntity>,
    val dpsSentenceTypes: Map<Pair<String, String?>, SentenceTypeEntity>,
    val legacySentenceTypes: Map<Pair<String, Int>, LegacySentenceTypeEntity>,
  )

  data class MergeHierarchyData(
    var prisonerId: String,
    var courtCaseId: String,
    var courtAppearanceId: String,
    var chargeId: String? = null,
  )
}
