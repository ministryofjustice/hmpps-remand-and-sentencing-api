package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.persistence.EntityNotFoundException
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.courtappearanceschedule.CourtAppearanceSchedulesResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.courtappearanceschedule.DeleteCourtAppearanceScheduleStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.courtappearanceschedule.SearchCourtAppearanceSchedulesRequest
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.courtappearanceschedule.UpdateCourtAppearanceSchedule
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.CourtAppearanceSchedulesService
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.DpsDomainEventService
import java.util.UUID

@RestController
@Tag(name = "court-appearance-schedules-controller", description = "Court Appearance schedules")
class CourtAppearanceSchedulesController(private val courtAppearanceSchedulesService: CourtAppearanceSchedulesService, private val dpsDomainEventService: DpsDomainEventService) {

  @PostMapping("/search/court-appearance-schedules")
  @PreAuthorize("hasAnyRole('ROLE_COURT_APPEARANCES__COURT_APPEARANCE_SCHEDULER__RW', 'ROLE_COURT_APPEARANCES__COURT_APPEARANCE_SCHEDULER__RO')")
  @Operation(
    summary = "Search court appearance schedules",
    description = "This endpoint will search for court appearance in the schedules format for supplied UUIDs",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "Returns court appearance schedules"),
      ApiResponse(responseCode = "401", description = "Unauthorised, requires a valid Oauth2 token"),
      ApiResponse(responseCode = "403", description = "Forbidden, requires an appropriate role"),
    ],
  )
  @ResponseStatus(HttpStatus.OK)
  fun searchCourtAppearanceSchedules(@RequestBody searchCourtAppearanceSchedulesRequest: SearchCourtAppearanceSchedulesRequest): CourtAppearanceSchedulesResponse = courtAppearanceSchedulesService.search(searchCourtAppearanceSchedulesRequest)

  @GetMapping("/court-appearance-schedule/{appearanceUuid}/delete-status")
  @PreAuthorize("hasAnyRole('ROLE_COURT_APPEARANCES__COURT_APPEARANCE_SCHEDULER__RW', 'ROLE_COURT_APPEARANCES__COURT_APPEARANCE_SCHEDULER__RO')")
  @Operation(
    summary = "Delete court appearance schedule status",
    description = "This endpoint will return whether a court appearance schedule can be deleted",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "return delete status"),
      ApiResponse(responseCode = "401", description = "Unauthorised, requires a valid Oauth2 token"),
      ApiResponse(responseCode = "403", description = "Forbidden, requires an appropriate role"),
      ApiResponse(responseCode = "404", description = "No court appearance schedule found"),
    ],
  )
  @ResponseStatus(HttpStatus.OK)
  fun deleteCourtAppearanceScheduleStatus(@PathVariable("appearanceUuid") appearanceUuid: UUID): DeleteCourtAppearanceScheduleStatus = courtAppearanceSchedulesService.deleteStatus(appearanceUuid) ?: throw EntityNotFoundException("No court appearance schedule found at $appearanceUuid")

  @GetMapping("/person/{prisonerId}/court-appearance-schedules")
  @PreAuthorize("hasAnyRole('ROLE_COURT_APPEARANCES__COURT_APPEARANCE_SCHEDULER__RW', 'ROLE_COURT_APPEARANCES__COURT_APPEARANCE_SCHEDULER__RO')")
  @Operation(
    summary = "Get court appearance schedules by prisoner id",
    description = "This endpoint will get court appearance in the schedules format for prisoner id",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "Returns court appearance schedules"),
      ApiResponse(responseCode = "401", description = "Unauthorised, requires a valid Oauth2 token"),
      ApiResponse(responseCode = "403", description = "Forbidden, requires an appropriate role"),
    ],
  )
  fun getCourtAppearanceSchedulesByPrisonerId(@PathVariable prisonerId: String): CourtAppearanceSchedulesResponse = courtAppearanceSchedulesService.getByPrisonerId(prisonerId)

  @PutMapping("/court-appearance-schedule/{appearanceUuid}")
  @PreAuthorize("hasAnyRole('ROLE_COURT_APPEARANCES__COURT_APPEARANCE_SCHEDULER__RW')")
  @Operation(
    summary = "Update court appearance schedule by appearance uuid",
    description = "This endpoint will update court appearance schedule by appearance uuid",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "Returns court appearance schedules"),
      ApiResponse(responseCode = "401", description = "Unauthorised, requires a valid Oauth2 token"),
      ApiResponse(responseCode = "403", description = "Forbidden, requires an appropriate role"),
      ApiResponse(responseCode = "404", description = "No court appearance schedule found"),
    ],
  )
  @ResponseStatus(HttpStatus.NO_CONTENT)
  fun updateCourtAppearanceSchedule(@PathVariable appearanceUuid: UUID, @RequestBody updateCourtAppearanceSchedule: UpdateCourtAppearanceSchedule) = courtAppearanceSchedulesService.updateCourtAppearanceSchedule(appearanceUuid, updateCourtAppearanceSchedule)?.let { eventsToEmit ->
    dpsDomainEventService.emitEvents(eventsToEmit)
  } ?: throw EntityNotFoundException("No court appearance schedule found at $appearanceUuid")
}
