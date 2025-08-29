package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.merge.MergeCreateCourtCasesResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.merge.MergePerson
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.service.LegacyPrisonerMergeService

@RestController
@RequestMapping("/legacy/court-case/merge/person", produces = [MediaType.APPLICATION_JSON_VALUE])
@Tag(name = "booking-controller", description = "Create operation for bookings in NOMIS. This is used when court cases are cloned onto different bookings")
class LegacyPrisonerMergeController(private val legacyPrisonerMergeService: LegacyPrisonerMergeService) {

  @PostMapping("/{retainedPrisonerNumber}")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Merge prisoner",
    description = "Merges all records between two prisoner numbers",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "201", description = "court case created"),
      ApiResponse(responseCode = "401", description = "Unauthorised, requires a valid Oauth2 token"),
      ApiResponse(responseCode = "403", description = "Forbidden, requires an appropriate role"),
    ],
  )
  @PreAuthorize("hasRole('ROLE_REMAND_AND_SENTENCING_COURT_CASE_RW')")
  fun process(@RequestBody mergePerson: MergePerson, @PathVariable retainedPrisonerNumber: String): MergeCreateCourtCasesResponse = legacyPrisonerMergeService.process(mergePerson, retainedPrisonerNumber)
}
