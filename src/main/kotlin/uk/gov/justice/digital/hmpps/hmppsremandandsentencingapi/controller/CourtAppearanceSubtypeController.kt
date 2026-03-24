package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.persistence.EntityNotFoundException
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CourtAppearanceSubtype
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ReferenceEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.CourtAppearanceSubtypeService
import java.util.UUID

@RestController
@RequestMapping("/court-appearance-subtype", produces = [MediaType.APPLICATION_JSON_VALUE])
@Tag(name = "court-appearance-subtype-controller", description = "Court appearance subtype")
class CourtAppearanceSubtypeController(private val courtAppearanceSubtypeService: CourtAppearanceSubtypeService) {

  @GetMapping("/status")
  @Operation(
    summary = "Get all court appearance subtypes by statuses",
    description = "This endpoint will get all court appearance subtypes by statuses",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "Returns court appearance subtypes"),
      ApiResponse(responseCode = "401", description = "Unauthorised, requires a valid Oauth2 token"),
    ],
  )
  fun getAllCourtAppearanceSubtypes(@RequestParam("statuses") statuses: List<ReferenceEntityStatus>): List<CourtAppearanceSubtype> = courtAppearanceSubtypeService.getAllByStatus(statuses)

  @GetMapping("/{courtAppearanceSubtypeUuid}")
  @Operation(
    summary = "Get court appearance subtype by uuid",
    description = "This endpoint will get court appearance subtype by uuid",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "Returns court appearance subtype"),
      ApiResponse(responseCode = "404", description = "Appearance type not found"),
      ApiResponse(responseCode = "401", description = "Unauthorised, requires a valid Oauth2 token"),
    ],
  )
  fun getCourtAppearanceSubtypeById(@PathVariable courtAppearanceSubtypeUuid: UUID): CourtAppearanceSubtype = courtAppearanceSubtypeService.findByUuid(courtAppearanceSubtypeUuid) ?: throw EntityNotFoundException("No court appearance subtype found at $courtAppearanceSubtypeUuid")
}
