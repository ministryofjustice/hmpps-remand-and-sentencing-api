package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.courtappearanceschedule.CourtAppearanceSchedule
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.courtappearanceschedule.CourtAppearanceSchedulesResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.courtappearanceschedule.DeleteCourtAppearanceScheduleStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.courtappearanceschedule.SearchCourtAppearanceSchedulesRequest
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.courtappearanceschedule.UpdateCourtAppearanceSchedule
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.EventMetadata
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.EventType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.util.EventMetadataCreator
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.CourtAppearanceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.NextCourtAppearanceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.audit.CourtAppearanceHistoryEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ChangeSource
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.CourtAppearanceEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.CourtAppearanceRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.NextCourtAppearanceRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.audit.CourtAppearanceHistoryRepository
import java.util.UUID
import kotlin.collections.plus

@Service
class CourtAppearanceSchedulesService(private val courtAppearanceRepository: CourtAppearanceRepository, private val nextCourtAppearanceRepository: NextCourtAppearanceRepository, private val courtAppearanceHistoryRepository: CourtAppearanceHistoryRepository, val serviceUserService: ServiceUserService, private val courtCaseService: CourtCaseService, private val immigrationDetentionService: ImmigrationDetentionService, private val nextCourtAppearanceService: NextCourtAppearanceService) {
  @Transactional(readOnly = true)
  fun search(searchCourtAppearanceSchedulesRequest: SearchCourtAppearanceSchedulesRequest): CourtAppearanceSchedulesResponse {
    val courtAppearances = courtAppearanceRepository.findAllByAppearanceUuidInAndStatusIdNot(searchCourtAppearanceSchedulesRequest.uuids)
    val associatedNextCourtAppearances = nextCourtAppearanceRepository.findByFutureSkeletonAppearanceIdIn(courtAppearances.map { it.id }.toSet()).groupBy { it.futureSkeletonAppearance.id }
    return CourtAppearanceSchedulesResponse(courtAppearances.map { CourtAppearanceSchedule.from(it, associatedNextCourtAppearances.getOrDefault(it.id, emptyList()).firstOrNull()) })
  }

  @Transactional(readOnly = true)
  fun deleteStatus(appearanceUuid: UUID): DeleteCourtAppearanceScheduleStatus? = getCourtAppearanceByUuid(appearanceUuid)?.let { courtAppearance ->
    DeleteCourtAppearanceScheduleStatus(courtAppearance.deleteStatus())
  }

  fun getByPrisonerId(prisonerId: String): CourtAppearanceSchedulesResponse {
    val courtAppearances = courtAppearanceRepository.findAllByCourtCasePrisonerIdAndStatusIdNot(prisonerId)
    val associatedNextCourtAppearances = nextCourtAppearanceRepository.findByFutureSkeletonAppearanceIdIn(courtAppearances.map { it.id }.toSet()).groupBy { it.futureSkeletonAppearance.id }
    return CourtAppearanceSchedulesResponse(courtAppearances.map { CourtAppearanceSchedule.from(it, associatedNextCourtAppearances.getOrDefault(it.id, emptyList()).firstOrNull()) })
  }

  @Transactional
  fun updateCourtAppearanceSchedule(
    appearanceUuid: UUID,
    updateCourtAppearanceSchedule: UpdateCourtAppearanceSchedule,
  ): Set<EventMetadata>? = getCourtAppearanceByUuid(appearanceUuid)?.let { existingCourtAppearance ->
    val eventsToEmit = mutableSetOf<EventMetadata>()
    val performedByUser = serviceUserService.getUsername()
    val updatedCourtAppearance = existingCourtAppearance.copyFrom(updateCourtAppearanceSchedule, performedByUser)

    if (!existingCourtAppearance.isSame(updatedCourtAppearance)) {
      existingCourtAppearance.updateFrom(updatedCourtAppearance)
      courtAppearanceHistoryRepository.save(
        CourtAppearanceHistoryEntity.from(
          existingCourtAppearance,
          ChangeSource.DPS,
        ),
      )
      courtCaseService.handleUpdatingLatestCourtAppearanceInCourtCase(existingCourtAppearance.courtCase, existingCourtAppearance, performedByUser)
      immigrationDetentionService.handleImmigrationDetention(existingCourtAppearance, performedByUser) {
        it.copyFrom(existingCourtAppearance, updateCourtAppearanceSchedule, performedByUser)
      }
      nextCourtAppearanceService.handleNextCourtAppearance(existingCourtAppearance, updateCourtAppearanceSchedule.reasonCode, performedByUser, updateCourtAppearanceSchedule.start) {
        NextCourtAppearanceEntity.from(updateCourtAppearanceSchedule, existingCourtAppearance, it)
      }
      eventsToEmit.add(
        EventMetadataCreator.courtAppearanceEventMetadata(
          existingCourtAppearance.courtCase.prisonerId,
          existingCourtAppearance.courtCase.caseUniqueIdentifier,
          existingCourtAppearance.appearanceUuid.toString(),
          EventType.COURT_APPEARANCE_UPDATED,
        ),
      )
    }
    eventsToEmit
  }

  private fun getCourtAppearanceByUuid(appearanceUuid: UUID): CourtAppearanceEntity? = courtAppearanceRepository.findByAppearanceUuid(appearanceUuid)?.takeUnless { it.statusId == CourtAppearanceEntityStatus.DELETED }
}
