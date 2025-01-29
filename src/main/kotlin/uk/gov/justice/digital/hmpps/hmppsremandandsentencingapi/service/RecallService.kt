package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateRecall
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.Recall
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.SaveRecallResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.event.EventSource
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.RecallEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.RecallRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.RecallTypeRepository
import java.util.UUID

@Service
class RecallService(
  private val recallRepository: RecallRepository,
  private val recallTypeRepository: RecallTypeRepository,
  private val recallDomainEventService: RecallDomainEventService,
) {
  @Transactional
  fun createRecall(createRecall: CreateRecall): SaveRecallResponse {
    val recallType = recallTypeRepository.findOneByCode(createRecall.recallTypeCode)
    val recall = createRecallAndEmitEvent(RecallEntity.placeholderEntity(createRecall, recallType!!))
    return SaveRecallResponse.from(recall)
  }

  @Transactional
  fun updateRecall(recallUuid: UUID, recall: CreateRecall): SaveRecallResponse {
    val recallType = recallTypeRepository.findOneByCode(recall.recallTypeCode)!!
    val recallToUpdate = recallRepository.findOneByRecallUuid(recallUuid)
      ?: createRecallAndEmitEvent(RecallEntity.placeholderEntity(recall, recallType, recallUuid))

    val savedRecall = recallRepository.save(
      recallToUpdate.copy(
        revocationDate = recall.revocationDate,
        returnToCustodyDate = recall.returnToCustodyDate,
        recallType = recallType,
      ),
    )
    recallDomainEventService.update(recall.prisonerId, savedRecall.recallUuid.toString(), EventSource.DPS)

    return SaveRecallResponse.from(savedRecall)
  }

  private fun createRecallAndEmitEvent(recallToSave: RecallEntity): RecallEntity {
    val recall = recallRepository.save(recallToSave)
    recallDomainEventService.create(recall.prisonerId, recall.recallUuid.toString(), EventSource.DPS)
    return recall
  }

  @Transactional(readOnly = true)
  fun findRecallByUuid(recallUuid: UUID): Recall {
    val recall = recallRepository.findOneByRecallUuid(recallUuid)
      ?: throw EntityNotFoundException("No recall exists for the passed in UUID")
    return Recall.from(recall)
  }

  @Transactional(readOnly = true)
  fun findRecallsByPrisonerId(prisonerId: String): List<Recall> = recallRepository.findByPrisonerId(prisonerId).map { Recall.from(it) }
}
