package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.event.EventSource
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacyCreatePeriodLength
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacyPeriodLength
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacyPeriodLengthCreatedResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.service.LegacyPeriodLengthService
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.PeriodLengthDomainEventService
import java.util.UUID

@RestController
@RequestMapping("/legacy/period-length", produces = [MediaType.APPLICATION_JSON_VALUE])
@Tag(
  name = "legacy-period-length-controller",
  description = "CRUD operations for syncing period-length data between NOMIS and RAS (period-lengths are called sentence-terms in NOMIS)",
)
class LegacyPeriodLengthController(
  private val legacyPeriodLengthService: LegacyPeriodLengthService,
  private val eventService: PeriodLengthDomainEventService,
) {

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Creates one or more period lengths for a single sentence",
    description = "Creates one or more period lengths for a single sentence",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "201", description = "period length created"),
      ApiResponse(responseCode = "401", description = "Unauthorised, requires a valid Oauth2 token"),
      ApiResponse(responseCode = "403", description = "Forbidden, requires an appropriate role"),
    ],
  )
  @PreAuthorize("hasRole('ROLE_REMAND_AND_SENTENCING_PERIOD_LENGTH_RW')")
  fun create(@RequestBody periodLength: LegacyCreatePeriodLength): LegacyPeriodLengthCreatedResponse = legacyPeriodLengthService.create(periodLength).also { periodLengthCreated ->
    eventService.create(
      prisonerId = periodLengthCreated.prisonerId,
      courtCaseId = periodLengthCreated.courtCaseId,
      courtAppearanceId = periodLengthCreated.appearanceUuid.toString(),
      sentenceId = periodLengthCreated.sentenceUuid.toString(),
      periodLengthId = periodLengthCreated.periodLengthUuid.toString(),
      source = EventSource.NOMIS,
      courtChargeId = periodLengthCreated.chargeUuid.toString(),
    )
  }

  @GetMapping("/{periodLengthUuid}")
  @Operation(
    summary = "retrieve a period-length",
    description = "This endpoint will retrieve period-length details.",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "Returns period-length details"),
      ApiResponse(responseCode = "401", description = "Unauthorised, requires a valid Oauth2 token"),
      ApiResponse(responseCode = "403", description = "Forbidden, requires an appropriate role"),
      ApiResponse(
        responseCode = "404",
        description = "Not found: Period length either doesn't exist, has no sentence, or is deleted",
      ),
    ],
  )
  @PreAuthorize("hasAnyRole('ROLE_REMAND_AND_SENTENCING_PERIOD_LENGTH_RW', 'ROLE_REMAND_AND_SENTENCING_PERIOD_LENGTH_RO')")
  fun get(@PathVariable periodLengthUuid: UUID): LegacyPeriodLength = legacyPeriodLengthService.get(periodLengthUuid)

  @DeleteMapping("/{periodLengthUuid}")
  @Operation(
    summary = "Delete Period Length",
    description = "This endpoint will soft-delete a period-length from RAS.",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "204"),
      ApiResponse(responseCode = "401", description = "Unauthorised, requires a valid Oauth2 token"),
      ApiResponse(responseCode = "403", description = "Forbidden, requires an appropriate role"),
    ],
  )
  @PreAuthorize("hasRole('ROLE_REMAND_AND_SENTENCING_PERIOD_LENGTH_RW')")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  fun delete(@PathVariable periodLengthUuid: UUID) {
    legacyPeriodLengthService.deletePeriodLengthWithSentence(periodLengthUuid)?.also { legacyPeriodLength ->
      eventService.delete(
        prisonerId = legacyPeriodLength.prisonerId,
        periodLengthId = legacyPeriodLength.periodLengthUuid.toString(),
        sentenceId = legacyPeriodLength.sentenceUuid.toString(),
        courtChargeId = legacyPeriodLength.courtChargeId.toString(),
        courtCaseId = legacyPeriodLength.courtCaseId,
        courtAppearanceId = legacyPeriodLength.courtAppearanceId.toString(),
        source = EventSource.NOMIS,
      )
    }
  }

  @PutMapping("/{periodLengthUuid}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
    summary = "Updates one or more period lengths related to a period-length UUID",
    description = "Updates one or more period lengths related to a period-length UUID",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "204", description = "No content"),
      ApiResponse(responseCode = "401", description = "Unauthorised, requires a valid Oauth2 token"),
      ApiResponse(responseCode = "403", description = "Forbidden, requires an appropriate role"),
      ApiResponse(responseCode = "404", description = "Period length or sentence not found"),
    ],
  )
  @PreAuthorize("hasRole('ROLE_REMAND_AND_SENTENCING_PERIOD_LENGTH_RW')")
  fun update(
    @PathVariable periodLengthUuid: UUID,
    @RequestBody periodLength: LegacyCreatePeriodLength,
  ) {
    legacyPeriodLengthService.update(periodLengthUuid, periodLength)?.let { updatedPeriodLength ->
      eventService.update(
        prisonerId = updatedPeriodLength.prisonerId,
        courtCaseId = updatedPeriodLength.courtCaseId,
        courtAppearanceId = updatedPeriodLength.appearanceUuid.toString(),
        sentenceId = updatedPeriodLength.sentenceUuid.toString(),
        periodLengthId = updatedPeriodLength.periodLengthUuid.toString(),
        source = EventSource.NOMIS,
        courtChargeId = updatedPeriodLength.chargeUuid.toString(),
      )
    }
  }
}
