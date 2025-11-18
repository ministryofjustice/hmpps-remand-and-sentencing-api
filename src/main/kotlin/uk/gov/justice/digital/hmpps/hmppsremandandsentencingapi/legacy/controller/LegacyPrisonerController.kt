package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacyCourtCaseUuids
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.service.LegacyCourtCaseService

@RestController
@RequestMapping("/legacy/prisoner", produces = [MediaType.APPLICATION_JSON_VALUE])
@Tag(
  name = "legacy-prisoner-controller",
  description = "CRUD operations for getting prisoner data",
)
class LegacyPrisonerController(private val legacyCourtCaseService: LegacyCourtCaseService) {
  @GetMapping("/{prisonerId}/court-case-uuids")
  @Operation(
    summary = "retrieve all court case uuids",
    description = "This endpoint will retrieve all court case uuids",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "Returns court case uuids"),
      ApiResponse(responseCode = "401", description = "Unauthorised, requires a valid Oauth2 token"),
      ApiResponse(responseCode = "403", description = "Forbidden, requires an appropriate role"),
    ],
  )
  fun getCourtCaseUuids(@PathVariable prisonerId: String): LegacyCourtCaseUuids = legacyCourtCaseService.getCourtCaseUuids(prisonerId)
}
