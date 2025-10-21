package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateRecall
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.DeleteRecallResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.Recall
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.SaveRecallResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.EventMetadata
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.EventType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.RecordResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.util.EventMetadataCreator
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.RecallEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.RecallSentenceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.audit.RecallHistoryEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.audit.RecallSentenceHistoryEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.RecallEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.RecallRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.RecallSentenceRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.RecallTypeRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.SentenceRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.audit.RecallHistoryRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.audit.RecallSentenceHistoryRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.service.LegacySentenceService
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
      val recallHistoryEntity =
        recallHistoryRepository.save(RecallHistoryEntity.from(recallToUpdate, RecallEntityStatus.EDITED))
      recallToUpdate.recallSentences.forEach {
        recallSentenceHistoryRepository.save(RecallSentenceHistoryEntity.from(recallHistoryEntity, it).apply {})
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
      }
      val savedRecall = recallRepository.save(recallToUpdate)

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

  @Transactional
  fun deleteRecall(recallUuid: UUID): RecordResponse<DeleteRecallResponse> {
    val recallToDelete = recallRepository.findOneByRecallUuid(recallUuid)
      ?: throw EntityNotFoundException("Recall not found $recallUuid")

    val isLegacyRecall =
      recallToDelete.recallSentences.all { it.sentence.sentenceType?.sentenceTypeUuid == LegacySentenceService.recallSentenceTypeBucketUuid }

    val eventsToEmit = mutableListOf<EventMetadata>()
    var previousRecall: RecallEntity? = null
    if (isLegacyRecall) {
      eventsToEmit.addAll(deleteLegacyRecallSentenceAndAssociatedRecall(recallToDelete))
    } else {
      val recallHistoryEntity =
        recallHistoryRepository.save(RecallHistoryEntity.from(recallToDelete, RecallEntityStatus.DELETED))
      recallToDelete.recallSentences.forEach {
        recallSentenceHistoryRepository.save(
          RecallSentenceHistoryEntity.from(
            recallHistoryEntity,
            it,
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
    return Recall.from(recall, recallSentences)
  }

  @Transactional(readOnly = true)
  fun findRecallsByPrisonerId(prisonerId: String): List<Recall> = recallRepository.findByPrisonerIdAndStatusId(prisonerId, RecallEntityStatus.ACTIVE).map {
    val recallSentences = recallSentenceRepository.findByRecallId(it.id).orEmpty()
    Recall.from(it, recallSentences)
  }
}
