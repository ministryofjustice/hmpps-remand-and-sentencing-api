package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.ChargeOutcome
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.ChargeOutcomeService

@RestController
@RequestMapping("/charge-outcome", produces = [MediaType.APPLICATION_JSON_VALUE])
@Tag(name = "charge-outcome-controller", description = "Charge outcome")
class ChargeOutcomeController(private val chargeOutcomeService: ChargeOutcomeService) {

  @GetMapping("/all")
  @Operation(
    summary = "Get all charge outcomes",
    description = "This endpoint will get all charge outcomes",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "Returns charge outcomes"),
      ApiResponse(responseCode = "401", description = "Unauthorised, requires a valid Oauth2 token"),
      ApiResponse(responseCode = "403", description = "Forbidden, requires an appropriate role"),
    ],
  )
  @ResponseStatus(HttpStatus.OK)
  fun getAllChargeOutcomes(): List<ChargeOutcome> {
    return chargeOutcomeService.getAll()
  }
}