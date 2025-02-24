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
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.error.ImmutableSentenceException
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.ChargeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.PeriodLengthEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.SentenceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.audit.SentenceHistoryEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityChangeStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.PeriodLengthRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.SentenceRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.SentenceTypeRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.audit.SentenceHistoryRepository
import java.util.UUID

@Service
class SentenceService(private val sentenceRepository: SentenceRepository, private val periodLengthRepository: PeriodLengthRepository, private val serviceUserService: ServiceUserService, private val sentenceTypeRepository: SentenceTypeRepository, private val sentenceHistoryRepository: SentenceHistoryRepository) {

  @Transactional(TxType.REQUIRED)
  fun createSentence(sentence: CreateSentence, chargeEntity: ChargeEntity, sentencesCreated: Map<String, SentenceEntity>, prisonerId: String, courtCaseId: String, courtAppearanceDateChanged: Boolean): RecordResponse<SentenceEntity> {
    val existingSentence = getSentenceFromChargeOrUuid(chargeEntity, sentence.sentenceUuid)
    return if (existingSentence != null) updateSentenceEntity(existingSentence, sentence, chargeEntity, sentencesCreated, prisonerId, courtCaseId, courtAppearanceDateChanged) else createSentenceEntity(sentence, chargeEntity, sentencesCreated, prisonerId, courtCaseId)
  }

  private fun updateSentenceEntity(existingSentence: SentenceEntity, sentence: CreateSentence, chargeEntity: ChargeEntity, sentencesCreated: Map<String, SentenceEntity>, prisonerId: String, courtCaseId: String, courtAppearanceDateChanged: Boolean): RecordResponse<SentenceEntity> {
    if (existingSentence.statusId == EntityStatus.EDITED) {
      throw ImmutableSentenceException("Cannot edit and already edited sentence")
    }
    val consecutiveToSentence = sentence.consecutiveToChargeNumber?.let { sentencesCreated[it] } ?: sentence.consecutiveToSentenceUuid?.let { sentenceRepository.findBySentenceUuid(it) }
    val sentenceType = sentenceTypeRepository.findBySentenceTypeUuid(sentence.sentenceTypeId) ?: throw EntityNotFoundException("No sentence type found at ${sentence.sentenceTypeId}")
    val compareSentence = existingSentence.copyFrom(sentence, serviceUserService.getUsername(), chargeEntity, consecutiveToSentence, sentenceType)
    var activeRecord = existingSentence
    val eventsToEmit: MutableSet<EventMetadata> = mutableSetOf()
    if (!existingSentence.isSame(compareSentence)) {
      existingSentence.updateFrom(compareSentence)
      sentenceHistoryRepository.save(SentenceHistoryEntity.from(existingSentence))
      updatePeriodLengths(existingSentence, compareSentence.periodLengths)
      eventsToEmit.add(
        EventMetadataCreator.sentenceEventMetadata(
          prisonerId,
          courtCaseId,
          chargeEntity.lifetimeChargeUuid.toString(),
          activeRecord.sentenceUuid.toString(),
          EventType.SENTENCE_UPDATED,
        ),
      )
    } else if (courtAppearanceDateChanged) {
      eventsToEmit.add(
        EventMetadataCreator.sentenceEventMetadata(
          prisonerId,
          courtCaseId,
          chargeEntity.lifetimeChargeUuid.toString(),
          activeRecord.sentenceUuid.toString(),
          EventType.SENTENCE_UPDATED,
        ),
      )
    }
    return RecordResponse(activeRecord, eventsToEmit)
  }

  private fun createSentenceEntity(sentence: CreateSentence, chargeEntity: ChargeEntity, sentencesCreated: Map<String, SentenceEntity>, prisonerId: String, courtCaseId: String): RecordResponse<SentenceEntity> {
    val consecutiveToSentence = sentence.consecutiveToChargeNumber?.let { sentencesCreated[it] } ?: sentence.consecutiveToSentenceUuid?.let { sentenceRepository.findBySentenceUuid(it) }
    val sentenceType = sentenceTypeRepository.findBySentenceTypeUuid(sentence.sentenceTypeId) ?: throw EntityNotFoundException("No sentence type found at ${sentence.sentenceTypeId}")
    val createdSentence = sentenceRepository.save(SentenceEntity.from(sentence, serviceUserService.getUsername(), chargeEntity, consecutiveToSentence, sentenceType))
    sentenceHistoryRepository.save(SentenceHistoryEntity.from(createdSentence))
    createdSentence.periodLengths = sentence.periodLengths.map { periodLengthRepository.save(PeriodLengthEntity.from(it)) }.toMutableList()

    return RecordResponse(
      createdSentence,
      mutableSetOf(
        EventMetadataCreator.sentenceEventMetadata(
          prisonerId,
          courtCaseId,
          chargeEntity.lifetimeChargeUuid.toString(),
          createdSentence.sentenceUuid.toString(),
          EventType.SENTENCE_INSERTED,
        ),
      ),
    )
  }

  private fun updatePeriodLengths(sentenceEntity: SentenceEntity, periodLengths: List<PeriodLengthEntity>) {
    sentenceEntity.periodLengths = sentenceEntity.periodLengths.map { existingPeriodLength ->
      val updatedPeriodLength = periodLengths.firstOrNull { it.periodLengthType == existingPeriodLength.periodLengthType }
      if (updatedPeriodLength != null) {
        existingPeriodLength.updateFrom(updatedPeriodLength)
        existingPeriodLength
      } else {
        existingPeriodLength.sentenceEntity = null
        null
      }
    }.filter { it != null }
      .map { it!! }.toMutableList()
    val toAddPeriodLengths = periodLengths.filter { toAddLength -> sentenceEntity.periodLengths.none { existingLength -> existingLength.periodLengthType == toAddLength.periodLengthType } }
      .map {
        it.sentenceEntity = sentenceEntity
        periodLengthRepository.save(it)
      }
    sentenceEntity.periodLengths.addAll(toAddPeriodLengths)
  }

  fun getSentenceFromChargeOrUuid(chargeEntity: ChargeEntity, sentenceUuid: UUID?): SentenceEntity? = chargeEntity.getActiveSentence() ?: sentenceUuid?.let { sentenceRepository.findBySentenceUuid(sentenceUuid) }

  @Transactional(TxType.REQUIRED)
  fun findSentenceByUuid(sentenceUuid: UUID): Sentence? = sentenceRepository.findBySentenceUuid(sentenceUuid)?.let { Sentence.from(it) }

  @Transactional(TxType.REQUIRED)
  fun deleteSentence(sentence: SentenceEntity, chargeEntity: ChargeEntity, prisonerId: String, courtCaseId: String): RecordResponse<SentenceEntity> {
    val changeStatus = if (sentence.statusId == EntityStatus.DELETED) EntityChangeStatus.NO_CHANGE else EntityChangeStatus.DELETED
    sentence.delete(serviceUserService.getUsername())
    sentenceHistoryRepository.save(SentenceHistoryEntity.from(sentence))
    val eventsToEmit: MutableSet<EventMetadata> = mutableSetOf()
    if (changeStatus == EntityChangeStatus.DELETED) {
      eventsToEmit.add(
        EventMetadataCreator.sentenceEventMetadata(
          prisonerId,
          courtCaseId,
          chargeEntity.lifetimeChargeUuid.toString(),
          sentence.sentenceUuid.toString(),
          EventType.SENTENCE_DELETED,
        ),
      )
    }
    return RecordResponse(sentence, eventsToEmit)
  }
}
