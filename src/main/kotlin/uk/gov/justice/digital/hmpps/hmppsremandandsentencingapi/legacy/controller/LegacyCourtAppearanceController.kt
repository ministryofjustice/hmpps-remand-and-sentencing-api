package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityChangeStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.legacy.controller.dto.LegacyCourtAppearance
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.legacy.controller.dto.LegacyCourtAppearanceCreatedResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.legacy.controller.dto.LegacyCreateCourtAppearance
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.service.LegacyCourtAppearanceService
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.CourtAppearanceDomainEventService
import java.util.UUID

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
  @PreAuthorize("hasRole('ROLE_REMAND_AND_SENTENCING_APPEARANCE_RW')")
  fun create(@RequestBody courtAppearance: LegacyCreateCourtAppearance): LegacyCourtAppearanceCreatedResponse {
    return legacyCourtAppearanceService.create(courtAppearance).also {
      eventService.create(it.prisonerId, it.lifetimeUuid.toString(), it.courtCaseUuid, "NOMIS")
    }
  }

  @PutMapping("/{lifetimeUuid}")
  @Operation(
    summary = "Updates a court appearance",
    description = "Synchronise an update of court appearance from NOMIS court events into remand and sentencing API.",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "court appearance updated"),
      ApiResponse(responseCode = "401", description = "Unauthorised, requires a valid Oauth2 token"),
      ApiResponse(responseCode = "403", description = "Forbidden, requires an appropriate role"),
    ],
  )
  @PreAuthorize("hasRole('ROLE_REMAND_AND_SENTENCING_APPEARANCE_RW')")
  fun update(@PathVariable lifetimeUuid: UUID, @RequestBody courtAppearance: LegacyCreateCourtAppearance) {
    legacyCourtAppearanceService.update(lifetimeUuid, courtAppearance).also<Pair<EntityChangeStatus, LegacyCourtAppearanceCreatedResponse>> { (entityChangeStatus, legacyCourtAppearanceCreatedResponse) ->
      if (entityChangeStatus == EntityChangeStatus.EDITED) {
        eventService.update(legacyCourtAppearanceCreatedResponse.prisonerId, legacyCourtAppearanceCreatedResponse.lifetimeUuid.toString(), legacyCourtAppearanceCreatedResponse.courtCaseUuid, "NOMIS")
      }
    }
  }

  @GetMapping("/{lifetimeUuid}")
  @Operation(
    summary = "Retrieve court appearance details",
    description = "This endpoint will retrieve court appearance details",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "Returns court appearance details"),
      ApiResponse(responseCode = "401", description = "Unauthorised, requires a valid Oauth2 token"),
      ApiResponse(responseCode = "403", description = "Forbidden, requires an appropriate role"),
      ApiResponse(responseCode = "404", description = "Not found if no court appearance at uuid"),
    ],
  )
  @PreAuthorize("hasAnyRole('ROLE_REMAND_AND_SENTENCING_APPEARANCE_RW', 'ROLE_REMAND_AND_SENTENCING_APPEARANCE_RO')")
  fun get(@PathVariable lifetimeUuid: UUID): LegacyCourtAppearance {
    return legacyCourtAppearanceService.get(lifetimeUuid)
  }
}
