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
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CourtAppearance
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.HmctsCourtDataService
import java.util.UUID

@RestController
@RequestMapping("/hmcts-court-data", produces = [MediaType.APPLICATION_JSON_VALUE])
@Tag(
  name = "hmcts-court-data-controller",
  description = "Endpoints to retrieve HMCTS Court Data in RaS structure.",
)
class HmctsCourtDataController(
  private val hmctsCourtDataService: HmctsCourtDataService,
) {

  @GetMapping("/{courtHearingId}/appearance")
  @PreAuthorize("hasAnyRole('ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI')")
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
  fun getCourtAppearanceFromHmctsHearingId(@PathVariable courtHearingId: UUID): CourtAppearance = hmctsCourtDataService.getCourtAppearanceFromHmctsHearingId(courtHearingId)
}
