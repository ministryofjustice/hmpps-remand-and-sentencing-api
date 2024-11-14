package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.persistence.EntityNotFoundException
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CourtAppearance
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateCharge
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateChargeResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateCourtAppearance
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateCourtAppearanceResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.CourtAppearanceService
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.SnsService
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.legacy.CourtCaseReferenceService
import java.util.UUID

@RestController
@Tag(name = "court-appearance-controller", description = "Court Appearances")
class CourtAppearanceController(private val courtAppearanceService: CourtAppearanceService, private val courtCaseReferenceService: CourtCaseReferenceService, private val snsService: SnsService) {

  @PostMapping("/court-appearance")
  @PreAuthorize("hasAnyRole('ROLE_REMAND_AND_SENTENCING', 'ROLE_RELEASE_DATES_CALCULATOR')")
  @Operation(
    summary = "Create Court appearance",
    description = "This endpoint will create a court appearance in a given court case",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "201", description = "Returns court case UUID"),
      ApiResponse(responseCode = "401", description = "Unauthorised, requires a valid Oauth2 token"),
      ApiResponse(responseCode = "403", description = "Forbidden, requires an appropriate role"),
    ],
  )
  @ResponseStatus(HttpStatus.CREATED)
  fun createCourtAppearance(@RequestBody createCourtAppearance: CreateCourtAppearance): CreateCourtAppearanceResponse {
    return courtAppearanceService.createCourtAppearance(createCourtAppearance)?.let { appearance ->
      courtCaseReferenceService.updateCourtCaseReferences(createCourtAppearance.courtCaseUuid!!)?.takeIf { it.hasUpdated }?.let {
        snsService.legacyCaseReferencesUpdated(it.prisonerId, it.courtCaseId, it.timeUpdated)
      }
      CreateCourtAppearanceResponse.from(createCourtAppearance)
    } ?: throw EntityNotFoundException("No court case found at ${createCourtAppearance.courtCaseUuid}")
  }

  @GetMapping("\${court.appearance.getByIdPath}")
  @PreAuthorize("hasAnyRole('ROLE_REMAND_AND_SENTENCING', 'ROLE_RELEASE_DATES_CALCULATOR')")
  @Operation(
    summary = "Retrieve court appearance details",
    description = "This endpoint will retrieve court appearance details",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "Returns court appearance details"),
      ApiResponse(responseCode = "401", description = "Unauthorised, requires a valid Oauth2 token"),
      ApiResponse(responseCode = "403", description = "Forbidden, requires an appropriate role"),
      ApiResponse(responseCode = "404", description = "Not found if no court appearance at uuid"),
    ],
  )
  fun getCourtAppearanceDetails(@PathVariable appearanceUuid: UUID): CourtAppearance {
    return courtAppearanceService.findAppearanceByUuid(appearanceUuid) ?: throw EntityNotFoundException("No court appearance found at $appearanceUuid")
  }

  @GetMapping("\${court.appearance.getByLifetimeIdPath}")
  @PreAuthorize("hasAnyRole('ROLE_REMAND_AND_SENTENCING_APPEARANCE_RW', 'ROLE_REMAND_AND_SENTENCING_APPEARANCE_RO')")
  @Operation(
    summary = "Retrieve court appearance details",
    description = "This endpoint will retrieve court appearance details",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "Returns court appearance details"),
      ApiResponse(responseCode = "401", description = "Unauthorised, requires a valid Oauth2 token"),
      ApiResponse(responseCode = "403", description = "Forbidden, requires an appropriate role"),
      ApiResponse(responseCode = "404", description = "Not found if no court appearance at uuid"),
    ],
  )
  fun getCourtAppearanceDetailsByLifetime(@PathVariable lifetimeUuid: UUID): CourtAppearance {
    return courtAppearanceService.findAppearanceByLifetimeUuid(lifetimeUuid) ?: throw EntityNotFoundException("No court appearance found at $lifetimeUuid")
  }

  @PutMapping("/court-appearance/{appearanceUuid}")
  @PreAuthorize("hasAnyRole('ROLE_REMAND_AND_SENTENCING', 'ROLE_RELEASE_DATES_CALCULATOR')")
  @Operation(
    summary = "Create Court appearance",
    description = "This endpoint will create a court appearance in a given court case",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "Returns court case UUID"),
      ApiResponse(responseCode = "401", description = "Unauthorised, requires a valid Oauth2 token"),
      ApiResponse(responseCode = "403", description = "Forbidden, requires an appropriate role"),
    ],
  )
  @ResponseStatus(HttpStatus.OK)
  fun updateCourtAppearance(@RequestBody createCourtAppearance: CreateCourtAppearance, @PathVariable appearanceUuid: UUID): CreateCourtAppearanceResponse {
    return courtAppearanceService.createCourtAppearance(createCourtAppearance.copy(appearanceUuid = appearanceUuid))?.let { appearance ->
      courtCaseReferenceService.updateCourtCaseReferences(createCourtAppearance.courtCaseUuid!!)?.takeIf { it.hasUpdated }?.let {
        snsService.legacyCaseReferencesUpdated(it.prisonerId, it.courtCaseId, it.timeUpdated)
      }
      CreateCourtAppearanceResponse.from(createCourtAppearance)
    } ?: throw EntityNotFoundException("No court case found at ${createCourtAppearance.courtCaseUuid}")
  }

  @PostMapping("/court-appearance/{appearanceUuid}/charge")
  @PreAuthorize("hasAnyRole('ROLE_REMAND_AND_SENTENCING_CHARGE_RW')")
  @Operation(
    summary = "Create Charge in appearance",
    description = "This endpoint will create a charge in a given court appearance",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "201", description = "Returns charge UUID"),
      ApiResponse(responseCode = "401", description = "Unauthorised, requires a valid Oauth2 token"),
      ApiResponse(responseCode = "403", description = "Forbidden, requires an appropriate role"),
    ],
  )
  @ResponseStatus(HttpStatus.CREATED)
  fun createChargeInAppearance(@RequestBody createCharge: CreateCharge, @PathVariable appearanceUuid: UUID): CreateChargeResponse {
    return courtAppearanceService.createChargeInAppearance(createCharge.copy(appearanceUuid = appearanceUuid))?.let { CreateChargeResponse.from(createCharge) } ?: throw EntityNotFoundException("No appearance found at $appearanceUuid")
  }

  @DeleteMapping("/court-appearance/{appearanceUuid}")
  @PreAuthorize("hasAnyRole('ROLE_REMAND_AND_SENTENCING_APPEARANCE_RW')")
  @Operation(
    summary = "Delete Appearance",
    description = "This endpoint will delete an appearance",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "204"),
      ApiResponse(responseCode = "401", description = "Unauthorised, requires a valid Oauth2 token"),
      ApiResponse(responseCode = "403", description = "Forbidden, requires an appropriate role"),
    ],
  )
  @ResponseStatus(HttpStatus.NO_CONTENT)
  fun deleteAppearance(@PathVariable appearanceUuid: UUID) {
    courtAppearanceService.deleteCourtAppearance(appearanceUuid)
  }

  @DeleteMapping("/court-appearance/{appearanceUuid}/charge/{chargeUuid}")
  @PreAuthorize("hasAnyRole('ROLE_REMAND_AND_SENTENCING_APPEARANCE_RW')")
  @Operation(
    summary = "Disassociate charge with appearance",
    description = "This endpoint will disassociate a charge with an appearance",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "204"),
      ApiResponse(responseCode = "401", description = "Unauthorised, requires a valid Oauth2 token"),
      ApiResponse(responseCode = "403", description = "Forbidden, requires an appropriate role"),
    ],
  )
  @ResponseStatus(HttpStatus.NO_CONTENT)
  fun disassociateChargeWithAppearance(@PathVariable appearanceUuid: UUID, @PathVariable chargeUuid: UUID) {
    courtAppearanceService.disassociateChargeWithAppearance(appearanceUuid, chargeUuid)
  }
}
