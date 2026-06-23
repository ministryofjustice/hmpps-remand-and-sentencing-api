package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.courtappearanceschedule.CourtAppearanceSchedule
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.courtappearanceschedule.CourtAppearanceSchedulesResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.courtappearanceschedule.DeleteCourtAppearanceScheduleStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.courtappearanceschedule.SearchCourtAppearanceSchedulesRequest
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.CourtAppearanceEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.CourtAppearanceRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.NextCourtAppearanceRepository
import java.util.UUID

@Service
class CourtAppearanceSchedulesService(private val courtAppearanceRepository: CourtAppearanceRepository, private val nextCourtAppearanceRepository: NextCourtAppearanceRepository) {
  @Transactional(readOnly = true)
  fun search(searchCourtAppearanceSchedulesRequest: SearchCourtAppearanceSchedulesRequest): CourtAppearanceSchedulesResponse {
    val courtAppearances = courtAppearanceRepository.findAllByAppearanceUuidInAndStatusIdNot(searchCourtAppearanceSchedulesRequest.uuids)
    val associatedNextCourtAppearances = nextCourtAppearanceRepository.findByFutureSkeletonAppearanceIdIn(courtAppearances.map { it.id }.toSet()).groupBy { it.futureSkeletonAppearance.id }
    return CourtAppearanceSchedulesResponse(courtAppearances.map { CourtAppearanceSchedule.from(it, associatedNextCourtAppearances.getOrDefault(it.id, emptyList()).firstOrNull()) })
  }

  @Transactional(readOnly = true)
  fun deleteStatus(appearanceUuid: UUID): DeleteCourtAppearanceScheduleStatus? = courtAppearanceRepository.findByAppearanceUuid(appearanceUuid)?.takeUnless { it.statusId == CourtAppearanceEntityStatus.DELETED }?.let { courtAppearance ->
    DeleteCourtAppearanceScheduleStatus(courtAppearance.deleteStatus())
  }

  fun getByPrisonerId(prisonerId: String): CourtAppearanceSchedulesResponse {
    val courtAppearances = courtAppearanceRepository.findAllByCourtCasePrisonerIdAndStatusIdNot(prisonerId)
    val associatedNextCourtAppearances = nextCourtAppearanceRepository.findByFutureSkeletonAppearanceIdIn(courtAppearances.map { it.id }.toSet()).groupBy { it.futureSkeletonAppearance.id }
    return CourtAppearanceSchedulesResponse(courtAppearances.map { CourtAppearanceSchedule.from(it, associatedNextCourtAppearances.getOrDefault(it.id, emptyList()).firstOrNull()) })
  }
}
