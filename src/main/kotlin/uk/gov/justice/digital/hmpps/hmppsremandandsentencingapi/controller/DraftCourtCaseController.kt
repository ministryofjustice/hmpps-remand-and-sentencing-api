package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller

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
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.DraftCourtCaseCreatedResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.DraftCreateCourtCase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.draft.DraftCourtCaseService

@RestController
@RequestMapping("/draft/court-case", produces = [MediaType.APPLICATION_JSON_VALUE])
@Tag(name = "draft-court-case-controller", description = "Draft Court case")
class DraftCourtCaseController(private val draftCourtCaseService: DraftCourtCaseService) {

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Create a draft court case",
    description = "Creates a draft court case for when a user wants to pause inputting a warrant and come back later",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "201", description = "court case created"),
      ApiResponse(responseCode = "401", description = "Unauthorised, requires a valid Oauth2 token"),
      ApiResponse(responseCode = "403", description = "Forbidden, requires an appropriate role"),
    ],
  )
  @PreAuthorize("hasRole('ROLE_REMAND_AND_SENTENCING_REMAND_AND_SENTENCING_UI')")
  fun create(@RequestBody draftCourtCase: DraftCreateCourtCase): DraftCourtCaseCreatedResponse {
    return draftCourtCaseService.create(draftCourtCase)
  }
}
