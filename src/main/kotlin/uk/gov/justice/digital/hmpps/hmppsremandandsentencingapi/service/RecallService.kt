package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateRecall
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.DeleteRecallResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.Recall
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.SaveRecallResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.EventType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.RecordResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.util.EventMetadataCreator
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.RecallEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.RecallSentenceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.RecallRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.RecallSentenceRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.RecallTypeRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.SentenceRepository
import java.time.ZonedDateTime
import java.util.UUID

@Service
class RecallService(
  private val recallRepository: RecallRepository,
  private val recallSentenceRepository: RecallSentenceRepository,
  private val recallTypeRepository: RecallTypeRepository,
  private val sentenceRepository: SentenceRepository,
) {
  @Transactional
  fun createRecall(createRecall: CreateRecall, recallUuid: UUID? = null): RecordResponse<SaveRecallResponse> {
    val recallType = recallTypeRepository.findOneByCode(createRecall.recallTypeCode)
    val recall = recallRepository.save(RecallEntity.placeholderEntity(createRecall, recallType!!, recallUuid))
    // Temporarily nullable because CRDS data doesn't have sentence Ids
    val recallSentences: List<RecallSentenceEntity>? =
      createRecall.sentenceIds?.let { sentenceIds ->
        sentenceRepository.findBySentenceUuidIn(sentenceIds)
          .map { recallSentenceRepository.save(RecallSentenceEntity.placeholderEntity(recall, it)) }
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
      val recallTypeEntity = recallTypeRepository.findOneByCode(recall.recallTypeCode)!!

      val previousSentenceIds = recallToUpdate.recallSentences.map { it.sentence.sentenceUuid }

      val sentencesToDelete = previousSentenceIds.filterNot { (recall.sentenceIds ?: emptyList()).contains(it) }
      val sentencesToCreate = recall.sentenceIds?.filterNot { previousSentenceIds.contains(it) } ?: listOf()

      recallToUpdate.recallSentences.filter { sentencesToDelete.contains(it.sentence.sentenceUuid) }.forEach {
        recallSentenceRepository.delete(it)
      }
      sentenceRepository.findBySentenceUuidIn(sentencesToCreate)
        .map { recallSentenceRepository.save(RecallSentenceEntity.placeholderEntity(recallToUpdate, it)) }

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

    recallToDelete.statusId = EntityStatus.DELETED

    recallToDelete.recallSentences.forEach {
      recallSentenceRepository.delete(it)
    }

    val previousRecall = recallToDelete.recallSentences.map {
      it.sentence
    }.flatMap { it.recallSentences }
      .map { it.recall }
      .filter { it.recallUuid != recallUuid }
      .maxByOrNull { it.createdAt }

    // TODO RCLL-277 Recall audit data.
    // TODO RCLL-386 Delete sentence if legacy recall.

    return RecordResponse(
      DeleteRecallResponse.from(recallToDelete),
      mutableSetOf(
        EventMetadataCreator.recallEventMetadata(
          recallToDelete.prisonerId,
          recallToDelete.recallUuid.toString(),
          recallToDelete.recallSentences.map { it.sentence.sentenceUuid.toString() }.distinct(),
          emptyList(),
          previousRecall?.recallUuid?.toString(),
          EventType.RECALL_DELETED,
        ),
      ),
    )
  }

  @Transactional(readOnly = true)
  fun findRecallByUuid(recallUuid: UUID): Recall {
    val recall = recallRepository.findOneByRecallUuid(recallUuid)
      ?: throw EntityNotFoundException("No recall exists for the passed in UUID")
    val recallSentences = recallSentenceRepository.findByRecallId(recall.id).orEmpty()
    return Recall.from(recall, recallSentences)
  }

  @Transactional(readOnly = true)
  fun findRecallsByPrisonerId(prisonerId: String): List<Recall> = recallRepository.findByPrisonerIdAndStatusId(prisonerId).map {
    val recallSentences = recallSentenceRepository.findByRecallId(it.id).orEmpty()
    Recall.from(it, recallSentences)
  }
}
