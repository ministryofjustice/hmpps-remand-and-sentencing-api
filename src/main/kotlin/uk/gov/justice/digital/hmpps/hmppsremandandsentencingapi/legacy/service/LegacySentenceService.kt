package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.service

import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.error.ChargeAlreadySentencedException
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.ChargeEntity
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
import java.time.ZonedDateTime
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
      val charge = getCharge(chargeUuid)
      val createdSentence = createSentenceRecord(charge, SentenceEntity.from(sentence, serviceUserService.getUsername(), charge, dpsSentenceType, consecutiveToSentence, sentenceUuid, isManyCharges))
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

  fun getCharge(chargeUuid: UUID): ChargeEntity {
    val charge = chargeRepository.findFirstByChargeUuidOrderByUpdatedAtDesc(chargeUuid)?.takeUnless { entity -> entity.statusId == EntityStatus.DELETED } ?: throw EntityNotFoundException("No charge found at $chargeUuid")
    if (charge.getActiveOrInactiveSentence() != null) {
      throw ChargeAlreadySentencedException("charge at $chargeUuid is already sentenced")
    }
    return charge
  }

  fun createSentenceRecord(charge: ChargeEntity, sentence: SentenceEntity): SentenceEntity {
    val createdSentence = sentenceRepository.save(sentence)
    sentenceHistoryRepository.save(SentenceHistoryEntity.from(createdSentence))
    charge.sentences.add(createdSentence)
    return sentence
  }

  @Transactional
  fun update(sentenceUuid: UUID, sentence: LegacyCreateSentence): List<Pair<EntityChangeStatus, LegacySentenceCreatedResponse>> {
    val dpsSentenceType = getDpsSentenceType(sentence.legacyData.sentenceCategory, sentence.legacyData.sentenceCalcType)
    sentence.legacyData.active = sentence.active
    sentence.legacyData = dpsSentenceType?.let { sentence.legacyData.copy(sentenceCategory = null, sentenceCalcType = null, sentenceTypeDesc = null) } ?: sentence.legacyData
    val isManyCharges = sentence.chargeUuids.size > 1
    val periodLengths = sentence.periodLengths
      .associate { it.periodLengthId to PeriodLengthEntity.from(it, dpsSentenceType?.nomisSentenceCalcType ?: sentence.legacyData.sentenceCalcType!!, serviceUserService.getUsername(), isManyCharges) }
    val consecutiveToSentence = sentence.consecutiveToLifetimeUuid?.let { getUnlessDeleted(it) }

    return sentence.chargeUuids.map { chargeUuid ->
      val (existingSentence, entityStatus) = (
        (
          (
            sentenceRepository.findFirstBySentenceUuidAndChargeChargeUuidOrderByUpdatedAtDesc(
              sentenceUuid,
              chargeUuid,
            )?.takeUnless { entity -> entity.statusId == EntityStatus.DELETED }?.let { it to EntityChangeStatus.NO_CHANGE }
            )
            ?: (
              getCharge(chargeUuid).let { charge ->
                createSentenceRecord(
                  charge,
                  SentenceEntity.from(
                    sentence,
                    serviceUserService.getUsername(),
                    charge,
                    dpsSentenceType,
                    consecutiveToSentence,
                    sentenceUuid,
                    isManyCharges,
                  ),
                )
              } to EntityChangeStatus.CREATED
              )
          )
        )
      var activeRecord = existingSentence
      var entityChangeStatus = entityStatus
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
      entityChangeStatus = if (periodLengthChangeStatus == EntityChangeStatus.NO_CHANGE || entityChangeStatus != EntityChangeStatus.NO_CHANGE) entityChangeStatus else EntityChangeStatus.EDITED
      val courtAppearance = activeRecord.charge.appearanceCharges
        .map { it.appearance!! }
        .filter { it.statusId == EntityStatus.ACTIVE }
        .maxByOrNull { it.appearanceDate }
        ?: throw IllegalStateException("No active court appearance found for charge ${activeRecord.charge.chargeUuid}")
      entityChangeStatus to LegacySentenceCreatedResponse(courtAppearance.courtCase.prisonerId, activeRecord.sentenceUuid, activeRecord.charge.chargeUuid, courtAppearance.appearanceUuid, courtAppearance.courtCase.caseUniqueIdentifier, createdPeriodLengths.map { LegacyPeriodLengthCreatedResponse(it.value.periodLengthUuid, it.key) })
    }
  }

  @Transactional(readOnly = true)
  fun get(sentenceUuid: UUID): LegacySentence = LegacySentence.from(getUnlessDeleted(sentenceUuid))

  @Transactional
  fun delete(sentenceUuid: UUID): LegacySentence? = sentenceRepository.findBySentenceUuid(sentenceUuid)
    .filter { it.statusId != EntityStatus.DELETED }
    .map { sentence ->
      delete(sentence)
      LegacySentence.from(sentence)
    }.firstOrNull()

  fun delete(sentence: SentenceEntity) {
    sentence.delete(serviceUserService.getUsername())
    sentenceHistoryRepository.save(SentenceHistoryEntity.from(sentence))
    sentence.periodLengths
      .filter { it.statusId != EntityStatus.DELETED }
      .forEach { periodLength ->
        legacyPeriodLengthService.delete(periodLength)
        periodLength
      }
  }

  fun handleManyChargesSentenceDeleted(sentenceUuid: UUID) {
    val sentences = sentenceRepository.findBySentenceUuidAndStatusId(sentenceUuid, EntityStatus.MANY_CHARGES_DATA_FIX)
    if (sentences.size == 1) {
      val sentenceRecord = sentences.first()
      sentenceRecord.statusId = EntityStatus.ACTIVE
      sentenceRecord.updatedAt = ZonedDateTime.now()
      sentenceRecord.updatedBy = serviceUserService.getUsername()
      sentenceHistoryRepository.save(SentenceHistoryEntity.from(sentenceRecord))
    }
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
