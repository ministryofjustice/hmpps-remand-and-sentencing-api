package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.service

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.error.ChargeAlreadySentencedException
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.FineAmountEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.PeriodLengthEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.SentenceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityChangeStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.ChargeRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.FineAmountRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.PeriodLengthRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.SentenceRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.SentenceTypeRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacyCreateSentence
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacySentence
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacySentenceCreatedResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.ServiceUserService
import java.util.UUID

@Service
class LegacySentenceService(private val sentenceRepository: SentenceRepository, private val chargeRepository: ChargeRepository, private val sentenceTypeRepository: SentenceTypeRepository, private val objectMapper: ObjectMapper, private val serviceUserService: ServiceUserService, private val fineAmountRepository: FineAmountRepository, private val periodLengthRepository: PeriodLengthRepository) {

  @Transactional
  fun create(sentence: LegacyCreateSentence): LegacySentenceCreatedResponse {
    val charge = chargeRepository.findFirstByLifetimeChargeUuidOrderByCreatedAtDesc(sentence.chargeLifetimeUuid)?.takeUnless { entity -> entity.statusId == EntityStatus.DELETED } ?: throw EntityNotFoundException("No charge found at ${sentence.chargeLifetimeUuid}")
    if (charge.getActiveOrInactiveSentence() != null) {
      throw ChargeAlreadySentencedException("charge at ${sentence.chargeLifetimeUuid} is already sentenced")
    }
    val dpsSentenceType = (sentence.legacyData.sentenceCategory to sentence.legacyData.sentenceCalcType).takeIf { (sentenceCategory, sentenceCalcType) -> sentenceCategory != null && sentenceCalcType != null }?.let { (sentenceCategory, sentenceCalcType) -> sentenceTypeRepository.findByNomisCjaCodeAndNomisSentenceCalcType(sentenceCategory!!, sentenceCalcType!!) }
    val sentenceLegacyData = dpsSentenceType?.let { sentence.legacyData.copy(sentenceCategory = null, sentenceCalcType = null, sentenceTypeDesc = null) } ?: sentence.legacyData
    val legacyData = objectMapper.valueToTree<JsonNode>(sentenceLegacyData)
    val consecutiveToSentence = sentence.consecutiveToLifetimeUuid?.let { getUnlessDeleted(it) }
    val createdSentence = sentenceRepository.save(SentenceEntity.from(sentence, serviceUserService.getUsername(), charge, dpsSentenceType, legacyData, consecutiveToSentence))
    charge.sentences.add(createdSentence)
    sentence.fine?.let { createdSentence.fineAmountEntity = fineAmountRepository.save(FineAmountEntity.from(it)) }
    createdSentence.periodLengths = sentence.periodLengths.map { legacyPeriodLength ->
      val createdPeriodLength = periodLengthRepository.save(
        PeriodLengthEntity.from(legacyPeriodLength, dpsSentenceType?.nomisSentenceCalcType ?: sentenceLegacyData.sentenceCalcType!!),
      )
      createdPeriodLength.sentenceEntity = createdSentence
      createdPeriodLength
    }

    return LegacySentenceCreatedResponse(charge.courtAppearances.filter { it.statusId == EntityStatus.ACTIVE }.maxBy { it.appearanceDate }.courtCase.prisonerId, createdSentence.lifetimeSentenceUuid!!, charge.lifetimeChargeUuid)
  }

  @Transactional
  fun update(lifetimeUuid: UUID, sentence: LegacyCreateSentence): Pair<EntityChangeStatus, LegacySentenceCreatedResponse> {
    var entityChangeStatus = EntityChangeStatus.NO_CHANGE
    val existingSentence = getUnlessDeleted(lifetimeUuid)
    var activeRecord = existingSentence
    val dpsSentenceType = (sentence.legacyData.sentenceCategory to sentence.legacyData.sentenceCalcType).takeIf { (sentenceCategory, sentenceCalcType) -> sentenceCategory != null && sentenceCalcType != null }?.let { (sentenceCategory, sentenceCalcType) -> sentenceTypeRepository.findByNomisCjaCodeAndNomisSentenceCalcType(sentenceCategory!!, sentenceCalcType!!) }
    val sentenceLegacyData = dpsSentenceType?.let { sentence.legacyData.copy(sentenceCategory = null, sentenceCalcType = null, sentenceTypeDesc = null) } ?: sentence.legacyData
    val legacyData = objectMapper.valueToTree<JsonNode>(sentenceLegacyData)
    val consecutiveToSentence = sentence.consecutiveToLifetimeUuid?.let { getUnlessDeleted(it) }
    val updatedSentence = existingSentence.copyFrom(sentence, serviceUserService.getUsername(), dpsSentenceType, legacyData, consecutiveToSentence)
    if (!existingSentence.isSame(updatedSentence)) {
      existingSentence.statusId = EntityStatus.EDITED
      entityChangeStatus = EntityChangeStatus.EDITED
      val toCreateFineAmount = updatedSentence.fineAmountEntity
      updatedSentence.fineAmountEntity = null
      val toCreatePeriodLengths = updatedSentence.periodLengths
      updatedSentence.periodLengths = emptyList()
      val activeRecord = sentenceRepository.save(updatedSentence)
      activeRecord.fineAmountEntity = toCreateFineAmount?.let { fineAmountRepository.save(it) }
      activeRecord.periodLengths = toCreatePeriodLengths.map {
        val createdPeriodLength = periodLengthRepository.save(it)
        createdPeriodLength.sentenceEntity = activeRecord
        createdPeriodLength
      }
      existingSentence.charge.sentences.add(activeRecord)
    }
    return entityChangeStatus to LegacySentenceCreatedResponse(activeRecord.charge.courtAppearances.filter { it.statusId == EntityStatus.ACTIVE }.maxBy { it.appearanceDate }.courtCase.prisonerId, activeRecord.lifetimeSentenceUuid!!, activeRecord.charge.lifetimeChargeUuid)
  }

  @Transactional(readOnly = true)
  fun get(lifetimeUuid: UUID): LegacySentence {
    return LegacySentence.from(getUnlessDeleted(lifetimeUuid), objectMapper)
  }

  @Transactional
  fun delete(lifetimeUuid: UUID) {
    val sentence = getUnlessDeleted(lifetimeUuid)
    sentence.statusId = EntityStatus.DELETED
  }

  private fun getUnlessDeleted(lifetimeUuid: UUID): SentenceEntity {
    return sentenceRepository.findFirstByLifetimeSentenceUuidOrderByCreatedAtDesc(lifetimeUuid)
      ?.takeUnless { entity -> entity.statusId == EntityStatus.DELETED } ?: throw EntityNotFoundException("No sentence found at $lifetimeUuid")
  }
}
