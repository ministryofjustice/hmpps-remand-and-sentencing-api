package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateRecall
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateRecallResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.Recall
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.RecallService
import java.util.UUID

@RestController
@RequestMapping("/recall", produces = [MediaType.APPLICATION_JSON_VALUE])
@Tag(name = "recall-controller", description = "Recall")
class RecallController(private val recallService: RecallService) {

  @PostMapping
  @PreAuthorize("hasAnyRole('ROLE_REMAND_AND_SENTENCING', 'ROLE_RELEASE_DATES_CALCULATOR')")
  @Operation(
    summary = "Create a recall",
    description = "This endpoint will create a recall",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "201", description = "Returns recall UUID"),
      ApiResponse(responseCode = "401", description = "Unauthorised, requires a valid Oauth2 token"),
      ApiResponse(responseCode = "403", description = "Forbidden, requires an appropriate role"),
    ],
  )
  @ResponseStatus(HttpStatus.CREATED)
  fun createRecall(@RequestBody createRecall: CreateRecall): CreateRecallResponse =
    recallService.createRecall(createRecall)

  @GetMapping("/{recallUuid}")
  @PreAuthorize("hasAnyRole('ROLE_REMAND_AND_SENTENCING', 'ROLE_RELEASE_DATES_CALCULATOR')")
  @Operation(
    summary = "Retrieve a recall",
    description = "This endpoint will retrieve the details of a recall",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "Returns recall details"),
      ApiResponse(responseCode = "401", description = "Unauthorised, requires a valid Oauth2 token"),
      ApiResponse(responseCode = "403", description = "Forbidden, requires an appropriate role"),
      ApiResponse(responseCode = "404", description = "Not found if no recall uuid"),
    ],
  )
  fun getRecall(@PathVariable recallUuid: UUID): Recall = recallService.findRecallByUuid(recallUuid)
}
