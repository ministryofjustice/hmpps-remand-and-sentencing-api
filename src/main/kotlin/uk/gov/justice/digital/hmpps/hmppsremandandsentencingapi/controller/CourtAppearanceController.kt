package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.persistence.EntityNotFoundException
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateCourtAppearance
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateCourtAppearanceResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.CourtAppearanceService

@RestController
@RequestMapping("/court-appearance", produces = [MediaType.APPLICATION_JSON_VALUE])
@Tag(name = "court-appearance-controller", description = "Court Appearances")
class CourtAppearanceController(private val courtAppearanceService: CourtAppearanceService) {

  @PostMapping
  @PreAuthorize("hasAnyRole('ROLE_REMAND_AND_SENTENCING', 'ROLE_RELEASE_DATES_CALCULATOR')")
  @Operation(
    summary = "Create Court appearance",
    description = "This endpoint will create a court appearance in a given court case",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "201", description = "Returns court case UUID"),
      ApiResponse(responseCode = "401", description = "Unauthorised, requires a valid Oauth2 token"),
      ApiResponse(responseCode = "403", description = "Forbidden, requires an appropriate role"),
    ],
  )
  @ResponseStatus(HttpStatus.CREATED)
  fun createCourtAppearance(@RequestBody createCourtAppearance: CreateCourtAppearance): CreateCourtAppearanceResponse {
    return courtAppearanceService.createCourtAppearance(createCourtAppearance)?.let { CreateCourtAppearanceResponse(it.appearanceUuid) } ?: throw EntityNotFoundException("No court case found at ${createCourtAppearance.courtCaseUuid}")
  }
}
