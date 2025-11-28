package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.AdjustmentsApiClient
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.dto.AdjustmentDto
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.dto.UnlawfullyAtLargeDto
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateRecall
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.DeleteRecallResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.IsRecallPossible
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.IsRecallPossibleRequest
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.IsRecallPossibleResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.SaveRecallResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.recall.Recall
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.EventMetadata
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.EventType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.RecordResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.util.EventMetadataCreator
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.RecallEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.RecallSentenceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.SentenceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.audit.RecallHistoryEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.audit.RecallSentenceHistoryEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ChangeSource
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.RecallEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.RecallType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.SentenceTypeClassification
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.RecallRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.RecallSentenceRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.RecallTypeRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.SentenceRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.audit.RecallHistoryRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.audit.RecallSentenceHistoryRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.service.LegacyRecallService
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.service.LegacySentenceService
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.*

@Service
class RecallService(
  private val recallRepository: RecallRepository,
  private val recallSentenceRepository: RecallSentenceRepository,
  private val recallTypeRepository: RecallTypeRepository,
  private val sentenceRepository: SentenceRepository,
  private val sentenceService: SentenceService,
  private val recallHistoryRepository: RecallHistoryRepository,
  private val recallSentenceHistoryRepository: RecallSentenceHistoryRepository,
  private val adjustmentsApiClient: AdjustmentsApiClient,
) {
  @Transactional
  fun createRecall(createRecall: CreateRecall, recallUuid: UUID? = null): RecordResponse<SaveRecallResponse> {
    val recallType = recallTypeRepository.findOneByCode(createRecall.recallTypeCode)
    val recall = recallRepository.save(RecallEntity.fromDps(createRecall, recallType!!, recallUuid))
    createRecall.sentenceIds?.let { sentenceIds ->
      sentenceRepository.findBySentenceUuidIn(sentenceIds)
        .forEach {
          if (LegacySentenceService.recallSentenceTypeBucketUuid == it.sentenceType?.sentenceTypeUuid) {
            throw IllegalStateException("Tried to create a recall using a legacy recall sentence (${it.sentenceUuid})")
          }
          recallSentenceRepository.save(RecallSentenceEntity.placeholderEntity(recall, it))
        }
    }
    if (doesRecallRequireUAL(createRecall.revocationDate, createRecall.returnToCustodyDate)) {
      adjustmentsApiClient.createAdjustments(
        listOf(
          createUalDtoForRecall(
            recall.prisonerId,
            recall.revocationDate!!,
            recall.returnToCustodyDate!!,
            recall.recallUuid,
          ),
        ),
      )
    }
    return RecordResponse(
      SaveRecallResponse.from(recall),
      mutableSetOf(
        EventMetadataCreator.recallEventMetadata(
          recall.prisonerId,
          recall.recallUuid.toString(),
          (createRecall.sentenceIds ?: listOf()).map { it.toString() },
          emptyList(),
          null,
          EventType.RECALL_INSERTED,
        ),
      ),
    )
  }

  @Transactional
  fun updateRecall(recallUuid: UUID, recall: CreateRecall): RecordResponse<SaveRecallResponse> {
    val recallToUpdate = recallRepository.findOneByRecallUuid(recallUuid)

    return if (recallToUpdate == null) {
      createRecall(recall, recallUuid)
    } else {
      val originalRevocationDate =
        requireNotNull(recallToUpdate.revocationDate) { "Can only update DPS recall which requires revocation date" }
      val originalRTCDate = recallToUpdate.returnToCustodyDate

      val recallHistoryEntity =
        recallHistoryRepository.save(
          RecallHistoryEntity.from(
            recallToUpdate,
            RecallEntityStatus.EDITED,
            ChangeSource.DPS,
          ),
        )
      recallToUpdate.recallSentences.forEach {
        recallSentenceHistoryRepository.save(
          RecallSentenceHistoryEntity.from(recallHistoryEntity, it, ChangeSource.DPS).apply {},
        )
      }
      val recallTypeEntity = recallTypeRepository.findOneByCode(recall.recallTypeCode)!!

      val previousSentenceIds = recallToUpdate.recallSentences.map { it.sentence.sentenceUuid }

      val sentencesToDelete = previousSentenceIds.filterNot { (recall.sentenceIds ?: emptyList()).contains(it) }
      val sentencesToCreate = recall.sentenceIds?.filterNot { previousSentenceIds.contains(it) } ?: listOf()

      recallToUpdate.recallSentences.filter { sentencesToDelete.contains(it.sentence.sentenceUuid) }.forEach {
        recallSentenceRepository.delete(it)
      }
      sentenceRepository.findBySentenceUuidIn(sentencesToCreate)
        .forEach {
          if (LegacySentenceService.recallSentenceTypeBucketUuid == it.sentenceType?.sentenceTypeUuid) {
            throw IllegalStateException("Tried to update a recall with a legacy recall sentence (${it.sentenceUuid})")
          }
          recallSentenceRepository.save(RecallSentenceEntity.placeholderEntity(recallToUpdate, it))
        }

      recallToUpdate.apply {
        revocationDate = recall.revocationDate
        returnToCustodyDate = recall.returnToCustodyDate
        recallType = recallTypeEntity
        updatedAt = ZonedDateTime.now()
        updatedBy = recall.createdByUsername
        updatedPrison = recall.createdByPrison
        calculationRequestId = recall.calculationRequestId
      }
      val savedRecall = recallRepository.save(recallToUpdate)
      updateAdjustmentsIfRequired(
        recallToUpdate.recallUuid,
        recallToUpdate.prisonerId,
        originalRevocationDate,
        originalRTCDate,
        recall.revocationDate,
        recall.returnToCustodyDate,
      )

      return RecordResponse(
        SaveRecallResponse.from(savedRecall),
        mutableSetOf(
          EventMetadataCreator.recallEventMetadata(
            savedRecall.prisonerId,
            savedRecall.recallUuid.toString(),
            sentenceIds = (recall.sentenceIds ?: listOf()).map { it.toString() },
            previousSentenceIds = previousSentenceIds.map { it.toString() },
            null,
            EventType.RECALL_UPDATED,
          ),
        ),
      )
    }
  }

  private fun updateAdjustmentsIfRequired(
    recallUuid: UUID,
    prisonerId: String,
    originalRevocationDate: LocalDate,
    originalRTCDate: LocalDate?,
    newRevocationDate: LocalDate,
    newRTCDate: LocalDate?,
  ) {
    val originalRequiresAdjustment = doesRecallRequireUAL(originalRevocationDate, originalRTCDate)
    val newRequiresAdjustment = doesRecallRequireUAL(newRevocationDate, newRTCDate)
    if (originalRequiresAdjustment && !newRequiresAdjustment) {
      adjustmentsApiClient.getRecallAdjustment(prisonerId, recallUuid)?.id?.let {
        adjustmentsApiClient.deleteAdjustment(it)
      }
    } else if (!originalRequiresAdjustment && newRequiresAdjustment) {
      adjustmentsApiClient.createAdjustments(
        listOf(
          createUalDtoForRecall(
            prisonerId,
            newRevocationDate,
            newRTCDate!!,
            recallUuid,
          ),
        ),
      )
    } else if (originalRequiresAdjustment && (originalRevocationDate != newRevocationDate || originalRTCDate != newRTCDate)) {
      val originalAdjustment = requireNotNull(
        adjustmentsApiClient.getRecallAdjustment(
          prisonerId,
          recallUuid,
        ),
      ) { "Original adjustment is missing" }
      adjustmentsApiClient.updateAdjustment(
        createUalDtoForRecall(
          prisonerId,
          newRevocationDate,
          newRTCDate!!,
          recallUuid,
        ).copy(id = originalAdjustment.id),
      )
    }
  }

  @Transactional
  fun deleteRecall(recallUuid: UUID): RecordResponse<DeleteRecallResponse> {
    val recallToDelete = recallRepository.findOneByRecallUuid(recallUuid)
      ?: throw EntityNotFoundException("Recall not found $recallUuid")

    val isLegacyRecall =
      recallToDelete.recallSentences.all { it.sentence.sentenceType?.sentenceTypeUuid == LegacySentenceService.recallSentenceTypeBucketUuid }

    val eventsToEmit = mutableListOf<EventMetadata>()
    var previousRecall: RecallEntity?
    val ualAdjustmentToDelete: AdjustmentDto? = if (!isLegacyRecall) {
      adjustmentsApiClient.getRecallAdjustment(recallToDelete.prisonerId, recallUuid)
    } else {
      null
    }
    if (isLegacyRecall) {
      eventsToEmit.addAll(deleteLegacyRecallSentenceAndAssociatedRecall(recallToDelete))
    } else {
      val recallHistoryEntity =
        recallHistoryRepository.save(
          RecallHistoryEntity.from(
            recallToDelete,
            RecallEntityStatus.DELETED,
            ChangeSource.DPS,
          ),
        )
      recallToDelete.recallSentences.forEach {
        recallSentenceHistoryRepository.save(
          RecallSentenceHistoryEntity.from(
            recallHistoryEntity,
            it,
            ChangeSource.DPS,
          ),
        )
      }
      previousRecall = recallToDelete.recallSentences.map {
        it.sentence
      }.flatMap { it.recallSentences }
        .map { it.recall }
        .filter { it.recallUuid != recallUuid }
        .maxByOrNull { it.createdAt }

      // deleting the sentence for the legacy recall will delete the recall so it's only required here for DPS
      recallToDelete.statusId = RecallEntityStatus.DELETED
      recallToDelete.recallSentences.forEach {
        recallSentenceRepository.delete(it)
      }
      recallRepository.save(recallToDelete)
      eventsToEmit.add(
        EventMetadataCreator.recallEventMetadata(
          recallToDelete.prisonerId,
          recallToDelete.recallUuid.toString(),
          recallToDelete.recallSentences.map { it.sentence.sentenceUuid.toString() }.distinct(),
          emptyList(),
          previousRecall?.recallUuid?.toString(),
          EventType.RECALL_DELETED,
        ),
      )
    }
    if (ualAdjustmentToDelete?.id != null) {
      adjustmentsApiClient.deleteAdjustment(ualAdjustmentToDelete.id)
    }
    return RecordResponse(
      DeleteRecallResponse.from(recallToDelete),
      (eventsToEmit).toMutableSet(),
    )
  }

  private fun deleteLegacyRecallSentenceAndAssociatedRecall(recallToDelete: RecallEntity): List<EventMetadata> = recallToDelete.recallSentences.flatMap {
    val appearance = it.sentence.charge.appearanceCharges.first().appearance
    if (appearance != null) {
      sentenceService.deleteSentence(
        it.sentence,
        it.sentence.charge,
        recallToDelete.prisonerId,
        appearance.courtCase.caseUniqueIdentifier,
        appearance.appearanceUuid.toString(),
      ).eventsToEmit
    } else {
      emptyList()
    }
  }

  @Transactional(readOnly = true)
  fun findRecallByUuid(recallUuid: UUID): Recall {
    val recall = recallRepository.findOneByRecallUuid(recallUuid)
      ?: throw EntityNotFoundException("No recall exists for the passed in UUID")
    val recallSentences = recallSentenceRepository.findByRecallId(recall.id).orEmpty()
    val adjustment =
      if (recall.revocationDate != null && doesRecallRequireUAL(recall.revocationDate!!, recall.returnToCustodyDate)) {
        adjustmentsApiClient.getRecallAdjustment(recall.prisonerId, recall.recallUuid)
      } else {
        null
      }
    return Recall.from(recall, recallSentences, adjustment)
  }

  @Transactional(readOnly = true)
  fun findRecallsByPrisonerId(prisonerId: String): List<Recall> {
    val recallAdjustments = adjustmentsApiClient.getAdjustments(prisonerId).filter { it.recallId != null }
    return recallRepository.findByPrisonerIdAndStatusId(prisonerId).map { recall ->
      val recallSentences = recallSentenceRepository.findByRecallId(recall.id).orEmpty()
      Recall.from(recall, recallSentences, recallAdjustments.find { it.recallId == recall.recallUuid.toString() })
    }
  }

  private fun createUalDtoForRecall(
    prisonerId: String,
    revocationDate: LocalDate,
    rtcDate: LocalDate,
    recallUuid: UUID,
  ): AdjustmentDto = AdjustmentDto(
    id = null,
    person = prisonerId,
    adjustmentType = "UNLAWFULLY_AT_LARGE",
    fromDate = revocationDate.plusDays(1),
    toDate = rtcDate.minusDays(1),
    days = null,
    recallId = recallUuid.toString(),
    unlawfullyAtLarge = UnlawfullyAtLargeDto(),
  )

  @Transactional(readOnly = true)
  fun isRecallPossible(request: IsRecallPossibleRequest): IsRecallPossibleResponse {
    val sentences = sentenceRepository.findBySentenceUuidIn(request.sentenceIds)
    val isPossibleForEachSentence = sentences.map { isRecallPossibleForSentence(it, request.recallType) }
    val isPossible = isPossibleForEachSentence.find { it == IsRecallPossible.UNKNOWN_PRE_RECALL_MAPPING }
      ?: isPossibleForEachSentence.find { it == IsRecallPossible.RECALL_TYPE_AND_SENTENCE_MAPPING_NOT_POSSIBLE }
      ?: IsRecallPossible.YES

    return IsRecallPossibleResponse(isPossible)
  }

  private fun isRecallPossibleForSentence(sentence: SentenceEntity, recallType: RecallType): IsRecallPossible {
    if (sentence.sentenceType!!.sentenceTypeUuid == LegacySentenceService.recallSentenceTypeBucketUuid) {
      return isRecallPossibleForLegacyRecall(sentence, recallType)
    }
    return isRecallPossibleForClassification(sentence.sentenceType!!.classification, recallType)
  }

  private fun isRecallPossibleForClassification(
    classification: SentenceTypeClassification,
    recallType: RecallType,
  ): IsRecallPossible = when (classification) {
    SentenceTypeClassification.STANDARD -> IsRecallPossible.YES
    SentenceTypeClassification.EXTENDED, SentenceTypeClassification.INDETERMINATE -> if (recallType == RecallType.LR) IsRecallPossible.YES else IsRecallPossible.RECALL_TYPE_AND_SENTENCE_MAPPING_NOT_POSSIBLE
    SentenceTypeClassification.SOPC -> if (listOf(RecallType.LR, RecallType.FTR_28).contains(recallType)) IsRecallPossible.YES else IsRecallPossible.RECALL_TYPE_AND_SENTENCE_MAPPING_NOT_POSSIBLE
    else -> IsRecallPossible.RECALL_TYPE_AND_SENTENCE_MAPPING_NOT_POSSIBLE
  }

  /* Is the new DPS recall possible for a sentence which has previously been recalled in NOMIS. */
  private fun isRecallPossibleForLegacyRecall(
    sentence: SentenceEntity,
    recallType: RecallType,
  ): IsRecallPossible {
    val latestRecall = sentence.latestRecall()!!
    val recallLegacyData =
      latestRecall.let { recall -> sentence.recallSentences.find { it.recall.id == recall.id }?.legacyData }!!
    val sentenceCalcType = recallLegacyData.sentenceCalcType

    val classification =
      LegacyRecallService.Companion.classificationToLegacySentenceTypeMap.mapNotNull { (classification, types) ->
        if (types.contains(sentenceCalcType)) {
          classification
        } else {
          null
        }
      }.firstOrNull()

    if (classification != null) {
      return isRecallPossibleForClassification(classification, recallType)
    } else {
      // Legacy recall can match more than one sentence type, but will all be standard sentences.
      if (recallType == RecallType.LR && unknownPreRecallSentenceTypes.contains(sentenceCalcType)) {
        return IsRecallPossible.UNKNOWN_PRE_RECALL_MAPPING
      }
    }
    return IsRecallPossible.YES
  }

  companion object {
    val unknownPreRecallSentenceTypes = listOf(
      "CUR",
      "CUR_ORA",
      "FTR",
      "FTR_HDC",
      "FTR_HDC_ORA",
      "HDR",
    )
  }
}
