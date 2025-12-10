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
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.RecordResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.CourtAppearanceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.ImmigrationDetentionEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.audit.ImmigrationDetentionHistoryEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ImmigrationDetentionEntityStatus.ACTIVE
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ImmigrationDetentionEntityStatus.DELETED
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ImmigrationDetentionEntityStatus.EDITED
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.CourtAppearanceRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.ImmigrationDetentionRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.audit.ImmigrationDetentionHistoryRepository
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
) {

  @Transactional
  fun createImmigrationDetention(
    immigrationDetention: CreateImmigrationDetention,
    immigrationDetentionUuid: UUID? = null,
  ): RecordResponse<SaveImmigrationDetentionResponse> {
    val courtCase = courtCaseService.getLatestImmigrationDetentionCourtCase(immigrationDetention.prisonerId)
    val eventsToEmit = mutableSetOf<EventMetadata>()
    if (courtCase == null) {
      eventsToEmit.addAll(
        courtCaseService.createCourtCase(
          CreateCourtCase(
            prisonerId = immigrationDetention.prisonerId,
            prisonId = immigrationDetention.createdByPrison,
            appearances = listOf(createCourtAppearanceFromImmigrationDetention(immigrationDetention)),
            legacyData = null,
          ),
        ).eventsToEmit,
      )
    } else {
      eventsToEmit.addAll(
        courtAppearanceService.createCourtAppearance(
          createCourtAppearanceFromImmigrationDetention(
            immigrationDetention,
            courtCase.caseUniqueIdentifier,
          ),
        )?.eventsToEmit ?: emptySet(),
      )
    }

    val savedImmigrationDetention = immigrationDetentionRepository.save(
      ImmigrationDetentionEntity.fromDPS(
        immigrationDetention,
        immigrationDetentionUuid,
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

      val courtCase = courtCaseService.getLatestImmigrationDetentionCourtCase(immigrationDetention.prisonerId)

      val latestMatchingAppearance = courtCase?.appearances
        ?.filter { it.appearanceOutcome?.outcomeUuid == immigrationDetention.appearanceOutcomeUuid }
        ?.maxByOrNull { it.appearanceDate }

      val eventsToEmit = mutableSetOf<EventMetadata>()

      if (latestMatchingAppearance == null) {
        eventsToEmit.addAll(
          courtAppearanceService.createCourtAppearance(
            createCourtAppearanceFromImmigrationDetention(
              immigrationDetention,
              courtCase?.caseUniqueIdentifier,
            ),
          )?.eventsToEmit ?: emptySet(),
        )
      } else {
        eventsToEmit.addAll(
          courtAppearanceService.createCourtAppearanceByAppearanceUuid(
            createCourtAppearanceFromImmigrationDetention(
              immigrationDetention,
              courtCase.caseUniqueIdentifier,
              latestMatchingAppearance.appearanceCharges.firstOrNull()?.charge?.chargeUuid,
            ),
            latestMatchingAppearance.appearanceUuid,
          )?.eventsToEmit ?: emptySet(),
        )
      }

      return RecordResponse(SaveImmigrationDetentionResponse.from(savedImmigrationDetention), eventsToEmit)
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
          offenceCode = "IA99000-001N",
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
