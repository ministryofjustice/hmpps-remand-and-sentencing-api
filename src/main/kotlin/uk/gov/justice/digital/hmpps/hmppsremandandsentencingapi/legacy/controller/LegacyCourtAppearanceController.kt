package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.legacy.controller.dto.LegacyCourtAppearanceCreatedResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.legacy.controller.dto.LegacyCreateCourtAppearance
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.service.LegacyCourtAppearanceService
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.CourtAppearanceDomainEventService

@RestController
@RequestMapping("/legacy/court-appearance", produces = [MediaType.APPLICATION_JSON_VALUE])
@Tag(name = "legacy-court-appearance-controller", description = "CRUD operations for syncing court appearance data from NOMIS Court Events into remand and sentencing api database.")
class LegacyCourtAppearanceController(private val legacyCourtAppearanceService: LegacyCourtAppearanceService, private val eventService: CourtAppearanceDomainEventService) {

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Create a court appearance",
    description = "Synchronise a creation of court appearance from NOMIS court events into remand and sentencing API.",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "201", description = "court appearance created"),
      ApiResponse(responseCode = "401", description = "Unauthorised, requires a valid Oauth2 token"),
      ApiResponse(responseCode = "403", description = "Forbidden, requires an appropriate role"),
    ],
  )
  @PreAuthorize("hasRole('ROLE_REMAND_AND_SENTENCING_COURT_APPEARANCE_RW')")
  fun create(@RequestBody courtAppearance: LegacyCreateCourtAppearance): LegacyCourtAppearanceCreatedResponse {
    return legacyCourtAppearanceService.create(courtAppearance).also {
      eventService.create(it.prisonerId, it.lifetimeUuid.toString(), it.courtCaseUuid, "NOMIS")
    }
  }
}
