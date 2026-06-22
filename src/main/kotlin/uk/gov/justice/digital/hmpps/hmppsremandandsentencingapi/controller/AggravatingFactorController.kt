package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.AggravatingFactor
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.AggravatingFactorService

@RestController
@RequestMapping("/aggravating-factors")
@Tag(
  name = "Aggravating Factors",
  description = "Aggravating factors are used to determine the seriousness of an offence and the appropriate sentence. They are used in conjunction with mitigating factors to determine the overall seriousness of an offence."
)
class AggravatingFactorController(private val aggravatingFactorService: AggravatingFactorService) {

  @GetMapping("")
  @PreAuthorize("hasAnyRole('ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI')")
  @Operation(
    summary = "Get all aggravating factors",
    description = "Get all aggravating factors",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "Returns all aggravating factors"),
      ApiResponse(responseCode = "401", description = "Unauthorised, requires a valid Oauth2 token"),
      ApiResponse(responseCode = "403", description = "Forbidden, requires an appropriate role"),
    ],
  )
  fun getAllAggravatingFactors(): List<AggravatingFactor> = aggravatingFactorService.getAggravatingFactors()
}