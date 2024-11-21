package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacyCourtCaseCreatedResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacyCreateCourtCase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.legacy.controller.dto.LegacyCourtCase
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

  @GetMapping("/{courtCaseUuid}")
  @Operation(
    summary = "Retrieve court case details",
    description = "This endpoint will retrieve court case details",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "Returns court case details"),
      ApiResponse(responseCode = "401", description = "Unauthorised, requires a valid Oauth2 token"),
      ApiResponse(responseCode = "403", description = "Forbidden, requires an appropriate role"),
      ApiResponse(responseCode = "404", description = "Not found if no court case at uuid"),
    ],
  )
  @PreAuthorize("hasAnyRole('ROLE_REMAND_AND_SENTENCING_COURT_CASE_RO', 'ROLE_REMAND_AND_SENTENCING_COURT_CASE_RW')")
  fun get(@PathVariable courtCaseUuid: String): LegacyCourtCase {
    return legacyCourtCaseService.get(courtCaseUuid)
  }

  @PutMapping("/{courtCaseUuid}")
  @Operation(
    summary = "Updates a court case",
    description = "Synchronise an update of court case from NOMIS into remand and sentencing API.",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "court case updated"),
      ApiResponse(responseCode = "401", description = "Unauthorised, requires a valid Oauth2 token"),
      ApiResponse(responseCode = "403", description = "Forbidden, requires an appropriate role"),
    ],
  )
  @PreAuthorize("hasRole('ROLE_REMAND_AND_SENTENCING_COURT_CASE_RW')")
  fun update(@PathVariable courtCaseUuid: String, @RequestBody courtCase: LegacyCreateCourtCase) {
    legacyCourtCaseService.update(courtCaseUuid, courtCase).also {
      eventService.update(it.courtCaseUuid, courtCase.prisonerId, "NOMIS")
    }
  }

  @DeleteMapping("/{courtCaseUuid}")
  @Operation(
    summary = "Deletes a court case",
    description = "Synchronise a deletion of court case from NOMIS into remand and sentencing API.",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "court case deleted"),
      ApiResponse(responseCode = "401", description = "Unauthorised, requires a valid Oauth2 token"),
      ApiResponse(responseCode = "403", description = "Forbidden, requires an appropriate role"),
    ],
  )
  @PreAuthorize("hasRole('ROLE_REMAND_AND_SENTENCING_COURT_CASE_RW')")
  fun delete(@PathVariable courtCaseUuid: String) {
    legacyCourtCaseService.get(courtCaseUuid).also { legacyCourtCase ->
      legacyCourtCaseService.delete(courtCaseUuid)
      eventService.delete(courtCaseUuid, legacyCourtCase.prisonerId, "NOMIS")
    }
  }
}
