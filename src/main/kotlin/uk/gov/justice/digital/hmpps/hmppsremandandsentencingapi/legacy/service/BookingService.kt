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
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.booking.BookingCreateCharge
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.booking.BookingCreateChargeResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.booking.BookingCreateCourtAppearance
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.booking.BookingCreateCourtAppearanceResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.booking.BookingCreateCourtCase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.booking.BookingCreateCourtCaseResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.booking.BookingCreateCourtCases
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.booking.BookingCreateCourtCasesResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.booking.BookingCreatePeriodLengthResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.booking.BookingCreateSentence
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.booking.BookingCreateSentenceResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.booking.BookingSentenceId
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.ServiceUserService

@Service
class BookingService(
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
  fun create(bookingCreateCourtCases: BookingCreateCourtCases): RecordResponse<BookingCreateCourtCasesResponse> {
    val tracking = BookingDataTracking(
      bookingCreateCourtCases.prisonerId,
      serviceUserService.getUsername(),
    )
    bookingCreateCourtCases.courtCases.forEach { bookingCreateCourtCase ->
      createCourtCase(bookingCreateCourtCase, tracking)
    }
    linkMergedCases(bookingCreateCourtCases, tracking)
    linkConsecutiveToSentences(bookingCreateCourtCases, tracking)
    auditCreatedRecords(
      tracking.createdCourtAppearancesMap.values,
      tracking.createdChargesMap.values.flatMap { it.map { it.second } }.distinct(),
      tracking.createdSentencesMap.values.flatMap { it }.distinct(),
      tracking.createdPeriodLengthMap.values.flatMap { it }.distinct(),
    )

    return RecordResponse(
      BookingCreateCourtCasesResponse(
        tracking.createdCourtCasesMap.map { (caseId, createdCourtCase) -> BookingCreateCourtCaseResponse(createdCourtCase.record.caseUniqueIdentifier, caseId) },
        tracking.createdCourtAppearancesMap.map { (eventId, createdAppearance) -> BookingCreateCourtAppearanceResponse(createdAppearance.appearanceUuid, eventId) },
        tracking.createdChargesMap.map { (chargeNOMISId, createdCharges) -> BookingCreateChargeResponse(createdCharges.first().second.chargeUuid, chargeNOMISId) },
        tracking.createdSentencesMap.map { (id, createdSentences) -> BookingCreateSentenceResponse(createdSentences.first().sentenceUuid, id) },
        tracking.createdPeriodLengthMap.map { (id, createdPeriodLengths) -> BookingCreatePeriodLengthResponse(createdPeriodLengths.first().periodLengthUuid, id) },
      ),
      tracking.eventsToEmit,
    )
  }

  fun manageMatchedDpsNextCourtAppearances(bookingCreateCourtCase: BookingCreateCourtCase, createdAppearances: Map<Long, CourtAppearanceEntity>) {
    val nomisAppearances = bookingCreateCourtCase.appearances.associateBy { appearance -> appearance.eventId }
    val matchedNomisAppearances = bookingCreateCourtCase.appearances.filter { appearance -> appearance.legacyData.nextEventDateTime != null }.map { appearance ->
      appearance.eventId to bookingCreateCourtCase.appearances.firstOrNull { potentialAppearance ->
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

  fun managedNoMatchedDpsNextCourtAppearance(latestCourtAppearance: CourtAppearanceEntity, bookingCreateCourtCase: BookingCreateCourtCase, createdAppearances: Map<Long, CourtAppearanceEntity>) {
    if (latestCourtAppearance.nextCourtAppearance == null && createdAppearances.values.any { it.statusId == EntityStatus.FUTURE }) {
      val (nextFutureDatedEventId, nextFutureDatedAppearance) = createdAppearances.filter { (_, courtAppearanceEntity) -> courtAppearanceEntity.statusId == EntityStatus.FUTURE }.minBy { (_, courtAppearanceEntity) -> courtAppearanceEntity.appearanceDate }
      val nomisNextFutureDatedAppearance = bookingCreateCourtCase.appearances.first { it.eventId == nextFutureDatedEventId }
      val nextAppearanceType = appearanceTypeRepository.findByAppearanceTypeUuid(nomisNextFutureDatedAppearance.appearanceTypeUuid)!!
      latestCourtAppearance.nextCourtAppearance = nextCourtAppearanceRepository.save(
        NextCourtAppearanceEntity.from(nextFutureDatedAppearance, nextAppearanceType),
      )
    }
  }

  fun linkMergedCases(bookingCreateCourtCases: BookingCreateCourtCases, tracking: BookingDataTracking) {
    val targetCourtCases = bookingCreateCourtCases.courtCases.filter { it.appearances.flatMap { it.charges }.any { it.mergedFromCaseId != null } }
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

  fun linkConsecutiveToSentences(bookingCreateCourtCases: BookingCreateCourtCases, tracking: BookingDataTracking) {
    bookingCreateCourtCases.courtCases.flatMap { it.appearances }.flatMap { it.charges }.filter { it.sentence?.consecutiveToSentenceId != null }
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

  fun createCourtCase(bookingCreateCourtCase: BookingCreateCourtCase, tracking: BookingDataTracking) {
    val createdCourtCase = courtCaseRepository.save(CourtCaseEntity.from(bookingCreateCourtCase, tracking.createdByUsername, tracking.prisonerId))
    val latestCourtCaseReference = bookingCreateCourtCase.courtCaseLegacyData.caseReferences.maxByOrNull { caseReferenceLegacyData -> caseReferenceLegacyData.updatedDate }?.offenderCaseReference
    val createdAppearances = createAppearances(bookingCreateCourtCase.appearances, createdCourtCase, latestCourtCaseReference, tracking)
    tracking.createdCourtAppearancesMap.putAll(createdAppearances)
    manageMatchedDpsNextCourtAppearances(bookingCreateCourtCase, createdAppearances)
    val latestCourtAppearance = createdAppearances.values.filter { courtAppearanceEntity -> courtAppearanceEntity.statusId == EntityStatus.DUPLICATE }.maxByOrNull { courtAppearanceEntity -> courtAppearanceEntity.appearanceDate }
    createdCourtCase.latestCourtAppearance = latestCourtAppearance
    latestCourtAppearance?.let { managedNoMatchedDpsNextCourtAppearance(it, bookingCreateCourtCase, createdAppearances) }
    tracking.createdCourtCasesMap[bookingCreateCourtCase.caseId] =
      RequestToRecord(bookingCreateCourtCase, createdCourtCase)
    tracking.eventsToEmit.add(EventMetadataCreator.courtCaseEventMetadata(createdCourtCase.prisonerId, createdCourtCase.caseUniqueIdentifier, EventType.COURT_CASE_INSERTED))
  }

  fun createAppearances(bookingCreateAppearances: List<BookingCreateCourtAppearance>, createdCourtCase: CourtCaseEntity, courtCaseReference: String?, tracking: BookingDataTracking): Map<Long, CourtAppearanceEntity> {
    val nomisAppearanceOutcomeIds = bookingCreateAppearances.filter { appearance -> appearance.legacyData.nomisOutcomeCode != null }.map { appearance -> appearance.legacyData.nomisOutcomeCode!! }.distinct()
    val dpsAppearanceOutcomes = appearanceOutcomeRepository.findByNomisCodeIn(nomisAppearanceOutcomeIds).associate { entity -> entity.nomisCode to entity }
    val nomisChargeOutcomeIds = bookingCreateAppearances.flatMap { courtAppearance -> courtAppearance.charges }.filter { charge -> charge.legacyData.nomisOutcomeCode != null }.map { charge -> charge.legacyData.nomisOutcomeCode!! }.distinct()
    val dpsChargeOutcomes = chargeOutcomeRepository.findByNomisCodeIn(nomisChargeOutcomeIds)
      .associateBy { entity -> entity.nomisCode }
    val dpsSentenceTypes = getDpsSentenceTypesMap(bookingCreateAppearances)
    val legacySentenceTypes = getLegacySentenceTypesMap(bookingCreateAppearances)
    return bookingCreateAppearances.sortedBy { courtAppearance -> courtAppearance.appearanceDate }.associate { appearance -> appearance.eventId to createAppearance(appearance, createdCourtCase, courtCaseReference, tracking, BookingReferenceData(dpsAppearanceOutcomes, dpsChargeOutcomes, dpsSentenceTypes, legacySentenceTypes)) }
  }

  fun createAppearance(bookingCreateCourtAppearance: BookingCreateCourtAppearance, createdCourtCase: CourtCaseEntity, courtCaseReference: String?, tracking: BookingDataTracking, referenceData: BookingReferenceData): CourtAppearanceEntity {
    val dpsAppearanceOutcome = bookingCreateCourtAppearance.legacyData.nomisOutcomeCode?.let { referenceData.dpsAppearanceOutcomes[it] }
    val createdAppearance = courtAppearanceRepository.save(CourtAppearanceEntity.from(bookingCreateCourtAppearance, dpsAppearanceOutcome, createdCourtCase, tracking.createdByUsername, courtCaseReference))
    val charges = bookingCreateCourtAppearance.charges.map { charge ->
      createCharge(
        charge,
        tracking,
        referenceData,
        bookingCreateCourtAppearance.eventId,
        BookingHierarchyData(tracking.prisonerId, createdCourtCase.caseUniqueIdentifier, createdAppearance.appearanceUuid.toString()),
      )
    }
    charges.forEach { charge ->
      val appearanceChargeEntity = AppearanceChargeEntity(
        createdAppearance,
        charge,
        tracking.createdByUsername,
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

  fun createCharge(bookingCreateCharge: BookingCreateCharge, tracking: BookingDataTracking, referenceData: BookingReferenceData, eventId: Long, bookingHierarchyData: BookingHierarchyData): ChargeEntity {
    val dpsChargeOutcome = bookingCreateCharge.legacyData.nomisOutcomeCode?.let { referenceData.dpsChargeOutcomes[it] }
    bookingCreateCharge.legacyData = dpsChargeOutcome?.let { bookingCreateCharge.legacyData.copy(nomisOutcomeCode = null, outcomeDescription = null, outcomeDispositionCode = null) } ?: bookingCreateCharge.legacyData
    val existingChangeRecords = tracking.createdChargesMap[bookingCreateCharge.chargeNOMISId] ?: mutableListOf()
    val existingCharge = existingChangeRecords.lastOrNull()?.second
    val toCreateCharge = if (existingCharge != null) {
      val chargeInAppearance = existingCharge.copyFrom(bookingCreateCharge, dpsChargeOutcome, tracking.createdByUsername)
      if (existingCharge.isSame(chargeInAppearance, bookingCreateCharge.sentence != null)) existingCharge else chargeInAppearance
    } else {
      ChargeEntity.from(bookingCreateCharge, dpsChargeOutcome, tracking.createdByUsername)
    }
    val createdCharge = chargeRepository.save(toCreateCharge)
    bookingHierarchyData.chargeId = createdCharge.chargeUuid.toString()
    bookingCreateCharge.sentence?.let { bookingSentence -> createdCharge.sentences.add(createSentence(bookingSentence, createdCharge, tracking, referenceData, bookingHierarchyData)) }
    existingChangeRecords.add(eventId to createdCharge)
    tracking.createdChargesMap[bookingCreateCharge.chargeNOMISId] = existingChangeRecords
    tracking.eventsToEmit.add(
      EventMetadataCreator.chargeEventMetadata(
        bookingHierarchyData.prisonerId,
        bookingHierarchyData.courtCaseId,
        null,
        createdCharge.chargeUuid.toString(),
        EventType.CHARGE_INSERTED,
      ),
    )
    return createdCharge
  }

  private fun getDpsSentenceTypesMap(bookingCreateAppearances: List<BookingCreateCourtAppearance>): Map<Pair<String, String?>, SentenceTypeEntity> {
    val (sentenceCalcTypes, sentenceCategories) = bookingCreateAppearances.flatMap { it.charges }.filter { charge -> charge.sentence != null && charge.sentence.legacyData.sentenceCalcType != null && charge.sentence.legacyData.sentenceCategory != null }.map { charge -> charge.sentence!!.legacyData.sentenceCalcType!! to charge.sentence.legacyData.sentenceCategory!! }.unzip()
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

  private fun getLegacySentenceTypesMap(bookingCreateAppearances: List<BookingCreateCourtAppearance>): Map<Pair<String, Int>, LegacySentenceTypeEntity> {
    val (sentenceCalcTypes, sentenceCategories) = bookingCreateAppearances.flatMap { it.charges }.filter { charge -> charge.sentence != null && charge.sentence.legacyData.sentenceCalcType != null && charge.sentence.legacyData.sentenceCategory != null && charge.sentence.legacyData.sentenceCategory!!.toIntOrNull() != null }.map { charge -> charge.sentence!!.legacyData.sentenceCalcType!! to charge.sentence.legacyData.sentenceCategory!!.toInt() }.unzip()
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

  fun createSentence(bookingCreateSentence: BookingCreateSentence, chargeEntity: ChargeEntity, tracking: BookingDataTracking, referenceData: BookingReferenceData, bookingHierarchyData: BookingHierarchyData): SentenceEntity {
    val dpsSentenceType = (bookingCreateSentence.legacyData.sentenceCalcType to bookingCreateSentence.legacyData.sentenceCategory).takeIf { (sentenceCalcType, sentenceCategory) -> sentenceCalcType != null && sentenceCategory != null }?.let { getDpsSentenceType(referenceData.dpsSentenceTypes, it) }
    val legacyData = bookingCreateSentence.legacyData
    bookingCreateSentence.legacyData = dpsSentenceType?.let { if (it.sentenceTypeUuid == LegacySentenceService.recallSentenceTypeBucketUuid) bookingCreateSentence.legacyData else bookingCreateSentence.legacyData.copy(sentenceCalcType = null, sentenceCategory = null, sentenceTypeDesc = null) } ?: bookingCreateSentence.legacyData

    val existingSentences = tracking.createdSentencesMap[bookingCreateSentence.sentenceId] ?: mutableListOf()
    val toCreateSentence = existingSentences.firstOrNull()?.let { existingSentence ->
      existingSentence.statusId = EntityStatus.MANY_CHARGES_DATA_FIX
      existingSentence.copyFrom(bookingCreateSentence, tracking.createdByUsername, chargeEntity, dpsSentenceType)
    } ?: SentenceEntity.from(bookingCreateSentence, tracking.createdByUsername, chargeEntity, dpsSentenceType)
    val createdSentence = sentenceRepository.save(toCreateSentence)
    existingSentences.add(createdSentence)

    if (dpsSentenceType?.sentenceTypeUuid == LegacySentenceService.recallSentenceTypeBucketUuid) {
      createRecall(bookingCreateSentence, createdSentence, tracking, referenceData, RecallSentenceLegacyData.from(legacyData), bookingHierarchyData)
    }

    createdSentence.periodLengths = bookingCreateSentence.periodLengths.map {
      val existingPeriodLengths = tracking.createdPeriodLengthMap[it.periodLengthId] ?: mutableListOf()
      val toCreatePeriodLength = existingPeriodLengths.firstOrNull()?.let { existingPeriodLength ->
        existingPeriodLength.statusId = EntityStatus.MANY_CHARGES_DATA_FIX
        val copiedPeriodLength = existingPeriodLength.copy()
        copiedPeriodLength.sentenceEntity = createdSentence
        copiedPeriodLength
      } ?: PeriodLengthEntity.from(it, dpsSentenceType?.nomisSentenceCalcType ?: bookingCreateSentence.legacyData.sentenceCalcType!!, serviceUserService.getUsername())
      val createdPeriodLength = periodLengthRepository.save(toCreatePeriodLength)
      createdPeriodLength.sentenceEntity = createdSentence
      existingPeriodLengths.add(createdPeriodLength)
      tracking.createdPeriodLengthMap[it.periodLengthId] = existingPeriodLengths
      tracking.eventsToEmit.add(
        EventMetadataCreator.periodLengthEventMetadata(
          prisonerId = bookingHierarchyData.prisonerId,
          courtCaseId = bookingHierarchyData.courtCaseId,
          courtAppearanceId = bookingHierarchyData.courtAppearanceId,
          chargeId = bookingHierarchyData.chargeId!!,
          sentenceId = createdSentence.sentenceUuid.toString(),
          periodLengthId = createdPeriodLength.periodLengthUuid.toString(),
          eventType = EventType.PERIOD_LENGTH_INSERTED,
        ),
      )
      createdPeriodLength
    }.toMutableSet()
    tracking.createdSentencesMap[bookingCreateSentence.sentenceId] = existingSentences
    tracking.eventsToEmit.add(
      EventMetadataCreator.sentenceEventMetadata(
        tracking.prisonerId,
        bookingHierarchyData.courtCaseId,
        bookingHierarchyData.chargeId!!,
        createdSentence.sentenceUuid.toString(),
        bookingHierarchyData.courtAppearanceId,
        EventType.SENTENCE_INSERTED,
      ),
    )
    return createdSentence
  }

  private fun createRecall(bookingCreateSentence: BookingCreateSentence, createdSentence: SentenceEntity, tracking: BookingDataTracking, referenceData: BookingReferenceData, recallSentenceLegacyData: RecallSentenceLegacyData, bookingHierarchyData: BookingHierarchyData) {
    val legacySentenceType = (bookingCreateSentence.legacyData.sentenceCalcType to bookingCreateSentence.legacyData.sentenceCategory)
      .takeIf { (sentenceCalcType, sentenceCategory) -> sentenceCalcType != null && sentenceCategory != null && sentenceCategory.toIntOrNull() != null }
      ?.let { referenceData.legacySentenceTypes[it.first to it.second!!.toInt()] }
    val defaultRecallType = recallTypeRepository.findOneByCode(RecallType.LR)!!
    val recall = recallRepository.save(RecallEntity.fromBooking(bookingCreateSentence, tracking.prisonerId, tracking.createdByUsername, legacySentenceType?.recallType ?: defaultRecallType))
    tracking.eventsToEmit.add(
      EventMetadataCreator.recallEventMetadata(
        bookingHierarchyData.prisonerId,
        recall.recallUuid.toString(),
        listOf(createdSentence.sentenceUuid.toString()),
        emptyList(),
        null,
        EventType.RECALL_INSERTED,
      ),
    )
    recallSentenceRepository.save(RecallSentenceEntity.fromBooking(createdSentence, recall, tracking.createdByUsername, recallSentenceLegacyData))
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  data class BookingDataTracking(
    val prisonerId: String,
    val createdByUsername: String,
    val createdCourtCasesMap: MutableMap<Long, RequestToRecord<BookingCreateCourtCase, CourtCaseEntity>> = HashMap(),
    val createdCourtAppearancesMap: MutableMap<Long, CourtAppearanceEntity> = HashMap(),
    val createdChargesMap: MutableMap<Long, MutableList<Pair<Long, ChargeEntity>>> = HashMap(),
    val createdSentencesMap: MutableMap<BookingSentenceId, MutableList<SentenceEntity>> = HashMap(),
    val createdPeriodLengthMap: MutableMap<NomisPeriodLengthId, MutableList<PeriodLengthEntity>> = HashMap(),
    val eventsToEmit: MutableSet<EventMetadata> = mutableSetOf(),
  )

  data class RequestToRecord<T, S>(
    val request: T,
    val record: S,
  )

  data class BookingReferenceData(
    val dpsAppearanceOutcomes: Map<String, AppearanceOutcomeEntity>,
    val dpsChargeOutcomes: Map<String, ChargeOutcomeEntity>,
    val dpsSentenceTypes: Map<Pair<String, String?>, SentenceTypeEntity>,
    val legacySentenceTypes: Map<Pair<String, Int>, LegacySentenceTypeEntity>,
  )

  data class BookingHierarchyData(
    var prisonerId: String,
    var courtCaseId: String,
    var courtAppearanceId: String,
    var chargeId: String? = null,
  )
}
