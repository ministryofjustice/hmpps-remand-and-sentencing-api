package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.service

import jakarta.persistence.EntityNotFoundException
import org.hibernate.exception.LockAcquisitionException
import org.springframework.dao.CannotAcquireLockException
import org.springframework.orm.ObjectOptimisticLockingFailureException
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.error.ChargeAlreadySentencedException
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.ChargeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.LegacySentenceTypeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.PeriodLengthEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.RecallEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.RecallSentenceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.SentenceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.SentenceTypeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.audit.PeriodLengthHistoryEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.audit.RecallHistoryEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.audit.RecallSentenceHistoryEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.audit.SentenceHistoryEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ChangeSource
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.CourtAppearanceEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityChangeStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.PeriodLengthEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.RecallType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.SentenceEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.ChargeRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.LegacySentenceTypeRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.PeriodLengthRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.RecallRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.RecallSentenceRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.RecallTypeRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.SentenceRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.SentenceTypeRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.audit.AppearanceChargeHistoryRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.audit.ChargeHistoryRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.audit.PeriodLengthHistoryRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.audit.RecallHistoryRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.audit.RecallSentenceHistoryRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.audit.SentenceHistoryRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacyCreateSentence
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacySearchSentence
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacySentence
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacySentenceCreatedResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacySentenceDeletedResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.RecallSentenceLegacyData
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.ServiceUserService
import java.time.ZonedDateTime
import java.util.*

@Service
class LegacySentenceService(
  private val sentenceRepository: SentenceRepository,
  chargeRepository: ChargeRepository,
  private val sentenceTypeRepository: SentenceTypeRepository,
  serviceUserService: ServiceUserService,
  private val sentenceHistoryRepository: SentenceHistoryRepository,
  private val legacySentenceTypeRepository: LegacySentenceTypeRepository,
  private val recallTypeRepository: RecallTypeRepository,
  private val recallRepository: RecallRepository,
  private val recallSentenceRepository: RecallSentenceRepository,
  private val periodLengthRepository: PeriodLengthRepository,
  private val periodLengthHistoryRepository: PeriodLengthHistoryRepository,
  private val legacyPeriodLengthService: LegacyPeriodLengthService,
  chargeHistoryRepository: ChargeHistoryRepository,
  appearanceChargeHistoryRepository: AppearanceChargeHistoryRepository,
  private val recallHistoryRepository: RecallHistoryRepository,
  private val recallSentenceHistoryRepository: RecallSentenceHistoryRepository,
) : LegacyBaseService(chargeRepository, appearanceChargeHistoryRepository, chargeHistoryRepository, serviceUserService) {

  @Transactional
  fun create(sentence: LegacyCreateSentence): List<LegacySentenceCreatedResponse> {
    val dpsSentenceType = getDpsSentenceType(sentence.legacyData.sentenceCategory, sentence.legacyData.sentenceCalcType)
    val legacySentenceType =
      getLegacySentenceType(sentence.legacyData.sentenceCategory, sentence.legacyData.sentenceCalcType)
    val legacyData = sentence.legacyData
    sentence.legacyData.active = sentence.active
    sentence.legacyData = dpsSentenceType?.let {
      sentence.legacyData.copy(
        sentenceCategory = null,
        sentenceCalcType = null,
        sentenceTypeDesc = null,
      )
    } ?: sentence.legacyData
    val consecutiveToSentence = sentence.consecutiveToLifetimeUuid?.let { getUnlessDeleted(it) }
    val isManyCharges = sentence.chargeUuids.size > 1
    val sentenceUuid = UUID.randomUUID()
    val prisonerId = getPrisonerIdIfSentenceIsRecall(dpsSentenceType, sentence)

    return sentence.chargeUuids.map { chargeUuid ->
      val charge = getUnsentencedCharge(chargeUuid, sentence.appearanceUuid, getPerformedByUsername(sentence))
      val createdSentence = createSentenceRecord(
        charge,
        SentenceEntity.from(
          sentence,
          getPerformedByUsername(sentence),
          charge,
          dpsSentenceType,
          consecutiveToSentence,
          sentenceUuid,
          isManyCharges,
        ),
      )
      if (dpsSentenceType?.sentenceTypeUuid == recallSentenceTypeBucketUuid) {
        createRecall(
          sentence,
          createdSentence,
          legacySentenceType,
          RecallSentenceLegacyData.from(legacyData),
          prisonerId!!,
        )
      }

      val courtAppearance = charge.appearanceCharges
        .map { it.appearance!! }
        .filter { it.statusId != CourtAppearanceEntityStatus.DELETED }
        .maxByOrNull { it.appearanceDate }
        ?: throw IllegalStateException("No active court appearance found for charge ${charge.chargeUuid}")
      LegacySentenceCreatedResponse(
        courtAppearance.courtCase.prisonerId,
        createdSentence.sentenceUuid,
        charge.chargeUuid,
        courtAppearance.appearanceUuid,
        courtAppearance.courtCase.caseUniqueIdentifier,
      )
    }
  }

  private fun getPrisonerIdIfSentenceIsRecall(
    dpsSentenceType: SentenceTypeEntity?,
    sentence: LegacyCreateSentence,
  ): String? = if (dpsSentenceType?.sentenceTypeUuid == recallSentenceTypeBucketUuid) {
    val charge = getChargeAtAppearance(sentence.chargeUuids[0], sentence.appearanceUuid)
    charge.appearanceCharges.first().appearance?.courtCase?.prisonerId
  } else {
    null
  }

  private fun createRecall(
    sentence: LegacyCreateSentence,
    createdSentence: SentenceEntity,
    legacySentenceType: LegacySentenceTypeEntity?,
    legacyData: RecallSentenceLegacyData,
    prisonerId: String,
  ) {
    val defaultRecallType = recallTypeRepository.findOneByCode(RecallType.LR)!!
    val recall = recallRepository.save(
      RecallEntity.fromLegacy(
        sentence,
        prisonerId,
        getPerformedByUsername(sentence),
        legacySentenceType?.recallType ?: defaultRecallType,
      ),
    )
    val recallSentence = recallSentenceRepository.save(
      RecallSentenceEntity.from(
        sentence,
        createdSentence,
        recall,
        getPerformedByUsername(sentence),
        legacyData,
      ),
    )
    val recallHistory = recallHistoryRepository.save(RecallHistoryEntity.from(recall, ChangeSource.NOMIS))
    recallSentenceHistoryRepository.save(RecallSentenceHistoryEntity.from(recallHistory, recallSentence, ChangeSource.NOMIS))
  }

  fun getUnsentencedCharge(chargeUuid: UUID, appearanceUuid: UUID, performedByUser: String): ChargeEntity {
    val charge = getChargeAtAppearance(chargeUuid, appearanceUuid)
    if (charge.getLiveSentence() != null) {
      throw ChargeAlreadySentencedException("charge at $chargeUuid is already sentenced")
    }
    val appearance = charge.appearanceCharges.first { it.appearance!!.appearanceUuid == appearanceUuid }.appearance!!
    val toUpdateCharge = charge.copyFrom(performedByUser)
    return createChargeRecordIfOverManyAppearancesOrUpdate(charge, appearance, toUpdateCharge, performedByUser)
  }

  fun getChargeAtAppearance(chargeUuid: UUID, appearanceUuid: UUID): ChargeEntity = chargeRepository.findFirstByAppearanceChargesAppearanceAppearanceUuidAndChargeUuidAndStatusIdNotOrderByCreatedAtDesc(
    appearanceUuid,
    chargeUuid,
  )
    ?: throw EntityNotFoundException("No charge found at $chargeUuid")

  fun createSentenceRecord(charge: ChargeEntity, sentence: SentenceEntity): SentenceEntity {
    val createdSentence = sentenceRepository.save(sentence)
    sentenceHistoryRepository.save(
      SentenceHistoryEntity.from(
        createdSentence,
        ChangeSource.NOMIS,
      ),
    )
    charge.sentences.add(createdSentence)
    return sentence
  }

  @Retryable(maxAttempts = 3, retryFor = [ObjectOptimisticLockingFailureException::class, CannotAcquireLockException::class, LockAcquisitionException::class])
  @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.SERIALIZABLE)
  fun update(
    sentenceUuid: UUID,
    sentence: LegacyCreateSentence,
  ): List<Pair<EntityChangeStatus, LegacySentenceCreatedResponse>> {
    val dpsSentenceType = getDpsSentenceType(sentence.legacyData.sentenceCategory, sentence.legacyData.sentenceCalcType)
    val legacyData = sentence.legacyData
    sentence.legacyData.active = sentence.active
    sentence.legacyData = dpsSentenceType?.let {
      sentence.legacyData.copy(
        sentenceCategory = null,
        sentenceCalcType = null,
        sentenceTypeDesc = null,
      )
    } ?: sentence.legacyData
    val isManyCharges = sentence.chargeUuids.size > 1
    val consecutiveToSentence = sentence.consecutiveToLifetimeUuid?.let { getUnlessDeleted(it) }
    val prisonerId = getPrisonerIdIfSentenceIsRecall(dpsSentenceType, sentence)
    val legacySentenceType =
      getLegacySentenceType(sentence.legacyData.sentenceCategory, sentence.legacyData.sentenceCalcType)
    val existingPeriodLengths = periodLengthRepository.findAllBySentenceEntitySentenceUuidAndStatusIdNot(sentenceUuid).distinctBy { it.periodLengthUuid }
    sentenceRepository.acquireSentenceTransactionLock(sentenceUuid)
    val sourceSentence = sentenceRepository.findFirstBySentenceUuidAndStatusIdNotOrderByUpdatedAtDesc(sentenceUuid)
    sentenceRepository.findBySentenceUuidAndChargeChargeUuidNotInAndStatusIdNot(sentenceUuid, sentence.chargeUuids)
      .forEach { delete(it, getPerformedByUsername(sentence)) }
    val periodLengthsByChargeUuid: MutableMap<UUID, MutableSet<PeriodLengthEntity>> = mutableMapOf()
    sentenceRepository.findBySentenceUuidAndChargeUuidsAndNotAppearanceUuidAndStatusIdNot(sentenceUuid, sentence.chargeUuids, sentence.appearanceUuid)
      .forEach {
        periodLengthsByChargeUuid[it.charge.chargeUuid] = it.periodLengths
        delete(it, getPerformedByUsername(sentence))
      }
    return sentence.chargeUuids.map { chargeUuid ->
      val (existingSentence, entityStatus) = (
        (
          (
            sentenceRepository.findFirstBySentenceUuidAndChargeChargeUuidOrderByUpdatedAtDesc(
              sentenceUuid,
              chargeUuid,
            )?.takeUnless { entity -> entity.statusId == SentenceEntityStatus.DELETED }
              ?.let { it to EntityChangeStatus.NO_CHANGE }
            )
            ?: (
              getUnsentencedCharge(chargeUuid, sentence.appearanceUuid, getPerformedByUsername(sentence)).let { charge ->
                createSentenceRecord(
                  charge,
                  SentenceEntity.from(
                    sentence = sentence,
                    createdBy = getPerformedByUsername(sentence),
                    chargeEntity = charge,
                    sentenceTypeEntity = dpsSentenceType,
                    consecutiveTo = consecutiveToSentence,
                    sentenceUuid = sentenceUuid,
                    isManyCharges = isManyCharges,
                    convictionDate = sourceSentence?.convictionDate,
                    countNumber = sourceSentence?.countNumber,
                  ),
                )
              }.also { newSentence ->
                // potential to improve this if performance becomes an issue here, this copyPeriodLengthsForNewSentence could be done in a batch rather than in a loop for each sentence
                copyPeriodLengthsForNewSentence(newSentence, getPerformedByUsername(sentence), periodLengthsByChargeUuid, existingPeriodLengths)

                if (dpsSentenceType?.sentenceTypeUuid == recallSentenceTypeBucketUuid) {
                  createRecall(
                    sentence,
                    newSentence,
                    legacySentenceType,
                    RecallSentenceLegacyData.from(legacyData),
                    prisonerId!!,
                  )
                }
              } to EntityChangeStatus.CREATED
              )
          )
        )
      val activeRecord = existingSentence
      var entityChangeStatus = entityStatus
      val updatedSentence = existingSentence.copyFrom(
        sentence,
        getPerformedByUsername(sentence),
        consecutiveToSentence,
        isManyCharges,
      )
      updateRtcDateIfFtrRecall(existingSentence, sentence)
      if (!existingSentence.isSame(updatedSentence)) {
        existingSentence.updateFrom(updatedSentence)
        sentenceHistoryRepository.save(
          SentenceHistoryEntity.from(
            existingSentence,
            ChangeSource.NOMIS,
          ),
        )
        entityChangeStatus = EntityChangeStatus.EDITED
        existingSentence.charge.sentences.add(activeRecord)
      }
      // Always update period lengths using the current sentence status. Required when the sentence is changed and to cover a race condition (RASS-1119)
      // This works for the race condition because `update-sentence` is invoked twice in such cases.
      checkAndUpdatePeriodLengthStatus(existingSentence, getPerformedByUsername(sentence))

      entityChangeStatus =
        if (entityChangeStatus != EntityChangeStatus.NO_CHANGE) entityChangeStatus else EntityChangeStatus.EDITED
      val courtAppearance = activeRecord.charge.appearanceCharges
        .map { it.appearance!! }
        .filter { it.statusId != CourtAppearanceEntityStatus.DELETED }
        .maxByOrNull { it.appearanceDate }
        ?: throw IllegalStateException("No active court appearance found for charge ${activeRecord.charge.chargeUuid}")
      entityChangeStatus to LegacySentenceCreatedResponse(
        courtAppearance.courtCase.prisonerId,
        activeRecord.sentenceUuid,
        activeRecord.charge.chargeUuid,
        courtAppearance.appearanceUuid,
        courtAppearance.courtCase.caseUniqueIdentifier,
      )
    }
  }

  private fun getPerformedByUsername(sentence: LegacyCreateSentence): String = sentence.performedByUser ?: serviceUserService.getUsername()

  private fun copyPeriodLengthsForNewSentence(newSentence: SentenceEntity, performedByUser: String, periodLengthsByChargeUuid: Map<UUID, MutableSet<PeriodLengthEntity>>, existingPeriodLengths: List<PeriodLengthEntity>) {
    val newPeriodLengths = (existingPeriodLengths + periodLengthsByChargeUuid.getOrDefault(newSentence.charge.chargeUuid, mutableSetOf()))
      .distinctBy { it.periodLengthUuid } // Ensure we only copy each unique periodLengthUuid once
      .map { periodLength ->
        periodLength.copy(
          sentenceEntity = newSentence,
          statusId = PeriodLengthEntityStatus.from(newSentence.statusId),
          createdBy = performedByUser,
          createdAt = ZonedDateTime.now(),
          updatedBy = performedByUser,
          updatedAt = ZonedDateTime.now(),
        )
      }

    if (newPeriodLengths.isNotEmpty()) {
      val savedPeriodLengths = periodLengthRepository.saveAll(newPeriodLengths)
      periodLengthHistoryRepository.saveAll(
        savedPeriodLengths.map {
          PeriodLengthHistoryEntity.from(
            it,
            ChangeSource.NOMIS,
          )
        },
      )
    }
  }

  private fun checkAndUpdatePeriodLengthStatus(existingSentence: SentenceEntity, performedByUser: String) {
    val periodLengths = periodLengthRepository
      .findAllBySentenceEntitySentenceUuidAndStatusIdNot(existingSentence.sentenceUuid)
      .filter { it.statusId != PeriodLengthEntityStatus.from(existingSentence.statusId) }
      .onEach {
        it.statusId = PeriodLengthEntityStatus.from(existingSentence.statusId)
        it.updatedBy = performedByUser
        it.updatedAt = ZonedDateTime.now()
      }

    if (periodLengths.isNotEmpty()) {
      periodLengthHistoryRepository.saveAll(
        periodLengths.map {
          PeriodLengthHistoryEntity.from(
            it,
            ChangeSource.NOMIS,
          )
        },
      )
    }
  }

  private fun updateRtcDateIfFtrRecall(updatedSentence: SentenceEntity, sentence: LegacyCreateSentence) {
    val latestRecall = updatedSentence.latestRecall() ?: return
    if (latestRecall.returnToCustodyDate != sentence.returnToCustodyDate && latestRecall.recallType.code.isFixedTermRecall()) {
      latestRecall.returnToCustodyDate = sentence.returnToCustodyDate
      val recallHistoryEntity =
        recallHistoryRepository.save(
          RecallHistoryEntity.from(
            latestRecall,
            ChangeSource.NOMIS,
          ),
        )
      latestRecall.recallSentences.forEach {
        recallSentenceHistoryRepository.save(
          RecallSentenceHistoryEntity.from(
            recallHistoryEntity,
            it,
            ChangeSource.NOMIS,
          ),
        )
      }
    }
  }

  @Transactional(readOnly = true)
  fun get(sentenceUuid: UUID): LegacySentence = LegacySentence.from(getUnlessDeleted(sentenceUuid))

  @Retryable(maxAttempts = 3, retryFor = [CannotAcquireLockException::class])
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  fun delete(sentenceUuid: UUID, performedByUser: String?): LegacySentenceDeletedResponse? = sentenceRepository.findBySentenceUuid(sentenceUuid)
    .filter { it.statusId != SentenceEntityStatus.DELETED }
    .map { sentence ->
      delete(sentence, performedByUser ?: serviceUserService.getUsername())
      LegacySentenceDeletedResponse.from(sentence)
    }.firstOrNull()

  fun delete(sentence: SentenceEntity, performedByUser: String) {
    sentence.delete(performedByUser)
    sentenceHistoryRepository.save(
      SentenceHistoryEntity.from(
        sentence,
        ChangeSource.NOMIS,
      ),
    )
    deletePeriodLengths(sentence, performedByUser)
    deleteRecallSentence(sentence, performedByUser)
  }

  private fun deleteRecallSentence(sentence: SentenceEntity, performedByUser: String) {
    sentence.recallSentences.forEach {
      val recall = it.recall

      if (recall.recallSentences.size == 1) {
        recall.delete(performedByUser)
        recallHistoryRepository.save(RecallHistoryEntity.from(recall, ChangeSource.NOMIS))
      } else {
        recallHistoryRepository.save(
          RecallHistoryEntity.from(
            recall,
            ChangeSource.NOMIS,
          ),
        )
      }
      recallSentenceRepository.delete(it)
    }
  }

  private fun deletePeriodLengths(sentence: SentenceEntity, performedByUser: String) {
    sentence.periodLengths.filter { it.statusId != PeriodLengthEntityStatus.DELETED }
      .forEach { periodLength ->
        legacyPeriodLengthService.delete(periodLength, performedByUser)
      }
  }

  fun handleManyChargesSentenceDeleted(sentenceUuid: UUID, performedByUsername: String) {
    val sentences = sentenceRepository.findBySentenceUuidAndStatusId(sentenceUuid, SentenceEntityStatus.MANY_CHARGES_DATA_FIX)
    if (sentences.size == 1) {
      val sentenceRecord = sentences.first()
      sentenceRecord.statusId = SentenceEntityStatus.ACTIVE
      sentenceRecord.updatedAt = ZonedDateTime.now()
      sentenceRecord.updatedBy = performedByUsername
      sentenceHistoryRepository.save(
        SentenceHistoryEntity.from(
          sentenceRecord,
          ChangeSource.NOMIS,
        ),
      )
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

  private fun getLegacySentenceType(sentenceCategory: String?, sentenceCalcType: String?): LegacySentenceTypeEntity? {
    if (sentenceCategory != null && sentenceCalcType != null && sentenceCategory.toIntOrNull() != null) {
      return legacySentenceTypeRepository.findByNomisSentenceTypeReferenceAndSentencingAct(
        sentenceCalcType,
        sentenceCategory.toInt(),
      )
    }
    return null
  }

  private fun getUnlessDeleted(sentenceUuid: UUID): SentenceEntity = sentenceRepository.findFirstBySentenceUuidAndStatusIdNotOrderByUpdatedAtDesc(sentenceUuid)
    ?: throw EntityNotFoundException("No sentence found at $sentenceUuid")

  @Transactional(readOnly = true)
  fun search(searchSentence: LegacySearchSentence): List<LegacySentence> = sentenceRepository.findBySentenceUuidIn(searchSentence.lifetimeUuids.distinct()).mapNotNull { sentence ->
    sentence.takeUnless { entity -> entity.statusId == SentenceEntityStatus.DELETED }?.let { LegacySentence.from(it) }
  }

  companion object {
    val recallNomisSentenceCalcTypes: Set<String> = setOf(
      "CUR",
      "CUR_ORA",
      "HDR",
      "HDR_ORA",
      "FTR",
      "FTR_ORA",
      "14FTR_ORA",
      "FTR_56ORA",
      "FTRSCH18",
      "FTRSCH18_ORA",
      "FTR_SCH15",
      "FTRSCH15_ORA",
      "FTR_HDC",
      "FTR_HDC_ORA",
      "14FTRHDC_ORA",
      "LR",
      "LR_ORA",
      "LR_DPP",
      "LR_DLP",
      "LR_ALP",
      "LR_ALP_LASPO",
      "LR_ALP_CDE18",
      "LR_ALP_CDE21",
      "LR_LIFE",
      "LR_EPP",
      "LR_IPP",
      "LR_MLP",
      "LR_SEC236A",
      "LR_SEC91_ORA",
      "LRSEC250_ORA",
      "LR_ES",
      "LR_EDS18",
      "LR_EDS21",
      "LR_EDSU18",
      "LR_LASPO_AR",
      "LR_LASPO_DR",
      "LR_SOPC18",
      "LR_SOPC21",
      "LR_YOI_ORA",
    )
    val recallSentenceTypeBucketUuid: UUID = UUID.fromString("f9a1551e-86b1-425b-96f7-23465a0f05fc")
  }
}
