package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.DraftCreateCourtAppearance
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.DraftCourtAppearanceService
import java.util.UUID

@RestController
@RequestMapping("/draft/court-appearance", produces = [MediaType.APPLICATION_JSON_VALUE])
@Tag(name = "draft-court-appearance-controller", description = "Draft Court appearance")
class DraftCourtAppearanceController(private val draftCourtAppearanceService: DraftCourtAppearanceService) {

  @PutMapping("/{draftUuid}")
  @Operation(
    summary = "Updates a draft court appearance",
    description = "Updates a draft court appearance for when a user wants to pause inputting a warrant and come back later",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "court appearance updated"),
      ApiResponse(responseCode = "401", description = "Unauthorised, requires a valid Oauth2 token"),
      ApiResponse(responseCode = "403", description = "Forbidden, requires an appropriate role"),
    ],
  )
  @PreAuthorize("hasRole('ROLE_REMAND_AND_SENTENCING_REMAND_AND_SENTENCING_UI')")
  fun update(@PathVariable draftUuid: UUID, @RequestBody draftCourtAppearance: DraftCreateCourtAppearance) {
    draftCourtAppearanceService.update(draftUuid, draftCourtAppearance)
  }
}
