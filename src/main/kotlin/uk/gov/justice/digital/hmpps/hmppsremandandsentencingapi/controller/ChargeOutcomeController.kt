package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.persistence.EntityNotFoundException
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.ChargeOutcome
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.chargeoutcome.CreateChargeOutcome
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ReferenceEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.ChargeOutcomeService
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.datafix.MigrateChargeRecordOutcomes
import java.util.UUID

@RestController
@RequestMapping("/charge-outcome", produces = [MediaType.APPLICATION_JSON_VALUE])
@Tag(name = "charge-outcome-controller", description = "Charge outcome")
class ChargeOutcomeController(private val chargeOutcomeService: ChargeOutcomeService, private val migrateChargeRecordOutcomes: MigrateChargeRecordOutcomes) {

  @PostMapping
  @Operation(
    summary = "Create charge outcome",
    description = "This endpoint will create a new charge outcome and migrate any charge data over that needs to be mapped to the newly created charge outcome",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "201"),
      ApiResponse(responseCode = "401", description = "Unauthorised, requires a valid Oauth2 token"),
      ApiResponse(responseCode = "403", description = "Forbidden, requires an appropriate role"),
    ],
  )
  @ResponseStatus(HttpStatus.CREATED)
  fun createChargeOutcome(@RequestBody @Valid createChargeOutcome: CreateChargeOutcome): ChargeOutcome = chargeOutcomeService.createChargeOutcome(createChargeOutcome).let { createdChargeOutcomeEntity ->
    migrateChargeRecordOutcomes.migrateChargeRecordsToOutcome(createdChargeOutcomeEntity)
    ChargeOutcome.from(createdChargeOutcomeEntity)
  }

  @GetMapping("/status")
  @Operation(
    summary = "Get all charge outcomes by statuses",
    description = "This endpoint will get all charge outcomes by statuses",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "Returns charge outcomes"),
      ApiResponse(responseCode = "401", description = "Unauthorised, requires a valid Oauth2 token"),
      ApiResponse(responseCode = "403", description = "Forbidden, requires an appropriate role"),
    ],
  )
  @ResponseStatus(HttpStatus.OK)
  fun getAllChargeOutcomes(@RequestParam("statuses") statuses: List<ReferenceEntityStatus>): List<ChargeOutcome> = chargeOutcomeService.getAllByStatus(statuses)

  @GetMapping("/{outcomeUuid}")
  @Operation(
    summary = "Get charge outcome by UUID",
    description = "This endpoint will retrieve charge outcome by UUID",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "Returns charge outcome"),
      ApiResponse(responseCode = "401", description = "Unauthorised, requires a valid Oauth2 token"),
      ApiResponse(responseCode = "403", description = "Forbidden, requires an appropriate role"),
      ApiResponse(responseCode = "404", description = "Not found if no charge outcome at uuid"),
    ],
  )
  @ResponseStatus(HttpStatus.OK)
  fun getChargeOutcomeByUuid(@PathVariable outcomeUuid: UUID): ChargeOutcome = chargeOutcomeService.findByUuid(outcomeUuid) ?: throw EntityNotFoundException("No charge outcome found at $outcomeUuid")

  @GetMapping("/uuid/multiple")
  @Operation(
    summary = "get all charge outcomes by uuids",
    description = "This endpoint will get all charge outcomes by uuids",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "Returns charge outcomes"),
      ApiResponse(responseCode = "401", description = "Unauthorised, requires a valid Oauth2 token"),
      ApiResponse(responseCode = "403", description = "Forbidden, requires an appropriate role"),
    ],
  )
  @ResponseStatus(HttpStatus.OK)
  fun getChargeOutcomesByIds(@RequestParam("uuids") uuids: List<UUID>): List<ChargeOutcome> = chargeOutcomeService.findByUuids(uuids)
}
