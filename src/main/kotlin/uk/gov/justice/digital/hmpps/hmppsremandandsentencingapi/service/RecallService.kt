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
  fun updateRecall(recallUuid: UUID, recall: CreateRecall): SaveRecallResponse {
    val recallToUpdate = recallRepository.findOneByRecallUuid(recallUuid)
      ?: recallRepository.save(RecallEntity.placeholderEntity(recall, recallUuid))

    val savedRecall = recallRepository.save(
      recallToUpdate.copy(
        revocationDate = recall.revocationDate,
        returnToCustodyDate = recall.returnToCustodyDate,
        recallType = recall.recallType,
      ),
    )

    return SaveRecallResponse.from(savedRecall)
  }

  @Transactional(readOnly = true)
  fun findRecallByUuid(recallUuid: UUID): Recall {
    val recall = recallRepository.findOneByRecallUuid(recallUuid)
      ?: throw EntityNotFoundException("No recall exists for the passed in UUID")
    return Recall.from(recall)
  }

  @Transactional(readOnly = true)
  fun findRecallsByPrisonerId(prisonerId: String): List<Recall> =
    recallRepository.findByPrisonerId(prisonerId).map { Recall.from(it) }
}
