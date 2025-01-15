package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateRecall
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.Recall
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.SaveRecallResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.RecallEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.RecallRepository
import java.util.UUID

@Service
class RecallService(private val recallRepository: RecallRepository) {
  @Transactional
  fun createRecall(createRecall: CreateRecall): SaveRecallResponse {
    val recall = recallRepository.save(RecallEntity.placeholderEntity(createRecall))
    return SaveRecallResponse.from(recall)
  }

  @Transactional
  fun updateRecall(recallUniqueIdentifier: UUID, recall: CreateRecall): SaveRecallResponse {
    val recallToUpdate = recallRepository.findOneByRecallUniqueIdentifier(recallUniqueIdentifier)
      ?: recallRepository.save(RecallEntity.placeholderEntity(recall, recallUniqueIdentifier))

    val savedRecall = recallRepository.save(
      recallToUpdate.copy(
        recallDate = recall.recallDate,
        returnToCustodyDate = recall.returnToCustodyDate,
        recallType = recall.recallType,
      ),
    )

    return SaveRecallResponse.from(savedRecall)
  }

  @Transactional(readOnly = true)
  fun findRecallByUuid(recallUniqueIdentifier: UUID): Recall {
    val recall = recallRepository.findOneByRecallUniqueIdentifier(recallUniqueIdentifier)
      ?: throw EntityNotFoundException("No recall exists for the passed in UUID")
    return Recall.from(recall)
  }

  @Transactional(readOnly = true)
  fun findRecallsByPrisonerId(prisonerId: String): List<Recall> =
    recallRepository.findByPrisonerId(prisonerId).map { Recall.from(it) }

  @Transactional(readOnly = true)
  fun findLatestRecallByPrisonerId(prisonerId: String): Recall {
    val latest = recallRepository.findFirstByPrisonerIdOrderByRecallDateDescCreatedAtDesc(prisonerId)
      ?: throw EntityNotFoundException("No recalls recorded for given prisonerId")
    return Recall.from(latest)
  }
}
