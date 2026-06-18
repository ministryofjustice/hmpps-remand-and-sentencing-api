package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.courtappearanceschedule.CourtAppearanceSchedule
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.courtappearanceschedule.SearchCourtAppearanceSchedulesRequest
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.courtappearanceschedule.SearchCourtAppearanceSchedulesResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.CourtAppearanceRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.NextCourtAppearanceRepository

@Service
class CourtAppearanceSchedulesService(private val courtAppearanceRepository: CourtAppearanceRepository, private val nextCourtAppearanceRepository: NextCourtAppearanceRepository) {
  @Transactional(readOnly = true)
  fun search(searchCourtAppearanceSchedulesRequest: SearchCourtAppearanceSchedulesRequest): SearchCourtAppearanceSchedulesResponse {
    val courtAppearances = courtAppearanceRepository.findAllByAppearanceUuidInAndStatusIdNot(searchCourtAppearanceSchedulesRequest.uuids)
    val associatedNextCourtAppearances = nextCourtAppearanceRepository.findByFutureSkeletonAppearanceIdIn(courtAppearances.map { it.id }.toSet()).groupBy { it.futureSkeletonAppearance.id }
    return SearchCourtAppearanceSchedulesResponse(courtAppearances.map { CourtAppearanceSchedule.from(it, associatedNextCourtAppearances.getOrDefault(it.id, emptyList()).firstOrNull()) })
  }
}
