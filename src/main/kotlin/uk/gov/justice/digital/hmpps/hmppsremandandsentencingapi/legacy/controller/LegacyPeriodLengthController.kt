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
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacyCreatePeriodLength
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacyPeriodLengthCreatedResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.service.LegacyPeriodLengthService
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.DpsDomainEventService

@RestController
@RequestMapping("/legacy/period-length", produces = [MediaType.APPLICATION_JSON_VALUE])
@Tag(name = "legacy-period-length-controller")
class LegacyPeriodLengthController(private val legacyPeriodLengthService: LegacyPeriodLengthService, private val dpsDomainEventService: DpsDomainEventService) {

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Create a new period length of a single sentence",
    description = "Creates a new period length of a single sentence",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "201", description = "period length created"),
      ApiResponse(responseCode = "401", description = "Unauthorised, requires a valid Oauth2 token"),
      ApiResponse(responseCode = "403", description = "Forbidden, requires an appropriate role"),
    ],
  )
  @PreAuthorize("hasRole('ROLE_REMAND_AND_SENTENCING_PERIOD_LENGTH_RW')")
  fun create(@RequestBody periodLength: LegacyCreatePeriodLength): LegacyPeriodLengthCreatedResponse {
    val (periodLengthCreated, eventsToEmit) = legacyPeriodLengthService.create(periodLength)
    dpsDomainEventService.emitEvents(eventsToEmit)
    return periodLengthCreated
  }
}