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
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.legacy.controller.dto.LegacyCourtCaseCreatedResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.legacy.controller.dto.LegacyCreateCourtCase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.service.LegacyCourtCaseService
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.CourtCaseDomainEventService

@RestController
@RequestMapping("/legacy/court-case", produces = [MediaType.APPLICATION_JSON_VALUE])
@Tag(name = "legacy-court-case-controller", description = "CRUD operations for syncing court case data from NOMIS into remand and sentencing api database.")
class LegacyCourtCaseController(private val legacyCourtCaseService: LegacyCourtCaseService, private val eventService: CourtCaseDomainEventService) {

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Create a court case",
    description = "Synchronise a creation of court case from NOMIS into remand and sentencing API.",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "201", description = "court case created"),
      ApiResponse(responseCode = "401", description = "Unauthorised, requires a valid Oauth2 token"),
      ApiResponse(responseCode = "403", description = "Forbidden, requires an appropriate role"),
    ],
  )
  @PreAuthorize("hasRole('ROLE_REMAND_AND_SENTENCING_COURT_CASE_RW')")
  fun create(@RequestBody courtCase: LegacyCreateCourtCase): LegacyCourtCaseCreatedResponse {
    return legacyCourtCaseService.create(courtCase).also {
      eventService.create(it.courtCaseUuid, courtCase.prisonerId, "NOMIS")
    }
  }
}
