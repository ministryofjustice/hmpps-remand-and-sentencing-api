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
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.FineAmountEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.PeriodLengthEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.SentenceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityChangeStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.FineAmountRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.PeriodLengthRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.SentenceRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.SentenceTypeRepository
import java.util.UUID

@Service
class SentenceService(private val sentenceRepository: SentenceRepository, private val periodLengthRepository: PeriodLengthRepository, private val serviceUserService: ServiceUserService, private val sentenceTypeRepository: SentenceTypeRepository, private val fineAmountRepository: FineAmountRepository) {

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
      existingSentence.statusId = EntityStatus.EDITED
      val toCreatePeriodLengths = compareSentence.periodLengths.toList()
      compareSentence.periodLengths = emptyList()
      val toCreateFineAmount = compareSentence.fineAmountEntity
      compareSentence.fineAmountEntity = null
      activeRecord = sentenceRepository.save(compareSentence)
      activeRecord.periodLengths = toCreatePeriodLengths.map { periodLengthRepository.save(it) }
      activeRecord.fineAmountEntity = toCreateFineAmount?.let { fineAmountRepository.save(it) }
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
    createdSentence.periodLengths = sentence.periodLengths.map { periodLengthRepository.save(PeriodLengthEntity.from(it)) }
    sentence.fineAmount?.let { createdSentence.fineAmountEntity = fineAmountRepository.save(FineAmountEntity.from(it)) }

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

  fun getSentenceFromChargeOrUuid(chargeEntity: ChargeEntity, sentenceUuid: UUID?): SentenceEntity? = chargeEntity.getActiveSentence() ?: sentenceUuid?.let { sentenceRepository.findBySentenceUuid(sentenceUuid) }

  @Transactional(TxType.REQUIRED)
  fun findSentenceByUuid(sentenceUuid: UUID): Sentence? = sentenceRepository.findBySentenceUuid(sentenceUuid)?.let { Sentence.from(it) }

  @Transactional(TxType.REQUIRED)
  fun deleteSentence(sentence: SentenceEntity, chargeEntity: ChargeEntity, prisonerId: String, courtCaseId: String): RecordResponse<SentenceEntity> {
    val changeStatus = if (sentence.statusId == EntityStatus.DELETED) EntityChangeStatus.NO_CHANGE else EntityChangeStatus.DELETED
    sentence.statusId = EntityStatus.DELETED
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
