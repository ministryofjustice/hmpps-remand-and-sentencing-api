package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

import jakarta.persistence.EntityNotFoundException
import jakarta.transaction.Transactional
import jakarta.transaction.Transactional.TxType
import org.jetbrains.annotations.VisibleForTesting
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateSentence
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.HasSentenceAfterOnOtherCourtAppearanceResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.Sentence
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.SentenceConsecutiveToDetailsResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.SentenceDetailsForConsecValidation
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.SentencesAfterOnOtherCourtAppearanceDetailsResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.EventMetadata
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.EventType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.RecordResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.util.EventMetadataCreator
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.ChargeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.PeriodLengthEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.RecallSentenceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.SentenceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.audit.RecallHistoryEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.audit.RecallSentenceHistoryEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.audit.SentenceHistoryEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityChangeStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.RecallSentenceRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.SentenceRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.SentenceTypeRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.audit.RecallHistoryRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.audit.RecallSentenceHistoryRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.audit.SentenceHistoryRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.service.LegacySentenceService
import java.time.ZonedDateTime
import java.util.UUID

@Service
class SentenceService(
  private val sentenceRepository: SentenceRepository,
  private val periodLengthService: PeriodLengthService,
  private val serviceUserService: ServiceUserService,
  private val sentenceTypeRepository: SentenceTypeRepository,
  private val sentenceHistoryRepository: SentenceHistoryRepository,
  private val fixManyChargesToSentenceService: FixManyChargesToSentenceService,
  private val recallSentenceRepository: RecallSentenceRepository,
  private val recallSentenceHistoryRepository: RecallSentenceHistoryRepository,
  private val recallHistoryRepository: RecallHistoryRepository,
) {

  @Transactional(TxType.REQUIRED)
  fun createSentence(sentence: CreateSentence, chargeEntity: ChargeEntity, sentencesCreated: MutableMap<UUID, SentenceEntity>, prisonerId: String, courtCaseId: String, courtAppearanceDateChanged: Boolean, courtAppearanceId: String): RecordResponse<SentenceEntity> {
    val existingSentence = getSentenceFromChargeOrUuid(chargeEntity, sentence.sentenceUuid)
    return if (existingSentence != null) updateSentenceEntity(existingSentence, sentence, chargeEntity, sentencesCreated, prisonerId, courtCaseId, courtAppearanceDateChanged, courtAppearanceId) else createSentenceEntity(sentence, chargeEntity, sentencesCreated, prisonerId, courtCaseId, courtAppearanceId)
  }

  private fun updateSentenceEntity(existingSentence: SentenceEntity, sentence: CreateSentence, chargeEntity: ChargeEntity, sentencesCreated: MutableMap<UUID, SentenceEntity>, prisonerId: String, courtCaseId: String, courtAppearanceDateChanged: Boolean, courtAppearanceId: String): RecordResponse<SentenceEntity> {
    val consecutiveToSentence = sentence.consecutiveToSentenceUuid?.let { sentencesCreated[it] } ?: sentence.consecutiveToSentenceUuid?.let { sentenceRepository.findFirstBySentenceUuidOrderByUpdatedAtDesc(it) }
    val sentenceType = sentence.sentenceTypeId?.let { sentenceTypeId -> sentenceTypeRepository.findBySentenceTypeUuid(sentenceTypeId) ?: throw EntityNotFoundException("No sentence type found at $sentenceTypeId") }
    val compareSentence = existingSentence.copyFrom(sentence, serviceUserService.getUsername(), chargeEntity, consecutiveToSentence, sentenceType)
    val activeRecord = existingSentence
    val eventsToEmit: MutableSet<EventMetadata> = mutableSetOf()
    var sentenceChangeStatus = if (courtAppearanceDateChanged) EntityChangeStatus.EDITED else EntityChangeStatus.NO_CHANGE
    if (!existingSentence.isSame(compareSentence)) {
      existingSentence.updateFrom(compareSentence)
      sentenceHistoryRepository.save(SentenceHistoryEntity.from(existingSentence))
      sentenceChangeStatus = EntityChangeStatus.EDITED
    }

    val newPeriodLengths = sentence.periodLengths.map { PeriodLengthEntity.from(it, serviceUserService.getUsername()) }

    val deleteResponse = periodLengthService.delete(
      newPeriodLengths,
      existingSentence.periodLengths,
      prisonerId,
      courtAppearanceId,
      courtCaseId,
      shouldGenerateEvents = true,
    )

    val updateResponse = periodLengthService.update(
      newPeriodLengths,
      existingSentence.periodLengths,
      prisonerId,
      courtAppearanceId,
      courtCaseId,
      shouldGenerateEvents = true,
    )

    val createResponse = periodLengthService.create(
      newPeriodLengths,
      existingSentence.periodLengths,
      prisonerId,
      { created -> created.sentenceEntity = existingSentence },
      courtAppearanceId,
      courtCaseId,
      shouldGenerateEvents = true,
    )

    val periodLengthChangeRecord = RecordResponse(
      EntityChangeStatus.NO_CHANGE,
      (deleteResponse.eventsToEmit + updateResponse.eventsToEmit + createResponse.eventsToEmit).toMutableSet(),
    )

    eventsToEmit.addAll(periodLengthChangeRecord.eventsToEmit)
    if (sentenceChangeStatus == EntityChangeStatus.EDITED) {
      eventsToEmit.add(
        EventMetadataCreator.sentenceEventMetadata(
          prisonerId,
          courtCaseId,
          chargeEntity.chargeUuid.toString(),
          activeRecord.sentenceUuid.toString(),
          courtAppearanceId,
          EventType.SENTENCE_UPDATED,
        ),
      )
    }
    sentencesCreated.put(sentence.sentenceUuid, activeRecord)
    return RecordResponse(activeRecord, eventsToEmit)
  }

  private fun createSentenceEntity(sentence: CreateSentence, chargeEntity: ChargeEntity, sentencesCreated: MutableMap<UUID, SentenceEntity>, prisonerId: String, courtCaseId: String, courtAppearanceId: String): RecordResponse<SentenceEntity> {
    val eventsToEmit = mutableSetOf<EventMetadata>()
    val consecutiveToSentence = sentence.consecutiveToSentenceUuid?.let { sentencesCreated[it] } ?: sentence.consecutiveToSentenceUuid?.let { sentenceRepository.findFirstBySentenceUuidOrderByUpdatedAtDesc(it) }
    val sentenceType = sentence.sentenceTypeId?.let { sentenceTypeId -> sentenceTypeRepository.findBySentenceTypeUuid(sentenceTypeId) ?: throw EntityNotFoundException("No sentence type found at $sentenceTypeId") }
    val createdSentence = sentenceRepository.save(SentenceEntity.from(sentence, serviceUserService.getUsername(), chargeEntity, consecutiveToSentence, sentenceType))
    sentenceHistoryRepository.save(SentenceHistoryEntity.from(createdSentence))

    val newPeriodLengths = sentence.periodLengths.map { PeriodLengthEntity.from(it, serviceUserService.getUsername()) }
    val periodLengthResponse = periodLengthService.create(
      newPeriodLengths,
      createdSentence.periodLengths,
      prisonerId,
      { created -> created.sentenceEntity = createdSentence },
      courtAppearanceId,
      courtCaseId,
      shouldGenerateEvents = true,
    )
    val sentenceEvent = EventMetadataCreator.sentenceEventMetadata(
      prisonerId,
      courtCaseId,
      chargeEntity.chargeUuid.toString(),
      createdSentence.sentenceUuid.toString(),
      courtAppearanceId,
      EventType.SENTENCE_INSERTED,
    )
    eventsToEmit.add(sentenceEvent)
    eventsToEmit.addAll(periodLengthResponse.eventsToEmit)
    sentencesCreated.put(sentence.sentenceUuid, createdSentence)
    return RecordResponse(createdSentence, eventsToEmit)
  }

  fun getSentenceFromChargeOrUuid(chargeEntity: ChargeEntity, sentenceUuid: UUID?): SentenceEntity? = chargeEntity.getActiveSentence() ?: sentenceUuid?.let { sentenceRepository.findFirstBySentenceUuidOrderByUpdatedAtDesc(sentenceUuid) }

  @Transactional(TxType.REQUIRED)
  fun findSentenceByUuid(sentenceUuid: UUID): Sentence? = sentenceRepository.findFirstBySentenceUuidOrderByUpdatedAtDesc(sentenceUuid)?.let { Sentence.from(it) }

  @Transactional(TxType.REQUIRED)
  fun deleteSentence(sentence: SentenceEntity, chargeEntity: ChargeEntity, prisonerId: String, courtCaseId: String, courtAppearanceId: String): RecordResponse<SentenceEntity> {
    val changeStatus = if (sentence.statusId == EntityStatus.DELETED) EntityChangeStatus.NO_CHANGE else EntityChangeStatus.DELETED
    sentence.delete(serviceUserService.getUsername())
    sentenceHistoryRepository.save(SentenceHistoryEntity.from(sentence))
    val eventsToEmit: MutableSet<EventMetadata> = mutableSetOf()
    if (changeStatus == EntityChangeStatus.DELETED) {
      eventsToEmit.add(
        EventMetadataCreator.sentenceEventMetadata(
          prisonerId,
          courtCaseId,
          chargeEntity.chargeUuid.toString(),
          sentence.sentenceUuid.toString(),
          courtAppearanceId,
          EventType.SENTENCE_DELETED,
        ),
      )
    }

    handleRecallsForDeletedSentence(sentence, eventsToEmit)

    sentence.periodLengths.forEach { it.delete(serviceUserService.getUsername()) }
    return RecordResponse(sentence, eventsToEmit)
  }

  private fun handleRecallsForDeletedSentence(
    sentence: SentenceEntity,
    eventsToEmit: MutableSet<EventMetadata>,
  ) {
    if (sentence.recallSentences.isNotEmpty()) {
      if (sentence.sentenceType?.sentenceTypeUuid == LegacySentenceService.recallSentenceTypeBucketUuid) {
        // delete recall as this will be the only sentence
        val onlyRecallSentence = sentence.recallSentences.first()
        deleteRecallWithOnlyOneSentence(onlyRecallSentence, eventsToEmit)
      } else {
        // update recalls with this sentence removed
        sentence.recallSentences.groupBy { it.recall }.forEach { (recall, recallSentences) ->
          if (recall.recallSentences.size == 1) {
            deleteRecallWithOnlyOneSentence(recallSentences.first(), eventsToEmit)
          } else {
            val recallHistory = recallHistoryRepository.save(RecallHistoryEntity.from(recall, EntityStatus.EDITED))
            recallSentenceHistoryRepository.saveAll(
              recallSentences.map {
                RecallSentenceHistoryEntity.from(
                  recallHistory,
                  it,
                )
              },
            )
            recall.updatedAt = ZonedDateTime.now()
            recall.updatedBy = serviceUserService.getUsername()
            recallSentences.forEach { recallSentenceRepository.delete(it) }
          }
        }
      }
      }
  }

  private fun deleteRecallWithOnlyOneSentence(
    onlyRecallSentence: RecallSentenceEntity,
    eventsToEmit: MutableSet<EventMetadata>,
  ) {
    val recallHistory =
      recallHistoryRepository.save(RecallHistoryEntity.from(onlyRecallSentence.recall, EntityStatus.DELETED))
    onlyRecallSentence.recall.statusId = EntityStatus.DELETED
    recallSentenceHistoryRepository.save(RecallSentenceHistoryEntity.from(recallHistory, onlyRecallSentence))
    recallSentenceRepository.delete(onlyRecallSentence)
    eventsToEmit.add(
      EventMetadataCreator.recallEventMetadata(
        onlyRecallSentence.recall.prisonerId,
        onlyRecallSentence.recall.recallUuid.toString(),
        onlyRecallSentence.recall.recallSentences.map { it.sentence.sentenceUuid.toString() }.distinct(),
        emptyList(),
        null,
        EventType.RECALL_DELETED,
      ),
    )
  }

  @Transactional
  fun findConsecutiveToSentenceDetails(sentenceUuids: List<UUID>): RecordResponse<SentenceConsecutiveToDetailsResponse> {
    val consecutiveToSentencesUuids = sentenceRepository.findConsecutiveToSentenceDetails(sentenceUuids).map { it.toRecordEventMetadata(it.sentenceUuid) }
    val eventsToEmit = fixManyChargesToSentenceService.fixSentencesBySentenceUuids(consecutiveToSentencesUuids)
    return RecordResponse(SentenceConsecutiveToDetailsResponse.from(sentenceRepository.findConsecutiveToSentenceDetails(sentenceUuids)), eventsToEmit)
  }

  fun hasSentencesAfterOnOtherCourtAppearance(sentenceUuids: List<UUID>): HasSentenceAfterOnOtherCourtAppearanceResponse {
    val count = sentenceRepository.countSentencesAfterOnOtherCourtAppearance(sentenceUuids)
    return HasSentenceAfterOnOtherCourtAppearanceResponse(count > 0)
  }

  fun sentencesAfterOnOtherCourtAppearanceDetails(sentenceUuids: List<UUID>): SentencesAfterOnOtherCourtAppearanceDetailsResponse = SentencesAfterOnOtherCourtAppearanceDetailsResponse.from(
    sentenceRepository.sentencesAfterOnOtherCourtAppearanceDetails(sentenceUuids),
  )

  fun moveSentencesToNewCharge(
    existingCharge: ChargeEntity,
    newChargeRecord: ChargeEntity,
    prisonerId: String,
    courtCaseId: String,
    courtAppearanceId: String,
  ): MutableSet<EventMetadata> {
    val existingSentences = existingCharge.sentences.filter { it.statusId != EntityStatus.DELETED }
    return existingSentences.map { existingSentence ->
      newChargeRecord.sentences.add(existingSentence)
      existingSentence.charge = newChargeRecord
      existingSentence.updatedBy = serviceUserService.getUsername()
      existingSentence.updatedAt = ZonedDateTime.now()
      sentenceHistoryRepository.save(SentenceHistoryEntity.from(existingSentence))
      existingCharge.sentences.remove(existingSentence)
      EventMetadataCreator.sentenceEventMetadata(
        prisonerId,
        courtCaseId,
        newChargeRecord.chargeUuid.toString(),
        existingSentence.sentenceUuid.toString(),
        courtAppearanceId,
        EventType.SENTENCE_UPDATED,
      )
    }.toMutableSet()
  }

  fun isTargetAlreadyInConsecutiveChain(
    prisonerId: String,
    appearanceUUID: UUID,
    sourceSentenceUUID: UUID,
    targetSentenceUUID: UUID,
    sentencesOnAppearanceFromUI: List<SentenceDetailsForConsecValidation>,
  ): Boolean {
    if (sourceSentenceUUID == targetSentenceUUID) return true

    // The sentence hasnt been added to the court appearance (yet)
    // i.e. still in the 'Add sentence' phase - therefore it cannot be part on any consec chain
    if (sentencesOnAppearanceFromUI.none { sentence -> sentence.sentenceUuid == sourceSentenceUUID }) {
      return false
    }

    sentencesOnAppearanceFromUI.forEach { log.info(it.toString()) }
    // build upstream chains for the current appearance, starting from the sourceSentence
    val upstreamChains = getUpstreamChains(sentencesOnAppearanceFromUI, sourceSentenceUUID)

    // If target exists anywhere in those UI upstream chains then return true
    if (upstreamChains.any { chain -> chain.any { it.sentenceUuid == targetSentenceUUID } }) {
      log.info("targetSentenceUUID $targetSentenceUUID exists in upstream chain passed from UI")
      return true
    }

    // source UUID's also to be checked against DB
    val sourceUuids = upstreamChains
      .flatten()
      .map { it.sentenceUuid }
      .distinct()

    if (sourceUuids.isEmpty()) return false

    // DB query to check if target appear downstream of the sources, any chains in the current appearance are omitted due tio UI check above
    log.info("Checking target descendant outside current appearance")
    log.info("  sourceUuids={}", sourceUuids)
    log.info("  targetSentenceId={}", targetSentenceUUID)
    log.info("  prisonerId={}", prisonerId)
    log.info("  currentAppearanceId={}", appearanceUUID)

    for (source in sourceUuids) {
      if (sentenceRepository.isTargetDescendantFromSource(
          sourceUuid = source,
          targetSentenceId = targetSentenceUUID,
          prisonerId = prisonerId,
          currentAppearanceId = appearanceUUID,
        )
      ) {
        return true
      }
    }
    return false
  }

  @VisibleForTesting
  fun getUpstreamChains(
    sentencesOnAppearanceFromUI: List<SentenceDetailsForConsecValidation>,
    sourceSentenceUUID: UUID,
  ): MutableList<MutableList<SentenceDetailsForConsecValidation>> {
    val childrenByParent: Map<UUID?, List<SentenceDetailsForConsecValidation>> = sentencesOnAppearanceFromUI.groupBy { it.consecutiveToSentenceUuid }

    val source = sentencesOnAppearanceFromUI.first { it.sentenceUuid == sourceSentenceUUID }
    val results = mutableListOf<MutableList<SentenceDetailsForConsecValidation>>()

    getChain(
      current = source,
      childrenByParent = childrenByParent,
      currentChain = mutableListOf(source),
      results = results,
    )
    return results
  }

  private fun getChain(
    current: SentenceDetailsForConsecValidation,
    childrenByParent: Map<UUID?, List<SentenceDetailsForConsecValidation>>,
    currentChain: MutableList<SentenceDetailsForConsecValidation>,
    results: MutableList<MutableList<SentenceDetailsForConsecValidation>>,
  ) {
    val children = childrenByParent[current.sentenceUuid].orEmpty()
    if (children.isEmpty()) {
      results.add(currentChain.toMutableList())
      return
    }

    for (child in children) {
      currentChain.add(child)
      getChain(child, childrenByParent, currentChain, results)
      currentChain.removeAt(currentChain.lastIndex)
    }
  }
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
