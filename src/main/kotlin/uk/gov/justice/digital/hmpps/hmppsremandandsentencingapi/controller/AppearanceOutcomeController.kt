package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
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
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.config.FieldErrorErrorResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CourtAppearanceOutcome
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.appearanceoutcome.CreateAppearanceOutcome
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ReferenceEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.AppearanceOutcomeService
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.datafix.MigrateCourtAppearanceRecordOutcomes
import java.util.UUID

@RestController
@RequestMapping("/appearance-outcome", produces = [MediaType.APPLICATION_JSON_VALUE])
@Tag(name = "appearance-outcome-controller", description = "Appearance outcome")
class AppearanceOutcomeController(private val appearanceOutcomeService: AppearanceOutcomeService, private val migrateCourtAppearanceRecordOutcomes: MigrateCourtAppearanceRecordOutcomes) {

  @PostMapping
  @Operation(
    summary = "Create appearance outcome",
    description = "This endpoint will create a new appearance outcome and migrate any charge data over that needs to be mapped to the newly created appearance outcome",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "201"),
      ApiResponse(responseCode = "401", description = "Unauthorised, requires a valid Oauth2 token"),
      ApiResponse(responseCode = "403", description = "Forbidden, requires an appropriate role"),
      ApiResponse(responseCode = "400", description = "Bad request", content = [Content(mediaType = "application/json", schema = Schema(implementation = FieldErrorErrorResponse::class))]),
    ],
  )
  @ResponseStatus(HttpStatus.CREATED)
  fun createAppearanceOutcome(@Valid @RequestBody createAppearanceOutcome: CreateAppearanceOutcome): CourtAppearanceOutcome = appearanceOutcomeService.createAppearanceOutcome(createAppearanceOutcome).let { createdAppearanceOutcomeEntity ->
    migrateCourtAppearanceRecordOutcomes.migrateCourtAppearanceRecordsToOutcome(createdAppearanceOutcomeEntity)
    CourtAppearanceOutcome.from(createdAppearanceOutcomeEntity)
  }

  @GetMapping("/status")
  @Operation(
    summary = "Get all appearance outcomes by status",
    description = "This endpoint will get all appearance outcomes by status",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "Returns appearance outcomes"),
      ApiResponse(responseCode = "401", description = "Unauthorised, requires a valid Oauth2 token"),
      ApiResponse(responseCode = "403", description = "Forbidden, requires an appropriate role"),
    ],
  )
  @ResponseStatus(HttpStatus.OK)
  fun getAllAppearanceOutcomes(@RequestParam("statuses") statuses: List<ReferenceEntityStatus>): List<CourtAppearanceOutcome> = appearanceOutcomeService.getAllByStatus(statuses)

  @GetMapping("/{outcomeUuid}")
  @Operation(
    summary = "Get appearance outcome by UUID",
    description = "This endpoint will retrieve appearance outcome by UUID",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "Returns appearance outcome"),
      ApiResponse(responseCode = "401", description = "Unauthorised, requires a valid Oauth2 token"),
      ApiResponse(responseCode = "403", description = "Forbidden, requires an appropriate role"),
      ApiResponse(responseCode = "404", description = "Not found if no appearance outcome at uuid"),
    ],
  )
  @ResponseStatus(HttpStatus.OK)
  fun getAppearanceOutcomeByUuid(@PathVariable outcomeUuid: UUID): CourtAppearanceOutcome = appearanceOutcomeService.findByUuid(outcomeUuid) ?: throw EntityNotFoundException("No appearance outcome found at $outcomeUuid")
}
