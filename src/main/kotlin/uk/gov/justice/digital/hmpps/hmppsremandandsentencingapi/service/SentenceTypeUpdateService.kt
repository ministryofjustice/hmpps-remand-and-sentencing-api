package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.SentenceTypeUpdate
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.UpdateSentenceTypeRequest
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.UpdateSentenceTypeResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.SentenceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.SentenceTypeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.audit.SentenceHistoryEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ReferenceEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.CourtCaseRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.SentenceTypeRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.audit.SentenceHistoryRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.service.LegacySentenceService
import java.util.UUID

@Service
class SentenceTypeUpdateService(
  private val courtCaseRepository: CourtCaseRepository,
  private val sentenceTypeRepository: SentenceTypeRepository,
  private val sentenceHistoryRepository: SentenceHistoryRepository,
  private val serviceUserService: ServiceUserService,
) {
  companion object {
    private const val MAX_UPDATES_PER_REQUEST = 50
  }

  private data class ValidatedUpdate(
    val updateRequest: SentenceTypeUpdate,
    val sentenceEntity: SentenceEntity,
    val newSentenceTypeEntity: SentenceTypeEntity,
  )

  @Transactional
  fun updateSentenceTypes(
    courtCaseUuid: UUID,
    request: UpdateSentenceTypeRequest,
  ): UpdateSentenceTypeResponse {
    // Validate request size
    if (request.updates.size > MAX_UPDATES_PER_REQUEST) {
      throw IllegalArgumentException("Maximum of $MAX_UPDATES_PER_REQUEST sentence updates allowed per request")
    }

    // Validate court case exists and is active
    val courtCase = courtCaseRepository.findByCaseUniqueIdentifier(courtCaseUuid.toString())
      ?: throw EntityNotFoundException("Court case with UUID $courtCaseUuid not found")

    if (courtCase.statusId == EntityStatus.DELETED) {
      throw IllegalStateException("Court case with UUID $courtCaseUuid is deleted")
    }

    val courtCaseSentences = courtCaseRepository.findSentencesByCourtCaseUuid(courtCaseUuid.toString())
      .associateBy { it.sentenceUuid }

    val validatedUpdates = validateUpdates(request.updates, courtCaseSentences)

    // Process all updates
    val updatedSentenceUuids = mutableListOf<UUID>()
    val username = serviceUserService.getUsername()

    validatedUpdates.forEach { validatedUpdate ->
      val sentence = validatedUpdate.sentenceEntity
      // Update sentence type
      sentence.sentenceType = validatedUpdate.newSentenceTypeEntity
      sentence.updatedBy = username

      sentenceHistoryRepository.save(SentenceHistoryEntity.from(sentence))
      
      updatedSentenceUuids.add(validatedUpdate.updateRequest.sentenceUuid)
    }

    return UpdateSentenceTypeResponse(
      updatedSentenceUuids = updatedSentenceUuids,
    )
  }

  private fun validateUpdates(
    updates: List<SentenceTypeUpdate>,
    courtCaseSentences: Map<UUID, SentenceEntity>,
  ): List<ValidatedUpdate> {
    // Collect all unique sentence type UUIDs from the request
    val sentenceTypeUuids = updates.map { it.sentenceTypeId }.distinct()

    val sentenceTypesByUuid = sentenceTypeRepository.findBySentenceTypeUuidIn(sentenceTypeUuids)
      .associateBy { it.sentenceTypeUuid }

    return updates.map { update ->
      // Check sentence exists and belongs to court case
      val sentence = courtCaseSentences[update.sentenceUuid]
        ?: throw EntityNotFoundException("Sentence with UUID ${update.sentenceUuid} not found in court case")

      // Check sentence status
      if (sentence.statusId == EntityStatus.DELETED) {
        throw IllegalStateException("Sentence with UUID ${update.sentenceUuid} is deleted")
      }

      // Check sentence has "unknown pre-recall sentence" type
      if (sentence.sentenceType?.sentenceTypeUuid != LegacySentenceService.recallSentenceTypeBucketUuid) {
        throw IllegalStateException("Sentence ${update.sentenceUuid} does not have type 'unknown pre-recall sentence'")
      }

      // Validate new sentence type against the pre-fetched map
      val newSentenceType = sentenceTypesByUuid[update.sentenceTypeId]
        ?: throw EntityNotFoundException("Sentence type '${update.sentenceTypeId}' is not a valid sentence type")

      if (newSentenceType.status == ReferenceEntityStatus.INACTIVE) {
        throw IllegalStateException("Sentence type '${update.sentenceTypeId}' is not active")
      }

      ValidatedUpdate(update, sentence, newSentenceType)
    }
  }
}
