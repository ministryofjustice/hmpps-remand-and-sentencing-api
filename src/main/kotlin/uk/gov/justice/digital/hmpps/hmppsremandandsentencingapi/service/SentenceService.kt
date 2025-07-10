package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

import jakarta.persistence.EntityNotFoundException
import jakarta.transaction.Transactional
import jakarta.transaction.Transactional.TxType
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateSentence
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.HasSentenceAfterOnOtherCourtAppearanceResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.Sentence
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.SentenceConsecutiveToDetailsResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.SentencesAfterOnOtherCourtAppearanceDetailsResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.EventMetadata
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.EventType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.RecordResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.util.EventMetadataCreator
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.ChargeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.PeriodLengthEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.SentenceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.audit.SentenceHistoryEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityChangeStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.SentenceRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.SentenceTypeRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.audit.SentenceHistoryRepository
import java.time.ZonedDateTime
import java.util.UUID

@Service
class SentenceService(private val sentenceRepository: SentenceRepository, private val periodLengthService: PeriodLengthService, private val serviceUserService: ServiceUserService, private val sentenceTypeRepository: SentenceTypeRepository, private val sentenceHistoryRepository: SentenceHistoryRepository, private val fixManyChargesToSentenceService: FixManyChargesToSentenceService) {

  @Transactional(TxType.REQUIRED)
  fun createSentence(sentence: CreateSentence, chargeEntity: ChargeEntity, sentencesCreated: MutableMap<String, SentenceEntity>, prisonerId: String, courtCaseId: String, courtAppearanceDateChanged: Boolean, courtAppearanceId: String): RecordResponse<SentenceEntity> {
    val existingSentence = getSentenceFromChargeOrUuid(chargeEntity, sentence.sentenceUuid)
    return if (existingSentence != null) updateSentenceEntity(existingSentence, sentence, chargeEntity, sentencesCreated, prisonerId, courtCaseId, courtAppearanceDateChanged, courtAppearanceId) else createSentenceEntity(sentence, chargeEntity, sentencesCreated, prisonerId, courtCaseId, courtAppearanceId)
  }

  private fun updateSentenceEntity(existingSentence: SentenceEntity, sentence: CreateSentence, chargeEntity: ChargeEntity, sentencesCreated: MutableMap<String, SentenceEntity>, prisonerId: String, courtCaseId: String, courtAppearanceDateChanged: Boolean, courtAppearanceId: String): RecordResponse<SentenceEntity> {
    val consecutiveToSentence = sentence.consecutiveToSentenceReference?.let { sentencesCreated[it] } ?: sentence.consecutiveToSentenceUuid?.let { sentenceRepository.findFirstBySentenceUuidOrderByUpdatedAtDesc(it) }
    val sentenceType = sentenceTypeRepository.findBySentenceTypeUuid(sentence.sentenceTypeId) ?: throw EntityNotFoundException("No sentence type found at ${sentence.sentenceTypeId}")
    val compareSentence = existingSentence.copyFrom(sentence, serviceUserService.getUsername(), chargeEntity, consecutiveToSentence, sentenceType)
    var activeRecord = existingSentence
    val eventsToEmit: MutableSet<EventMetadata> = mutableSetOf()
    var sentenceChangeStatus = if (courtAppearanceDateChanged) EntityChangeStatus.EDITED else EntityChangeStatus.NO_CHANGE
    if (!existingSentence.isSame(compareSentence)) {
      existingSentence.updateFrom(compareSentence)
      sentenceHistoryRepository.save(SentenceHistoryEntity.from(existingSentence))
      sentenceChangeStatus = EntityChangeStatus.EDITED
    }

    val periodLengthChangeRecord = periodLengthService.upsert(
      createPeriodLengthEntities = sentence.periodLengths.map { PeriodLengthEntity.from(it, serviceUserService.getUsername()) },
      existingPeriodLengths = existingSentence.periodLengths,
      prisonerId = prisonerId,
      onCreateConsumer = { toCreatePeriodLength ->
        toCreatePeriodLength.sentenceEntity = existingSentence
      },
      courtAppearanceId = courtAppearanceId,
      courtCaseId = courtCaseId,
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
    sentencesCreated.put(sentence.sentenceReference, activeRecord)
    return RecordResponse(activeRecord, eventsToEmit)
  }

  private fun createSentenceEntity(sentence: CreateSentence, chargeEntity: ChargeEntity, sentencesCreated: MutableMap<String, SentenceEntity>, prisonerId: String, courtCaseId: String, courtAppearanceId: String): RecordResponse<SentenceEntity> {
    val eventsToEmit = mutableSetOf<EventMetadata>()
    val consecutiveToSentence = sentence.consecutiveToSentenceReference?.let { sentencesCreated[it] } ?: sentence.consecutiveToSentenceUuid?.let { sentenceRepository.findFirstBySentenceUuidOrderByUpdatedAtDesc(it) }
    val sentenceType = sentenceTypeRepository.findBySentenceTypeUuid(sentence.sentenceTypeId) ?: throw EntityNotFoundException("No sentence type found at ${sentence.sentenceTypeId}")
    val createdSentence = sentenceRepository.save(SentenceEntity.from(sentence, serviceUserService.getUsername(), chargeEntity, consecutiveToSentence, sentenceType))
    sentenceHistoryRepository.save(SentenceHistoryEntity.from(createdSentence))

    val periodLengthResponse = periodLengthService.upsert(
      createPeriodLengthEntities = sentence.periodLengths.map {
        PeriodLengthEntity.from(
          it,
          serviceUserService.getUsername(),
        )
      },
      existingPeriodLengths = createdSentence.periodLengths,
      prisonerId = prisonerId,
      onCreateConsumer = { toCreatePeriodLength ->
        toCreatePeriodLength.sentenceEntity = createdSentence
      },
      courtAppearanceId = courtAppearanceId,
      courtCaseId = courtCaseId,
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
    sentencesCreated.put(sentence.sentenceReference, createdSentence)
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

    sentence.periodLengths.forEach { it.delete(serviceUserService.getUsername()) }
    return RecordResponse(sentence, eventsToEmit)
  }

  @Transactional
  fun findConsecutiveToSentenceDetails(sentenceUuids: List<UUID>): RecordResponse<SentenceConsecutiveToDetailsResponse> {
    val consecutiveToSentences = sentenceRepository.findConsecutiveToSentenceDetails(sentenceUuids)
    val eventsToEmit = fixManyChargesToSentenceService.fixSentences(consecutiveToSentences.map { it.toRecordEventMetadata(it.sentence) })
    return RecordResponse(SentenceConsecutiveToDetailsResponse.from(consecutiveToSentences), eventsToEmit)
  }

  fun hasSentencesAfterOnOtherCourtAppearance(sentenceUuid: UUID): HasSentenceAfterOnOtherCourtAppearanceResponse {
    val count = sentenceRepository.countSentencesAfterOnOtherCourtAppearance(sentenceUuid)
    return HasSentenceAfterOnOtherCourtAppearanceResponse(count > 0)
  }

  fun sentencesAfterOnOtherCourtAppearanceDetails(sentenceUuid: UUID): SentencesAfterOnOtherCourtAppearanceDetailsResponse = SentencesAfterOnOtherCourtAppearanceDetailsResponse.from(
    sentenceRepository.sentencesAfterOnOtherCourtAppearanceDetails(sentenceUuid),
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
}
