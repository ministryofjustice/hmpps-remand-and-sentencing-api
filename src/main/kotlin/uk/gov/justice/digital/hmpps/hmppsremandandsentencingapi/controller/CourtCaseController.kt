package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.persistence.EntityNotFoundException
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
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
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CourtAppearance
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CourtCase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateCourtCase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateCourtCaseResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.legacy.controller.dto.CourtCaseLegacyData
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.CourtCaseService
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.SnsService
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.legacy.CourtCaseReferenceService

@RestController
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
@Tag(name = "court-case-controller", description = "Court case")
class CourtCaseController(private val courtCaseService: CourtCaseService, private val courtCaseReferenceService: CourtCaseReferenceService, private val snsService: SnsService) {

  @PostMapping("/court-case")
  @PreAuthorize("hasAnyRole('ROLE_REMAND_AND_SENTENCING', 'ROLE_RELEASE_DATES_CALCULATOR')")
  @Operation(
    summary = "Create Court case",
    description = "This endpoint will create a court case",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "201", description = "Returns court case UUID"),
      ApiResponse(responseCode = "401", description = "Unauthorised, requires a valid Oauth2 token"),
      ApiResponse(responseCode = "403", description = "Forbidden, requires an appropriate role"),
    ],
  )
  @ResponseStatus(HttpStatus.CREATED)
  fun createCourtCase(@RequestBody createCourtCase: CreateCourtCase): CreateCourtCaseResponse {
    val courtCase = courtCaseService.createCourtCase(createCourtCase).also { courtCaseReferenceService.updateCourtCaseReferences(it.caseUniqueIdentifier) }
    return CreateCourtCaseResponse.from(courtCase.caseUniqueIdentifier, createCourtCase)
  }

  @PutMapping("/court-case/{courtCaseUuid}")
  @PreAuthorize("hasAnyRole('ROLE_REMAND_AND_SENTENCING', 'ROLE_RELEASE_DATES_CALCULATOR')")
  @Operation(
    summary = "Create Court case",
    description = "This endpoint will create a court case",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "201", description = "Returns court case UUID"),
      ApiResponse(responseCode = "401", description = "Unauthorised, requires a valid Oauth2 token"),
      ApiResponse(responseCode = "403", description = "Forbidden, requires an appropriate role"),
    ],
  )
  @ResponseStatus(HttpStatus.OK)
  fun putCourtCase(@RequestBody createCourtCase: CreateCourtCase, @PathVariable courtCaseUuid: String): CreateCourtCaseResponse {
    val courtCase = courtCaseService.putCourtCase(createCourtCase, courtCaseUuid).also {
      val updatedCourtCaseReferences = courtCaseReferenceService.updateCourtCaseReferences(it.caseUniqueIdentifier)
      updatedCourtCaseReferences?.takeIf { it.hasUpdated }?.let {
        snsService.legacyCaseReferencesUpdated(it.prisonerId, it.courtCaseId, it.timeUpdated)
      }
    }
    return CreateCourtCaseResponse.from(courtCaseUuid, createCourtCase)
  }

  @GetMapping("/court-case/search")
  @PreAuthorize("hasAnyRole('ROLE_REMAND_AND_SENTENCING', 'ROLE_RELEASE_DATES_CALCULATOR')")
  @Operation(
    summary = "Retrieve all court cases for person",
    description = "This endpoint will retrieve all court cases for a person",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "Returns court cases"),
      ApiResponse(responseCode = "401", description = "Unauthorised, requires a valid Oauth2 token"),
      ApiResponse(responseCode = "403", description = "Forbidden, requires an appropriate role"),
    ],
  )
  @ResponseStatus(HttpStatus.OK)
  fun searchCourtCases(@RequestParam("prisonerId") prisonerId: String, pageable: Pageable): Page<CourtCase> {
    return courtCaseService.searchCourtCases(prisonerId, pageable)
  }

  @GetMapping("\${court.case.getByIdPath}")
  @PreAuthorize("hasAnyRole('ROLE_REMAND_AND_SENTENCING', 'ROLE_RELEASE_DATES_CALCULATOR')")
  @Operation(
    summary = "Retrieve court case details",
    description = "This endpoint will retrieve court case details",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "Returns court case details"),
      ApiResponse(responseCode = "401", description = "Unauthorised, requires a valid Oauth2 token"),
      ApiResponse(responseCode = "403", description = "Forbidden, requires an appropriate role"),
      ApiResponse(responseCode = "404", description = "Not found if no court case at uuid"),
    ],
  )
  fun getCourtCaseDetails(@PathVariable courtCaseUuid: String): CourtCase {
    return courtCaseService.getCourtCaseByUuid(courtCaseUuid) ?: throw EntityNotFoundException("No court case found at $courtCaseUuid")
  }

  @GetMapping("/court-case/{courtCaseUuid}/latest-appearance")
  @PreAuthorize("hasAnyRole('ROLE_REMAND_AND_SENTENCING', 'ROLE_RELEASE_DATES_CALCULATOR')")
  @Operation(
    summary = "Retrieve latest court appearance of court case",
    description = "This endpoint will retrieve latest court appearance of court case",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "Returns latest appearance details"),
      ApiResponse(responseCode = "401", description = "Unauthorised, requires a valid Oauth2 token"),
      ApiResponse(responseCode = "403", description = "Forbidden, requires an appropriate role"),
      ApiResponse(responseCode = "404", description = "Not found if no court case at uuid"),
    ],
  )
  fun getLatestAppearanceDetails(@PathVariable courtCaseUuid: String): CourtAppearance {
    return courtCaseService.getLatestAppearanceByCourtCaseUuid(courtCaseUuid) ?: throw EntityNotFoundException("No court case found at $courtCaseUuid")
  }

  @PutMapping("/court-case/{courtCaseUuid}/case-references/refresh")
  @PreAuthorize("hasAnyRole('ROLE_REMAND_AND_SENTENCING_COURT_CASE_RW')")
  @Operation(
    summary = "Refresh case references",
    description = "This endpoint will refresh case references",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "204"),
      ApiResponse(responseCode = "401", description = "Unauthorised, requires a valid Oauth2 token"),
      ApiResponse(responseCode = "403", description = "Forbidden, requires an appropriate role"),
    ],
  )
  @ResponseStatus(HttpStatus.NO_CONTENT)
  fun refreshCaseReferences(@RequestBody courtCaseLegacyData: CourtCaseLegacyData, @PathVariable courtCaseUuid: String) {
    courtCaseReferenceService.refreshCaseReferences(courtCaseLegacyData, courtCaseUuid)
  }

  @DeleteMapping("/court-case/{courtCaseUuid}")
  @PreAuthorize("hasAnyRole('ROLE_REMAND_AND_SENTENCING_COURT_CASE_RW')")
  @Operation(
    summary = "Delete Court case",
    description = "This endpoint will delete a court case",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "204"),
      ApiResponse(responseCode = "401", description = "Unauthorised, requires a valid Oauth2 token"),
      ApiResponse(responseCode = "403", description = "Forbidden, requires an appropriate role"),
    ],
  )
  @ResponseStatus(HttpStatus.NO_CONTENT)
  fun deleteCourtCase(@PathVariable courtCaseUuid: String) {
    courtCaseService.deleteCourtCase(courtCaseUuid)
  }
}
