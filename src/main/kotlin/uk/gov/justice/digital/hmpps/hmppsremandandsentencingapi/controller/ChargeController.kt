package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.persistence.EntityNotFoundException
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.Charge
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.ChargeService
import java.util.UUID

@RestController
@Tag(name = "charge-controller", description = "Charges")
class ChargeController(private val chargeService: ChargeService) {

  @GetMapping("\${court.charge.getByIdPath}")
  @PreAuthorize("hasAnyRole('ROLE_REMAND_AND_SENTENCING', 'ROLE_RELEASE_DATES_CALCULATOR')")
  @Operation(
    summary = "Retrieve charge details",
    description = "This endpoint will retrieve charge details",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "Returns charge details"),
      ApiResponse(responseCode = "401", description = "Unauthorised, requires a valid Oauth2 token"),
      ApiResponse(responseCode = "403", description = "Forbidden, requires an appropriate role"),
      ApiResponse(responseCode = "404", description = "Not found if no charge at uuid"),
    ],
  )
  fun getChargeDetails(@PathVariable chargeUuid: UUID): Charge = chargeService.findChargeByUuid(chargeUuid) ?: throw EntityNotFoundException("No charge found at $chargeUuid")
}
