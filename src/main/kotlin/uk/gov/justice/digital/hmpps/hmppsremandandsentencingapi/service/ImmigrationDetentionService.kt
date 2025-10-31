package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateImmigrationDetention
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.DeleteImmigrationDetentionResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.ImmigrationDetention
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.SaveImmigrationDetentionResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.RecordResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.ImmigrationDetentionEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.audit.ImmigrationDetentionHistoryEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ImmigrationDetentionEntityStatus.ACTIVE
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ImmigrationDetentionEntityStatus.DELETED
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ImmigrationDetentionEntityStatus.EDITED
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.ImmigrationDetentionRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.audit.ImmigrationDetentionHistoryRepository
import java.time.ZonedDateTime
import java.util.UUID

@Service
class ImmigrationDetentionService(
  private val immigrationDetentionRepository: ImmigrationDetentionRepository,
  private val immigrationDetentionHistoryRepository: ImmigrationDetentionHistoryRepository,
) {

  @Transactional
  fun createImmigrationDetention(
    immigrationDetention: CreateImmigrationDetention,
    immigrationDetentionUuid: UUID? = null,
  ): RecordResponse<SaveImmigrationDetentionResponse> {
    val savedImmigrationDetention = immigrationDetentionRepository.save(
      ImmigrationDetentionEntity.fromDPS(
        immigrationDetention,
        immigrationDetentionUuid,
      ),
    )
    return RecordResponse(SaveImmigrationDetentionResponse.from(savedImmigrationDetention), mutableSetOf())
  }

  @Transactional
  fun updateImmigrationDetention(
    immigrationDetention: CreateImmigrationDetention,
    immigrationDetentionUuid: UUID,
  ): RecordResponse<SaveImmigrationDetentionResponse> {
    val immigrationDetentionToUpdate =
      immigrationDetentionRepository.findOneByImmigrationDetentionUuid(immigrationDetentionUuid)

    return if (immigrationDetentionToUpdate == null) {
      createImmigrationDetention(immigrationDetention, immigrationDetentionUuid)
    } else {
      immigrationDetentionHistoryRepository.save(
        ImmigrationDetentionHistoryEntity.from(
          immigrationDetentionToUpdate,
          EDITED,
        ),
      )
      immigrationDetentionToUpdate.apply {
        homeOfficeReferenceNumber = immigrationDetention.homeOfficeReferenceNumber
        recordDate = immigrationDetention.recordDate
        noLongerOfInterestReason = immigrationDetention.noLongerOfInterestReason
        noLongerOfInterestComment = immigrationDetention.noLongerOfInterestComment
        immigrationDetentionRecordType = immigrationDetention.immigrationDetentionRecordType
        updatedAt = ZonedDateTime.now()
        updatedBy = immigrationDetention.createdByUsername
        updatedPrison = immigrationDetention.createdByPrison
      }
      val savedImmigrationDetention = immigrationDetentionRepository.save(immigrationDetentionToUpdate)

      return RecordResponse(SaveImmigrationDetentionResponse.from(savedImmigrationDetention), mutableSetOf())
    }
  }

  @Transactional
  fun deleteImmigrationDetention(immigrationDetentionUuid: UUID): RecordResponse<DeleteImmigrationDetentionResponse> {
    val immigrationDetentionToDelete =
      immigrationDetentionRepository.findOneByImmigrationDetentionUuid(immigrationDetentionUuid)
        ?: throw EntityNotFoundException("Immigration Detention not found $immigrationDetentionUuid")

    immigrationDetentionToDelete.statusId = DELETED

    immigrationDetentionHistoryRepository.save(
      ImmigrationDetentionHistoryEntity.from(
        immigrationDetentionToDelete,
        DELETED,
      ),
    )

    return RecordResponse(
      DeleteImmigrationDetentionResponse.from(immigrationDetentionToDelete),
      mutableSetOf(),
    )
  }

  @Transactional(readOnly = true)
  fun findImmigrationDetentionRecallByUuid(immigrationDetentionUuid: UUID): ImmigrationDetention {
    val immigrationDetention =
      immigrationDetentionRepository.findOneByImmigrationDetentionUuid(immigrationDetentionUuid)
        ?: throw EntityNotFoundException("No immigration detention exists for the passed in UUID")
    return ImmigrationDetention.from(immigrationDetention)
  }

  @Transactional(readOnly = true)
  fun findImmigrationDetentionByPrisonerId(prisonerId: String): List<ImmigrationDetention> = immigrationDetentionRepository.findByPrisonerIdAndStatusId(prisonerId, ACTIVE).map {
    ImmigrationDetention.from(it)
  }
}
