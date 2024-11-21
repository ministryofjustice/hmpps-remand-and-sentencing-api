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
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacyChargeCreatedResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacyCreateCharge
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.service.LegacyChargeService
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.ChargeDomainEventService

@RestController
@RequestMapping("/legacy/charge", produces = [MediaType.APPLICATION_JSON_VALUE])
@Tag(name = "legacy-charge-controller", description = "CRUD operations for syncing charge data from NOMIS Offender charges into remand and sentencing api database.")
class LegacyChargeController(private val legacyChargeService: LegacyChargeService, private val eventService: ChargeDomainEventService) {

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Create a charge",
    description = "Synchronise a creation of charge from NOMIS Offender charges into remand and sentencing API.",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "201", description = "charge created"),
      ApiResponse(responseCode = "401", description = "Unauthorised, requires a valid Oauth2 token"),
      ApiResponse(responseCode = "403", description = "Forbidden, requires an appropriate role"),
    ],
  )
  @PreAuthorize("hasRole('ROLE_REMAND_AND_SENTENCING_CHARGE_RW')")
  fun create(@RequestBody charge: LegacyCreateCharge): LegacyChargeCreatedResponse {
    return legacyChargeService.create(charge).also {
      eventService.create(it.prisonerId, it.lifetimeUuid.toString(), it.courtCaseUuid, "NOMIS")
    }
  }
}
