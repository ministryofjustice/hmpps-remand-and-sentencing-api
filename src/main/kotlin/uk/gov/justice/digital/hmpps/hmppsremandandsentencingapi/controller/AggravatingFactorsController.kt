package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.AggravatingFactor
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.AggravatingFactorStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.AggravatingFactorsService

@RestController
@RequestMapping("/aggravating-factors")
@Tag(
  name = "Aggravating Factors",
  description = "Aggravating factors are used to determine the seriousness of an offence and the appropriate sentence. They are used in conjunction with mitigating factors to determine the overall seriousness of an offence.",
)
class AggravatingFactorsController(private val aggravatingFactorsService: AggravatingFactorsService) {

  @GetMapping("/status")
  @Operation(
    summary = "Get all aggravating factors by status",
    description = "This endpoint will get all aggravating factors by status",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "Returns all aggravating factors"),
      ApiResponse(responseCode = "401", description = "Unauthorised, requires a valid Oauth2 token"),
      ApiResponse(responseCode = "403", description = "Forbidden, requires an appropriate role"),
    ],
  )
  fun getAllAggravatingFactors(@RequestParam("statuses") statuses: List<AggravatingFactorStatus>): List<AggravatingFactor> = aggravatingFactorsService.getAllByStatuses(statuses)
}
