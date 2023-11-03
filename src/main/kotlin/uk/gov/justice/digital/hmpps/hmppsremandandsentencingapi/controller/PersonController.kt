package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.PersonDetails
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.PersonService

@RestController
@RequestMapping("/person", produces = [MediaType.APPLICATION_JSON_VALUE])
@Tag(name = "person-controller", description = "Get person details")
class PersonController(private val personService: PersonService) {

  @GetMapping("/{prisonerId}")
  @PreAuthorize("hasAnyRole('ROLE_REMAND_AND_SENTENCING', 'ROLE_RELEASE_DATES_CALCULATOR')")
  @Operation(
    summary = "Retrieve person details",
    description = "This endpoint will retrieve person details",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "Returns person details"),
      ApiResponse(responseCode = "401", description = "Unauthorised, requires a valid Oauth2 token"),
      ApiResponse(responseCode = "403", description = "Forbidden, requires an appropriate role"),
    ],
  )
  suspend fun getPersonDetails(@PathVariable prisonerId: String): PersonDetails {
    return personService.getPersonDetailsByPrisonerId(prisonerId)
  }
}
