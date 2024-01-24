package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

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
import java.util.UUID

@Service
class SentenceService(private val sentenceRepository: SentenceRepository, private val periodLengthRepository: PeriodLengthRepository, private val serviceUserService: ServiceUserService) {

  @Transactional(TxType.REQUIRED)
  fun createSentence(sentence: CreateSentence, chargeEntity: ChargeEntity): SentenceEntity {
    val toCreateSentence = getSentenceFromChargeOrUuid(chargeEntity, sentence.sentenceUuid)
      ?.let { sentenceEntity ->
        if (sentenceEntity.statusId == EntityStatus.DELETED) {
          throw ImmutableSentenceException("Cannot edit and already edited sentence")
        }
        val compareSentence = SentenceEntity.from(sentence, serviceUserService.getUsername(), chargeEntity)
        if (sentenceEntity.isSame(compareSentence)) {
          return@let sentenceEntity
        }
        sentenceEntity.statusId = EntityStatus.EDITED
        compareSentence.sentenceUuid = UUID.randomUUID()
        compareSentence.supersedingSentence = sentenceEntity
        compareSentence.lifetimeSentenceUuid = sentenceEntity.lifetimeSentenceUuid
        compareSentence.custodialPeriodLength = if (sentenceEntity.custodialPeriodLength.isSame(compareSentence.custodialPeriodLength)) sentenceEntity.custodialPeriodLength else compareSentence.custodialPeriodLength
        compareSentence.extendedLicensePeriodLength = if (sentenceEntity.extendedLicensePeriodLength?.isSame(compareSentence.extendedLicensePeriodLength) == true) sentenceEntity.extendedLicensePeriodLength else compareSentence.extendedLicensePeriodLength
        compareSentence
      } ?: SentenceEntity.from(sentence, serviceUserService.getUsername(), chargeEntity)
    toCreateSentence.custodialPeriodLength = periodLengthRepository.save(toCreateSentence.custodialPeriodLength)
    toCreateSentence.extendedLicensePeriodLength = toCreateSentence.extendedLicensePeriodLength?.let { periodLengthRepository.save(it) }
    return sentenceRepository.save(toCreateSentence)
  }

  fun getSentenceFromChargeOrUuid(chargeEntity: ChargeEntity, sentenceUuid: UUID?): SentenceEntity? {
    return chargeEntity.getActiveSentence() ?: sentenceUuid?.let { sentenceRepository.findBySentenceUuid(sentenceUuid) }
  }

  @Transactional(TxType.REQUIRED)
  fun deleteSentence(sentence: SentenceEntity) {
    sentence.statusId = EntityStatus.DELETED
  }
}
