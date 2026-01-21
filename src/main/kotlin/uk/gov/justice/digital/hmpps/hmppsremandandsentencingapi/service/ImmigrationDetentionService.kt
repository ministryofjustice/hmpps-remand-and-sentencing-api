package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateCharge
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateCourtAppearance
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateCourtCase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateImmigrationDetention
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.DeleteImmigrationDetentionResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.ImmigrationDetention
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.SaveImmigrationDetentionResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.EventMetadata
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.EventType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.RecordResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.util.EventMetadataCreator
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.CourtAppearanceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.ImmigrationDetentionEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.audit.ImmigrationDetentionHistoryEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ImmigrationDetentionEntityStatus.ACTIVE
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ImmigrationDetentionEntityStatus.DELETED
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.CourtAppearanceRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.ImmigrationDetentionRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.audit.ImmigrationDetentionHistoryRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.legacy.CourtCaseReferenceService
import java.time.ZonedDateTime
import java.util.UUID

@Service
class ImmigrationDetentionService(
  private val immigrationDetentionRepository: ImmigrationDetentionRepository,
  private val immigrationDetentionHistoryRepository: ImmigrationDetentionHistoryRepository,
  private val courtCaseService: CourtCaseService,
  private val courtAppearanceService: CourtAppearanceService,
  private val chargeOutcomeService: ChargeOutcomeService,
  private val appearanceOutcomeService: AppearanceOutcomeService,
  private val courtAppearanceRepository: CourtAppearanceRepository,
  private val courtCaseReferenceService: CourtCaseReferenceService,
) {

  @Transactional
  fun createImmigrationDetention(
    immigrationDetention: CreateImmigrationDetention,
    immigrationDetentionUuid: UUID? = null,
  ): RecordResponse<SaveImmigrationDetentionResponse> {
    val eventsToEmit = mutableSetOf<EventMetadata>()

    val (createdCourtCase, events) = courtCaseService.createCourtCase(
      CreateCourtCase(
        prisonerId = immigrationDetention.prisonerId,
        prisonId = immigrationDetention.createdByPrison,
        appearances = emptyList(),
        legacyData = null,
      ),
    )
    eventsToEmit.addAll(events)

    var courtAppearanceUuid: UUID? = null

    courtAppearanceService.createCourtAppearance(
      createCourtAppearanceFromImmigrationDetention(
        immigrationDetention,
        courtCaseUuid = createdCourtCase.caseUniqueIdentifier,
      ),
    )?.let { (courtAppearance, events) ->
      eventsToEmit.addAll(events)
      courtAppearanceUuid = courtAppearance.appearanceUuid
    }

    val updatedCourtCaseReferences = courtCaseReferenceService.updateCourtCaseEntity(createdCourtCase)
    if (updatedCourtCaseReferences.hasUpdated) {
      eventsToEmit.add(
        EventMetadataCreator.courtCaseEventMetadata(
          updatedCourtCaseReferences.prisonerId,
          updatedCourtCaseReferences.courtCaseId,
          EventType.LEGACY_COURT_CASE_REFERENCES_UPDATED,
        ),
      )
    }

    val savedImmigrationDetention = immigrationDetentionRepository.save(
      ImmigrationDetentionEntity.fromDPS(
        immigrationDetention,
        immigrationDetentionUuid,
        courtAppearanceUuid,
      ),
    )

    return RecordResponse(
      SaveImmigrationDetentionResponse.from(savedImmigrationDetention),
      eventsToEmit,
    )
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
      val courtAppearance =
        courtAppearanceRepository.findByAppearanceUuid(immigrationDetentionToUpdate.courtAppearanceUuid!!)
      val eventsToEmit = mutableSetOf<EventMetadata>()
      courtAppearanceService.createCourtAppearanceByAppearanceUuid(
        createCourtAppearanceFromImmigrationDetention(
          immigrationDetention,
          courtAppearance?.courtCase?.caseUniqueIdentifier,
          courtAppearance?.appearanceCharges?.firstOrNull()?.charge?.chargeUuid,
        ),
        courtAppearance?.appearanceUuid!!,
      )?.let { courtAppearanceRecord ->
        eventsToEmit.addAll(
          courtAppearanceRecord.eventsToEmit,
        )
        val updatedCourtCaseReferences = courtCaseReferenceService.updateCourtCaseEntity(courtAppearanceRecord.record.courtCase)
        if (updatedCourtCaseReferences.hasUpdated) {
          eventsToEmit.add(
            EventMetadataCreator.courtCaseEventMetadata(
              updatedCourtCaseReferences.prisonerId,
              updatedCourtCaseReferences.courtCaseId,
              EventType.LEGACY_COURT_CASE_REFERENCES_UPDATED,
            ),
          )
        }
      }

      return RecordResponse(SaveImmigrationDetentionResponse.from(savedImmigrationDetention), eventsToEmit)
    }
  }

  @Transactional
  fun deleteImmigrationDetention(immigrationDetentionUuid: UUID): RecordResponse<DeleteImmigrationDetentionResponse> {
    val immigrationDetentionToDelete =
      immigrationDetentionRepository.findOneByImmigrationDetentionUuid(immigrationDetentionUuid)
        ?: throw EntityNotFoundException("Immigration Detention not found $immigrationDetentionUuid")

    val eventsToEmit = mutableSetOf<EventMetadata>()
    val deletedCourtAppearanceEntity = courtAppearanceService.delete(immigrationDetentionToDelete.courtAppearanceUuid!!).records
    eventsToEmit.addAll(deletedCourtAppearanceEntity.eventsToEmit)
    val updatedCourtCaseReferences = courtCaseReferenceService.updateCourtCaseEntity(deletedCourtAppearanceEntity.record.courtCase)
    if (updatedCourtCaseReferences.hasUpdated) {
      eventsToEmit.add(
        EventMetadataCreator.courtCaseEventMetadata(
          updatedCourtCaseReferences.prisonerId,
          updatedCourtCaseReferences.courtCaseId,
          EventType.LEGACY_COURT_CASE_REFERENCES_UPDATED,
        ),
      )
    }
    immigrationDetentionToDelete.statusId = DELETED

    immigrationDetentionHistoryRepository.save(
      ImmigrationDetentionHistoryEntity.from(immigrationDetentionToDelete),
    )

    return RecordResponse(
      DeleteImmigrationDetentionResponse.from(immigrationDetentionToDelete),
      eventsToEmit,
    )
  }

  @Transactional(readOnly = true)
  fun findImmigrationDetentionByUuid(immigrationDetentionUuid: UUID): ImmigrationDetention {
    val immigrationDetention =
      immigrationDetentionRepository.findOneByImmigrationDetentionUuid(immigrationDetentionUuid)
        ?: throw EntityNotFoundException("No immigration detention exists for the passed in UUID")
    return ImmigrationDetention.from(immigrationDetention)
  }

  @Transactional(readOnly = true)
  fun findImmigrationDetentionByPrisonerId(prisonerId: String): List<ImmigrationDetention> {
    val dpsRecords = immigrationDetentionRepository.findByPrisonerIdAndStatusId(prisonerId, ACTIVE)
      .map { ImmigrationDetention.from(it) }

    val nomisRecords = courtAppearanceRepository.findNomisImmigrationDetentionRecordsForPrisoner(prisonerId)
      .map { courtAppearance: CourtAppearanceEntity -> ImmigrationDetention.fromCourtAppearance(courtAppearance, prisonerId) }

    return dpsRecords + nomisRecords
  }

  @Transactional(readOnly = true)
  fun findLatestImmigrationDetentionByPrisonerId(prisonerId: String): ImmigrationDetention {
    val dpsRecords = immigrationDetentionRepository.findTop1ByPrisonerIdAndStatusIdOrderByCreatedAtDesc(prisonerId)
      ?.let { listOf(ImmigrationDetention.from(it)) }
      ?: emptyList()

    val nomisRecords = courtAppearanceRepository.findNomisImmigrationDetentionRecordsForPrisoner(prisonerId).map {
      ImmigrationDetention.fromCourtAppearance(it, prisonerId)
    }

    val allRecords = (dpsRecords + nomisRecords).sortedByDescending { it.createdAt }
    return allRecords.firstOrNull()
      ?: throw EntityNotFoundException("No immigration detention records exist for the prisoner ID: $prisonerId")
  }

  private fun createCourtAppearanceFromImmigrationDetention(
    immigrationDetention: CreateImmigrationDetention,
    courtCaseUuid: String? = null,
    chargeUuid: UUID? = null,
  ): CreateCourtAppearance {
    val appearanceOutcome =
      appearanceOutcomeService.findByUuid(immigrationDetention.appearanceOutcomeUuid)
        ?: throw EntityNotFoundException("No appearance outcome found for UUID: ${immigrationDetention.appearanceOutcomeUuid}")

    return CreateCourtAppearance(
      appearanceDate = immigrationDetention.recordDate,
      courtCaseUuid = courtCaseUuid,
      outcomeUuid = immigrationDetention.appearanceOutcomeUuid,
      courtCode = "IMM",
      courtCaseReference = null,
      warrantType = "IMMIGRATION",
      overallSentenceLength = null,
      nextCourtAppearance = null,
      charges = listOf(
        CreateCharge(
          chargeUuid = chargeUuid ?: UUID.randomUUID(),
          appearanceUuid = null,
          offenceCode = "ZI26000",
          offenceStartDate = immigrationDetention.recordDate,
          offenceEndDate = null,
          outcomeUuid = chargeOutcomeService.findByUuid(appearanceOutcome.relatedChargeOutcomeUuid)?.outcomeUuid,
          terrorRelated = null,
          foreignPowerRelated = null,
          domesticViolenceRelated = null,
          sentence = null,
          legacyData = null,
          prisonId = immigrationDetention.createdByPrison,
          replacingChargeUuid = null,
        ),
      ),
      overallConvictionDate = null,
      legacyData = null,
      prisonId = immigrationDetention.createdByPrison,
      documents = null,
    )
  }
}
