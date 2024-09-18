package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

import jakarta.persistence.EntityNotFoundException
import jakarta.transaction.Transactional
import jakarta.transaction.Transactional.TxType
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateSentence
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.error.ImmutableSentenceException
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.ChargeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.SentenceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.PeriodLengthRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.SentenceRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.SentenceTypeRepository
import java.util.UUID

@Service
class SentenceService(private val sentenceRepository: SentenceRepository, private val periodLengthRepository: PeriodLengthRepository, private val serviceUserService: ServiceUserService, private val sentenceTypeRepository: SentenceTypeRepository) {

  @Transactional(TxType.REQUIRED)
  fun createSentence(sentence: CreateSentence, chargeEntity: ChargeEntity, sentencesCreated: Map<String, SentenceEntity>): SentenceEntity {
    val consecutiveToSentence = sentence.consecutiveToChargeNumber?.let { sentencesCreated[it] }
    val sentenceType = sentenceTypeRepository.findBySentenceTypeUuid(sentence.sentenceTypeId) ?: throw EntityNotFoundException("No sentence type found at ${sentence.sentenceTypeId}")
    val toCreateSentence = getSentenceFromChargeOrUuid(chargeEntity, sentence.sentenceUuid)
      ?.let { sentenceEntity ->
        if (sentenceEntity.statusId == EntityStatus.DELETED) {
          throw ImmutableSentenceException("Cannot edit and already edited sentence")
        }

        val compareSentence = SentenceEntity.from(sentence, serviceUserService.getUsername(), chargeEntity, consecutiveToSentence, sentenceType)
        if (sentenceEntity.isSame(compareSentence)) {
          return@let sentenceEntity
        }
        sentenceEntity.statusId = EntityStatus.EDITED
        compareSentence.sentenceUuid = UUID.randomUUID()
        compareSentence.supersedingSentence = sentenceEntity
        compareSentence.lifetimeSentenceUuid = sentenceEntity.lifetimeSentenceUuid
        compareSentence
      } ?: SentenceEntity.from(sentence, serviceUserService.getUsername(), chargeEntity, consecutiveToSentence, sentenceType)
    val toCreatePeriodLengths = toCreateSentence.periodLengths.filter { it.id == 0 }
    toCreateSentence.periodLengths = toCreateSentence.periodLengths.filter { it.id != 0 }
    val createdSentence = sentenceRepository.save(toCreateSentence)
    toCreatePeriodLengths.forEach {
      it.sentenceEntity = createdSentence
      periodLengthRepository.save(it)
    }
    return createdSentence
  }

  fun getSentenceFromChargeOrUuid(chargeEntity: ChargeEntity, sentenceUuid: UUID?): SentenceEntity? {
    return chargeEntity.getActiveSentence() ?: sentenceUuid?.let { sentenceRepository.findBySentenceUuid(sentenceUuid) }
  }

  @Transactional(TxType.REQUIRED)
  fun deleteSentence(sentence: SentenceEntity) {
    sentence.statusId = EntityStatus.DELETED
  }
}
