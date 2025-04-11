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
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.SentenceRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.SentenceTypeRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.audit.SentenceHistoryRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacyCreateSentence
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacyPeriodLengthCreatedResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacySentence
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacySentenceCreatedResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.ServiceUserService
import java.util.UUID

@Service
class LegacySentenceService(private val sentenceRepository: SentenceRepository, private val chargeRepository: ChargeRepository, private val sentenceTypeRepository: SentenceTypeRepository, private val serviceUserService: ServiceUserService, private val legacyPeriodLengthService: LegacyPeriodLengthService, private val sentenceHistoryRepository: SentenceHistoryRepository) {

  @Transactional
  fun create(sentence: LegacyCreateSentence): List<LegacySentenceCreatedResponse> {
    val dpsSentenceType = getDpsSentenceType(sentence.legacyData.sentenceCategory, sentence.legacyData.sentenceCalcType)
    sentence.legacyData.active = sentence.active
    sentence.legacyData = dpsSentenceType?.let { sentence.legacyData.copy(sentenceCategory = null, sentenceCalcType = null, sentenceTypeDesc = null) } ?: sentence.legacyData
    val consecutiveToSentence = sentence.consecutiveToLifetimeUuid?.let { getUnlessDeleted(it) }
    val isManyCharges = sentence.chargeUuids.size > 1
    val sentenceUuid = UUID.randomUUID()
    val periodLengths = sentence.periodLengths
      .associate { it.periodLengthId to PeriodLengthEntity.from(it, dpsSentenceType?.nomisSentenceCalcType ?: sentence.legacyData.sentenceCalcType!!, serviceUserService.getUsername(), isManyCharges) }

    return sentence.chargeUuids.map { chargeUuid ->
      val charge = chargeRepository.findFirstByChargeUuidOrderByUpdatedAtDesc(chargeUuid)?.takeUnless { entity -> entity.statusId == EntityStatus.DELETED } ?: throw EntityNotFoundException("No charge found at $chargeUuid")
      if (charge.getActiveOrInactiveSentence() != null) {
        throw ChargeAlreadySentencedException("charge at $chargeUuid is already sentenced")
      }

      val createdSentence = sentenceRepository.save(SentenceEntity.from(sentence, serviceUserService.getUsername(), charge, dpsSentenceType, consecutiveToSentence, sentenceUuid, isManyCharges))
      sentenceHistoryRepository.save(SentenceHistoryEntity.from(createdSentence))
      charge.sentences.add(createdSentence)
      val (_, createdPeriodLengths) = legacyPeriodLengthService.upsert(
        periodLengths.mapValues { (_, periodLengthEntity) -> periodLengthEntity.copy() },
        createdSentence,
      )
      val courtAppearance = charge.appearanceCharges
        .map { it.appearance!! }
        .filter { it.statusId == EntityStatus.ACTIVE }
        .maxByOrNull { it.appearanceDate }
        ?: throw IllegalStateException("No active court appearance found for charge ${charge.chargeUuid}")
      LegacySentenceCreatedResponse(
        courtAppearance.courtCase.prisonerId,
        createdSentence.sentenceUuid,
        charge.chargeUuid,
        courtAppearance.appearanceUuid,
        courtAppearance.courtCase.caseUniqueIdentifier,
        createdPeriodLengths.map { LegacyPeriodLengthCreatedResponse(it.value.periodLengthUuid, it.key) },
      )
    }
  }

  @Transactional
  fun update(sentenceUuid: UUID, sentence: LegacyCreateSentence): List<Pair<EntityChangeStatus, LegacySentenceCreatedResponse>> {
    var entityChangeStatus = EntityChangeStatus.NO_CHANGE
    val dpsSentenceType = getDpsSentenceType(sentence.legacyData.sentenceCategory, sentence.legacyData.sentenceCalcType)
    sentence.legacyData.active = sentence.active
    sentence.legacyData = dpsSentenceType?.let { sentence.legacyData.copy(sentenceCategory = null, sentenceCalcType = null, sentenceTypeDesc = null) } ?: sentence.legacyData
    val isManyCharges = sentence.chargeUuids.size > 1
    val periodLengths = sentence.periodLengths
      .associate { it.periodLengthId to PeriodLengthEntity.from(it, dpsSentenceType?.nomisSentenceCalcType ?: sentence.legacyData.sentenceCalcType!!, serviceUserService.getUsername(), isManyCharges) }
    val consecutiveToSentence = sentence.consecutiveToLifetimeUuid?.let { getUnlessDeleted(it) }

    return sentence.chargeUuids.map { chargeUuid ->
      val existingSentence = sentenceRepository.findFirstBySentenceUuidAndChargeChargeUuidOrderByUpdatedAtDesc(sentenceUuid, chargeUuid)?.takeUnless { entity -> entity.statusId == EntityStatus.DELETED } ?: throw EntityNotFoundException("No sentence found at $sentenceUuid")
      var activeRecord = existingSentence
      val updatedSentence = existingSentence.copyFrom(sentence, serviceUserService.getUsername(), dpsSentenceType, consecutiveToSentence, isManyCharges)
      if (!existingSentence.isSame(updatedSentence)) {
        existingSentence.updateFrom(updatedSentence)
        sentenceHistoryRepository.save(SentenceHistoryEntity.from(existingSentence))
        entityChangeStatus = EntityChangeStatus.EDITED
        existingSentence.charge.sentences.add(activeRecord)
      }
      val (periodLengthChangeStatus, createdPeriodLengths) = legacyPeriodLengthService.upsert(
        periodLengths.mapValues { (_, periodLengthEntity) -> periodLengthEntity.copy() },
        existingSentence,
      )
      entityChangeStatus = if (periodLengthChangeStatus == EntityChangeStatus.NO_CHANGE) entityChangeStatus else EntityChangeStatus.EDITED
      val courtAppearance = activeRecord.charge.appearanceCharges
        .map { it.appearance!! }
        .filter { it.statusId == EntityStatus.ACTIVE }
        .maxByOrNull { it.appearanceDate }
        ?: throw IllegalStateException("No active court appearance found for charge ${activeRecord.charge.chargeUuid}")
      entityChangeStatus to LegacySentenceCreatedResponse(courtAppearance.courtCase.prisonerId, activeRecord.sentenceUuid, activeRecord.charge.chargeUuid, courtAppearance.appearanceUuid, courtAppearance.courtCase.caseUniqueIdentifier, createdPeriodLengths.map { LegacyPeriodLengthCreatedResponse(it.value.periodLengthUuid, it.key) })
    }
  }

  @Transactional(readOnly = true)
  fun get(lifetimeUuid: UUID): LegacySentence = LegacySentence.from(getUnlessDeleted(lifetimeUuid))

  @Transactional
  fun delete(lifetimeUuid: UUID) {
    val sentence = getUnlessDeleted(lifetimeUuid)
    sentence.delete(serviceUserService.getUsername())
    sentenceHistoryRepository.save(SentenceHistoryEntity.from(sentence))
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

  private fun getUnlessDeleted(sentenceUuid: UUID): SentenceEntity = sentenceRepository.findFirstBySentenceUuidOrderByUpdatedAtDesc(sentenceUuid)
    ?.takeUnless { entity -> entity.statusId == EntityStatus.DELETED } ?: throw EntityNotFoundException("No sentence found at $sentenceUuid")

  companion object {
    val recallNomisSentenceCalcTypes: Set<String> = setOf("CUR", "CUR_ORA", "HDR", "HDR_ORA", "FTR", "FTR_ORA", "14FTR_ORA", "FTRSCH18", "FTRSCH18_ORA", "FTR_SCH15", "FTRSCH15_ORA", "FTR_HDC", "FTR_HDC_ORA", "14FTRHDC_ORA", "LR", "LR_ORA", "LR_DPP", "LR_DLP", "LR_ALP", "LR_ALP_LASPO", "LR_ALP_CDE18", "LR_ALP_CDE21", "LR_LIFE", "LR_EPP", "LR_IPP", "LR_MLP", "LR_SEC236A", "LR_SEC91_ORA", "LRSEC250_ORA", "LR_ES", "LR_EDS18", "LR_EDS21", "LR_EDSU18", "LR_LASPO_AR", "LR_LASPO_DR", "LR_SOPC18", "LR_SOPC21", "LR_YOI_ORA")
    val recallSentenceTypeBucketUuid: UUID = UUID.fromString("f9a1551e-86b1-425b-96f7-23465a0f05fc")
  }
}
