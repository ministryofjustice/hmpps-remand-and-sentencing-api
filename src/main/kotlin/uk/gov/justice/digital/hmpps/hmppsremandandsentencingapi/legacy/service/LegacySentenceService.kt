package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.service

import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.error.ChargeAlreadySentencedException
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.PeriodLengthEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.SentenceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.SentenceTypeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.audit.SentenceHistoryEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityChangeStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.ChargeRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.PeriodLengthRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.SentenceRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.SentenceTypeRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.audit.SentenceHistoryRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacyCreateSentence
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacySentence
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacySentenceCreatedResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.ServiceUserService
import java.util.UUID

@Service
class LegacySentenceService(private val sentenceRepository: SentenceRepository, private val chargeRepository: ChargeRepository, private val sentenceTypeRepository: SentenceTypeRepository, private val serviceUserService: ServiceUserService, private val periodLengthRepository: PeriodLengthRepository, private val sentenceHistoryRepository: SentenceHistoryRepository) {

  @Transactional
  fun create(sentence: LegacyCreateSentence): LegacySentenceCreatedResponse {
    val charge = chargeRepository.findFirstByLifetimeChargeUuidOrderByCreatedAtDesc(sentence.chargeLifetimeUuid)?.takeUnless { entity -> entity.statusId == EntityStatus.DELETED } ?: throw EntityNotFoundException("No charge found at ${sentence.chargeLifetimeUuid}")
    if (charge.getActiveOrInactiveSentence() != null) {
      throw ChargeAlreadySentencedException("charge at ${sentence.chargeLifetimeUuid} is already sentenced")
    }
    val dpsSentenceType = getDpsSentenceType(sentence.legacyData.sentenceCategory, sentence.legacyData.sentenceCalcType)
    sentence.legacyData = dpsSentenceType?.let { sentence.legacyData.copy(sentenceCategory = null, sentenceCalcType = null, sentenceTypeDesc = null) } ?: sentence.legacyData
    val consecutiveToSentence = sentence.consecutiveToLifetimeUuid?.let { getUnlessDeleted(it) }
    val createdSentence = sentenceRepository.save(SentenceEntity.from(sentence, serviceUserService.getUsername(), charge, dpsSentenceType, consecutiveToSentence))
    sentenceHistoryRepository.save(SentenceHistoryEntity.from(createdSentence))
    charge.sentences.add(createdSentence)
    createdSentence.periodLengths = sentence.periodLengths.map { legacyPeriodLength ->
      val createdPeriodLength = periodLengthRepository.save(
        PeriodLengthEntity.from(legacyPeriodLength, dpsSentenceType?.nomisSentenceCalcType ?: sentence.legacyData.sentenceCalcType!!),
      )
      createdPeriodLength.sentenceEntity = createdSentence
      createdPeriodLength
    }.toMutableList()
    val courtCase = charge.courtAppearances.filter { it.statusId == EntityStatus.ACTIVE }.maxBy { it.appearanceDate }.courtCase
    return LegacySentenceCreatedResponse(
      courtCase.prisonerId,
      createdSentence.sentenceUuid,
      charge.lifetimeChargeUuid,
      courtCase.caseUniqueIdentifier,
    )
  }

  @Transactional
  fun update(lifetimeUuid: UUID, sentence: LegacyCreateSentence): Pair<EntityChangeStatus, LegacySentenceCreatedResponse> {
    var entityChangeStatus = EntityChangeStatus.NO_CHANGE
    val existingSentence = getUnlessDeleted(lifetimeUuid)
    var activeRecord = existingSentence
    val dpsSentenceType = getDpsSentenceType(sentence.legacyData.sentenceCategory, sentence.legacyData.sentenceCalcType)
    sentence.legacyData = dpsSentenceType?.let { sentence.legacyData.copy(sentenceCategory = null, sentenceCalcType = null, sentenceTypeDesc = null) } ?: sentence.legacyData
    val consecutiveToSentence = sentence.consecutiveToLifetimeUuid?.let { getUnlessDeleted(it) }
    val updatedSentence = existingSentence.copyFrom(sentence, serviceUserService.getUsername(), dpsSentenceType, consecutiveToSentence)
    if (!existingSentence.isSame(updatedSentence)) {
      existingSentence.updateFrom(updatedSentence)
      sentenceHistoryRepository.save(SentenceHistoryEntity.from(existingSentence))
      entityChangeStatus = EntityChangeStatus.EDITED
      updatePeriodLengths(existingSentence, updatedSentence.periodLengths)
      existingSentence.charge.sentences.add(activeRecord)
    }
    val courtCase = activeRecord.charge.courtAppearances.filter { it.statusId == EntityStatus.ACTIVE }.maxBy { it.appearanceDate }.courtCase
    return entityChangeStatus to LegacySentenceCreatedResponse(courtCase.prisonerId, activeRecord.sentenceUuid, activeRecord.charge.lifetimeChargeUuid, courtCase.caseUniqueIdentifier)
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

  @Transactional(readOnly = true)
  fun get(lifetimeUuid: UUID): LegacySentence = LegacySentence.from(getUnlessDeleted(lifetimeUuid))

  @Transactional
  fun delete(lifetimeUuid: UUID) {
    val sentence = getUnlessDeleted(lifetimeUuid)
    sentence.statusId = EntityStatus.DELETED
  }

  private fun getDpsSentenceType(sentenceCategory: String?, sentenceCalcType: String?): SentenceTypeEntity? {
    if (sentenceCategory != null && sentenceCalcType != null) {
      if (recallNomisSentenceCalcTypes.contains(sentenceCalcType)) {
        return sentenceTypeRepository.findBySentenceTypeUuid(recallSentenceTypeBucketUuid)
      }
      return sentenceTypeRepository.findByNomisCjaCodeAndNomisSentenceCalcType(sentenceCategory, sentenceCalcType)
    }
    return null
  }

  private fun getUnlessDeleted(sentenceUuid: UUID): SentenceEntity = sentenceRepository.findBySentenceUuid(sentenceUuid)
    ?.takeUnless { entity -> entity.statusId == EntityStatus.DELETED } ?: throw EntityNotFoundException("No sentence found at $sentenceUuid")

  companion object {
    val recallNomisSentenceCalcTypes: Set<String> = setOf("CUR", "CUR_ORA", "HDR", "HDR_ORA", "FTR", "FTR_ORA", "14FTR_ORA", "FTRSCH18", "FTRSCH18_ORA", "FTR_SCH15", "FTRSCH15_ORA", "FTR_HDC", "FTR_HDC_ORA", "14FTRHDC_ORA", "LR", "LR_ORA", "LR_DPP", "LR_DLP", "LR_ALP", "LR_ALP_LASPO", "LR_ALP_CDE18", "LR_ALP_CDE21", "LR_LIFE", "LR_EPP", "LR_IPP", "LR_MLP", "LR_SEC236A", "LR_SEC91_ORA", "LRSEC250_ORA", "LR_ES", "LR_EDS18", "LR_EDS21", "LR_EDSU18", "LR_LASPO_AR", "LR_LASPO_DR", "LR_SOPC18", "LR_SOPC21", "LR_YOI_ORA")
    val recallSentenceTypeBucketUuid: UUID = UUID.fromString("f9a1551e-86b1-425b-96f7-23465a0f05fc")
  }
}
