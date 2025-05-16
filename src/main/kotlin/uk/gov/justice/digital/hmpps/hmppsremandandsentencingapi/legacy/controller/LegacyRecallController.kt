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
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacyRecall
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.service.LegacyRecallService
import java.util.UUID

@RestController
@RequestMapping("/legacy/recall", produces = [MediaType.APPLICATION_JSON_VALUE])
@Tag(name = "legacy-recall-controller", description = "GET operation for syncing recall data. This controller is only ever used for syncing DPS recalls to NOMIS.")
class LegacyRecallController(private val legacyRecallService: LegacyRecallService) {

  @GetMapping("/{uuid}")
  @Operation(
    summary = "retrieve a recall",
    description = "This endpoint will retrieve legacy recall details.",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "Returns recall details"),
      ApiResponse(responseCode = "401", description = "Unauthorised, requires a valid Oauth2 token"),
      ApiResponse(responseCode = "403", description = "Forbidden, requires an appropriate role"),
    ],
  )
  @PreAuthorize("hasAnyRole('ROLE_REMAND_AND_SENTENCING_SENTENCE_RW', 'ROLE_REMAND_AND_SENTENCING_SENTENCE_RO')")
  fun get(@PathVariable uuid: UUID): LegacyRecall = legacyRecallService.get(uuid)
}
