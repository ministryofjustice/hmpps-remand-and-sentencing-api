package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

import jakarta.persistence.EntityNotFoundException
import jakarta.transaction.Transactional
import jakarta.transaction.Transactional.TxType
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateSentence
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.Sentence
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.event.EventSource
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.error.ImmutableSentenceException
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.ChargeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.FineAmountEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.PeriodLengthEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.SentenceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.FineAmountRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.PeriodLengthRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.SentenceRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.SentenceTypeRepository
import java.util.UUID

@Service
class SentenceService(private val sentenceRepository: SentenceRepository, private val periodLengthRepository: PeriodLengthRepository, private val serviceUserService: ServiceUserService, private val sentenceTypeRepository: SentenceTypeRepository, private val sentenceDomainEventService: SentenceDomainEventService, private val fineAmountRepository: FineAmountRepository) {

  @Transactional(TxType.REQUIRED)
  fun createSentence(sentence: CreateSentence, chargeEntity: ChargeEntity, sentencesCreated: Map<String, SentenceEntity>, prisonerId: String): SentenceEntity {
    val existingSentence = getSentenceFromChargeOrUuid(chargeEntity, sentence.sentenceUuid)
    return if (existingSentence != null) updateSentenceEntity(existingSentence, sentence, chargeEntity, sentencesCreated, prisonerId) else createSentenceEntity(sentence, chargeEntity, sentencesCreated, prisonerId)
  }

  private fun updateSentenceEntity(existingSentence: SentenceEntity, sentence: CreateSentence, chargeEntity: ChargeEntity, sentencesCreated: Map<String, SentenceEntity>, prisonerId: String): SentenceEntity {
    if (existingSentence.statusId == EntityStatus.EDITED) {
      throw ImmutableSentenceException("Cannot edit and already edited sentence")
    }
    val consecutiveToSentence = sentence.consecutiveToChargeNumber?.let { sentencesCreated[it] } ?: sentence.consecutiveToSentenceUuid?.let { sentenceRepository.findBySentenceUuid(it) }
    val sentenceType = sentenceTypeRepository.findBySentenceTypeUuid(sentence.sentenceTypeId) ?: throw EntityNotFoundException("No sentence type found at ${sentence.sentenceTypeId}")
    val compareSentence = existingSentence.copyFrom(sentence, serviceUserService.getUsername(), chargeEntity, consecutiveToSentence, sentenceType)
    var activeRecord = existingSentence
    if (!existingSentence.isSame(compareSentence)) {
      existingSentence.statusId = EntityStatus.EDITED
      val toCreatePeriodLengths = compareSentence.periodLengths.toList()
      compareSentence.periodLengths = emptyList()
      val toCreateFineAmount = compareSentence.fineAmountEntity
      compareSentence.fineAmountEntity = null
      activeRecord = sentenceRepository.save(compareSentence)
      activeRecord.periodLengths = toCreatePeriodLengths.map { periodLengthRepository.save(it) }
      activeRecord.fineAmountEntity = toCreateFineAmount?.let { fineAmountRepository.save(it) }
      sentenceDomainEventService.update(
        prisonerId,
        activeRecord.lifetimeSentenceUuid.toString(),
        chargeEntity.lifetimeChargeUuid.toString(),
        EventSource.DPS,
      )
    }
    return activeRecord
  }

  private fun createSentenceEntity(sentence: CreateSentence, chargeEntity: ChargeEntity, sentencesCreated: Map<String, SentenceEntity>, prisonerId: String): SentenceEntity {
    val consecutiveToSentence = sentence.consecutiveToChargeNumber?.let { sentencesCreated[it] } ?: sentence.consecutiveToSentenceUuid?.let { sentenceRepository.findBySentenceUuid(it) }
    val sentenceType = sentenceTypeRepository.findBySentenceTypeUuid(sentence.sentenceTypeId) ?: throw EntityNotFoundException("No sentence type found at ${sentence.sentenceTypeId}")
    val createdSentence = sentenceRepository.save(SentenceEntity.from(sentence, serviceUserService.getUsername(), chargeEntity, consecutiveToSentence, sentenceType))
    createdSentence.periodLengths = sentence.periodLengths.map { periodLengthRepository.save(PeriodLengthEntity.from(it)) }
    sentence.fineAmount?.let { createdSentence.fineAmountEntity = fineAmountRepository.save(FineAmountEntity.from(it)) }
    sentenceDomainEventService.create(
      prisonerId,
      createdSentence.lifetimeSentenceUuid.toString(),
      chargeEntity.lifetimeChargeUuid.toString(),
      EventSource.DPS,
    )
    return createdSentence
  }

  fun getSentenceFromChargeOrUuid(chargeEntity: ChargeEntity, sentenceUuid: UUID?): SentenceEntity? = chargeEntity.getActiveSentence() ?: sentenceUuid?.let { sentenceRepository.findBySentenceUuid(sentenceUuid) }

  @Transactional(TxType.REQUIRED)
  fun findSentenceByUuid(sentenceUuid: UUID): Sentence? = sentenceRepository.findBySentenceUuid(sentenceUuid)?.let { Sentence.from(it) }

  @Transactional(TxType.REQUIRED)
  fun deleteSentence(sentence: SentenceEntity, chargeEntity: ChargeEntity, prisonerId: String) {
    sentence.statusId = EntityStatus.DELETED
    sentenceDomainEventService.delete(
      prisonerId,
      sentence.lifetimeSentenceUuid.toString(),
      chargeEntity.lifetimeChargeUuid.toString(),
      EventSource.DPS,
    )
  }
}
