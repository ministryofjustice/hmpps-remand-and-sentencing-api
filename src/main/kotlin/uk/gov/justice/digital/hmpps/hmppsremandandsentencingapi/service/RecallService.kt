package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateRecall
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.Recall
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.SaveRecallResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.EventType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.RecordResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.util.EventMetadataCreator
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.RecallEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.RecallSentenceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.RecallRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.RecallSentenceRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.RecallTypeRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.SentenceRepository
import java.util.UUID

@Service
class RecallService(
  private val recallRepository: RecallRepository,
  private val recallSentenceRepository: RecallSentenceRepository,
  private val recallTypeRepository: RecallTypeRepository,
  private val sentenceRepository: SentenceRepository,
) {
  @Transactional
  fun createRecall(createRecall: CreateRecall): RecordResponse<SaveRecallResponse> {
    val recallType = recallTypeRepository.findOneByCode(createRecall.recallTypeCode)
    val recall = recallRepository.save(RecallEntity.placeholderEntity(createRecall, recallType!!))
    // TODO Do we need a domain event for these?
    val recallSentences: List<RecallSentenceEntity> = createRecall.sentenceIds
      ?.map { sentenceRepository.findBySentenceUuid(it)!! }
      ?.map { recallSentenceRepository.save(RecallSentenceEntity.placeholderEntity(recall, it)) }
      ?: emptyList()

    return RecordResponse(
      SaveRecallResponse.from(recall),
      mutableSetOf(
        EventMetadataCreator.recallEventMetadata(
          recall.prisonerId,
          recall.recallUuid.toString(),
          EventType.RECALL_INSERTED,
        ),
      ),
    )
  }

  @Transactional
  fun updateRecall(recallUuid: UUID, recall: CreateRecall): RecordResponse<SaveRecallResponse> {
    val recallType = recallTypeRepository.findOneByCode(recall.recallTypeCode)!!
    val recallToUpdate = recallRepository.findOneByRecallUuid(recallUuid)

    if (recallToUpdate == null) {
      val savedRecall = recallRepository.save(RecallEntity.placeholderEntity(recall, recallType, recallUuid))

      return RecordResponse(
        SaveRecallResponse.from(savedRecall),
        mutableSetOf(
          EventMetadataCreator.recallEventMetadata(
            savedRecall.prisonerId,
            savedRecall.recallUuid.toString(),
            EventType.RECALL_INSERTED,
          ),
        ),
      )
    } else {
      val savedRecall = recallRepository.save(
        recallToUpdate.copy(
          revocationDate = recall.revocationDate,
          returnToCustodyDate = recall.returnToCustodyDate,
          recallType = recallType,
        ),
      )

      return RecordResponse(
        SaveRecallResponse.from(savedRecall),
        mutableSetOf(
          EventMetadataCreator.recallEventMetadata(
            savedRecall.prisonerId,
            savedRecall.recallUuid.toString(),
            EventType.RECALL_UPDATED,
          ),
        ),
      )
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
  fun findRecallsByPrisonerId(prisonerId: String): List<Recall> = recallRepository.findByPrisonerId(prisonerId).map {
    val recallSentences = recallSentenceRepository.findByRecallId(it.id).orEmpty()
    Recall.from(it, recallSentences)
  }
}
