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
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.courtappearanceschedule.DeleteCourtAppearanceScheduleStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.courtappearanceschedule.SearchCourtAppearanceSchedulesRequest
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.courtappearanceschedule.SearchCourtAppearanceSchedulesResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.CourtAppearanceSchedulesService
import java.util.UUID

@RestController
@Tag(name = "court-appearance-schedules-controller", description = "Court Appearance schedules")
class CourtAppearanceSchedulesController(private val courtAppearanceSchedulesService: CourtAppearanceSchedulesService) {

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
  fun searchCourtAppearanceSchedules(@RequestBody searchCourtAppearanceSchedulesRequest: SearchCourtAppearanceSchedulesRequest): SearchCourtAppearanceSchedulesResponse = courtAppearanceSchedulesService.search(searchCourtAppearanceSchedulesRequest)

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
}
