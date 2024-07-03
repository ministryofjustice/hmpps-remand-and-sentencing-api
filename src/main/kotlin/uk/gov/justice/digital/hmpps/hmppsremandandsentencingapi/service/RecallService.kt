package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateRecall
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateRecallResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.Recall
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.RecallEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.RecallRepository
import java.util.UUID

@Service
class RecallService(private val recallRepository: RecallRepository) {
  @Transactional
  fun createRecall(createRecall: CreateRecall): CreateRecallResponse {
    val recall = recallRepository.save(RecallEntity.placeholderEntity(createRecall))
    return CreateRecallResponse.from(recall)
  }

  @Transactional(readOnly = true)
  fun findRecallByUuid(recallUniqueIdentifier: UUID): Recall {
    val recall = recallRepository.findOneByRecallUniqueIdentifier(recallUniqueIdentifier) ?: throw EntityNotFoundException("No recall exists for the passed in UUID")
    return Recall.from(recall)
  }

  @Transactional(readOnly = true)
  fun findRecallsByPrisonerId(prisonerId: String): List<Recall> =
    recallRepository.findByPrisonerId(prisonerId).map { Recall.from(it) }
}
