package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller

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
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacyPeriodLength
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.service.LegacyPeriodLengthService
import java.util.UUID

@RestController
@RequestMapping("/legacy/period-length", produces = [MediaType.APPLICATION_JSON_VALUE])
@Tag(
  name = "legacy-period-length-controller",
  description = "CRUD operations for syncing period-length data between NOMIS and RAS (period-lengths are called sentence-terms in NOMIS)",
)
class LegacyPeriodLengthController(private val legacyPeriodLengthService: LegacyPeriodLengthService) {
  @GetMapping("/{lifetimeUuid}")
  @Operation(
    summary = "retrieve a period-length",
    description = "This endpoint will retrieve period-length details.",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "Returns period-length details"),
      ApiResponse(responseCode = "401", description = "Unauthorised, requires a valid Oauth2 token"),
      ApiResponse(responseCode = "403", description = "Forbidden, requires an appropriate role"),
    ],
  )
  @PreAuthorize("hasAnyRole('ROLE_REMAND_AND_SENTENCING_PERIOD_LENGTH_RW', 'ROLE_REMAND_AND_SENTENCING_PERIOD_LENGTH_RO')")
  fun get(@PathVariable lifetimeUuid: UUID): LegacyPeriodLength = legacyPeriodLengthService.get(lifetimeUuid)
}
