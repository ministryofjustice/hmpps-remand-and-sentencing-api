package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

import jakarta.persistence.EntityNotFoundException
import jakarta.transaction.Transactional
import jakarta.transaction.Transactional.TxType
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateSentence
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.Sentence
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
import java.util.UUID

@Service
class SentenceService(private val sentenceRepository: SentenceRepository, private val periodLengthService: PeriodLengthService, private val serviceUserService: ServiceUserService, private val sentenceTypeRepository: SentenceTypeRepository, private val sentenceHistoryRepository: SentenceHistoryRepository) {

  @Transactional(TxType.REQUIRED)
  fun createSentence(sentence: CreateSentence, chargeEntity: ChargeEntity, sentencesCreated: Map<String, SentenceEntity>, prisonerId: String, courtCaseId: String, courtAppearanceDateChanged: Boolean, courtAppearanceId: String): RecordResponse<SentenceEntity> {
    val existingSentence = getSentenceFromChargeOrUuid(chargeEntity, sentence.sentenceUuid)
    return if (existingSentence != null) updateSentenceEntity(existingSentence, sentence, chargeEntity, sentencesCreated, prisonerId, courtCaseId, courtAppearanceDateChanged, courtAppearanceId) else createSentenceEntity(sentence, chargeEntity, sentencesCreated, prisonerId, courtCaseId, courtAppearanceId)
  }

  private fun updateSentenceEntity(existingSentence: SentenceEntity, sentence: CreateSentence, chargeEntity: ChargeEntity, sentencesCreated: Map<String, SentenceEntity>, prisonerId: String, courtCaseId: String, courtAppearanceDateChanged: Boolean, courtAppearanceId: String): RecordResponse<SentenceEntity> {
    println("Calling from updateSentenceEntity")
    val consecutiveToSentence = sentence.consecutiveToChargeNumber?.let { sentencesCreated[it] } ?: sentence.consecutiveToSentenceUuid?.let { sentenceRepository.findFirstBySentenceUuidOrderByUpdatedAtDesc(it) }
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
    if (sentenceChangeStatus == EntityChangeStatus.EDITED || periodLengthChangeRecord.record != EntityChangeStatus.NO_CHANGE) {
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
    return RecordResponse(activeRecord, eventsToEmit)
  }

  private fun createSentenceEntity(sentence: CreateSentence, chargeEntity: ChargeEntity, sentencesCreated: Map<String, SentenceEntity>, prisonerId: String, courtCaseId: String, courtAppearanceId: String): RecordResponse<SentenceEntity> {
    println("Calling from createSentenceEntity")
    val eventsToEmit = mutableSetOf<EventMetadata>()
    val consecutiveToSentence = sentence.consecutiveToChargeNumber?.let { sentencesCreated[it] } ?: sentence.consecutiveToSentenceUuid?.let { sentenceRepository.findFirstBySentenceUuidOrderByUpdatedAtDesc(it) }
    val sentenceType = sentenceTypeRepository.findBySentenceTypeUuid(sentence.sentenceTypeId) ?: throw EntityNotFoundException("No sentence type found at ${sentence.sentenceTypeId}")
    val createdSentence = sentenceRepository.save(SentenceEntity.from(sentence, serviceUserService.getUsername(), chargeEntity, consecutiveToSentence, sentenceType))
    sentenceHistoryRepository.save(SentenceHistoryEntity.from(createdSentence))

    val periodLengthResponse = periodLengthService.upsert(
      createPeriodLengthEntities = sentence.periodLengths.map {
        PeriodLengthEntity.from(
          it,
          serviceUserService.getUsername()
        )
      },
      existingPeriodLengths = createdSentence.periodLengths,
      prisonerId = prisonerId,
      onCreateConsumer = { toCreatePeriodLength ->  // Explicitly name the parameter
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
    return RecordResponse(sentence, eventsToEmit)
  }
}
