package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacySentenceType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.model.LegacySentenceTypeGroupingSummary
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.service.LegacySentenceTypesService

@RestController
@RequestMapping("/legacy/sentence-type", produces = [MediaType.APPLICATION_JSON_VALUE])
@Tag(name = "sentence-type-controller", description = "Sentence Type")
class LegacySentenceTypeController(private val legacySentenceTypesService: LegacySentenceTypesService) {

  @GetMapping("/")
  @Operation(
    summary = "Get historic NOMIS sentence type by nomis sentence type reference",
    description = "Returns historic NOMIS sentence type information for the specified type in a detailed format",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "Returns historic NOMIS sentence type"),
      ApiResponse(responseCode = "401", description = "Unauthorised, requires a valid OAuth2 token"),
      ApiResponse(responseCode = "403", description = "Forbidden, requires an appropriate role"),
      ApiResponse(responseCode = "404", description = "Not found if no sentence type at given NOMIS type"),
    ],
  )
  @ResponseStatus(HttpStatus.OK)
  fun getLegacySentenceType(@RequestParam nomisSentenceTypeReference: String): List<LegacySentenceType> = legacySentenceTypesService.getLegacySentencesByNomisSentenceTypeReference(nomisSentenceTypeReference)

  @GetMapping("/summary")
  @Operation(
    summary = "Get historic NOMIS sentence type by nomis sentence type reference",
    description = "Returns historic NOMIS sentence type information for the specified type in a summary format",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "Returns historic NOMIS sentence type"),
      ApiResponse(responseCode = "401", description = "Unauthorised, requires a valid OAuth2 token"),
      ApiResponse(responseCode = "403", description = "Forbidden, requires an appropriate role"),
      ApiResponse(responseCode = "404", description = "Not found if no sentence type at given NOMIS type"),
    ],
  )
  @ResponseStatus(HttpStatus.OK)
  fun getLegacySentenceTypeSummary(@RequestParam nomisSentenceTypeReference: String): LegacySentenceTypeGroupingSummary = legacySentenceTypesService.getLegacySentencesByNomisSentenceTypeReferenceAsSummary(nomisSentenceTypeReference)

  @GetMapping("/all")
  @Operation(
    summary = "Get all historic NOMIS sentence types",
    description = "Returns a set of historic NOMIS sentence type information in a detailed format",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "Returns historic NOMIS sentence type"),
      ApiResponse(responseCode = "401", description = "Unauthorised, requires a valid OAuth2 token"),
      ApiResponse(responseCode = "403", description = "Forbidden, requires an appropriate role"),
      ApiResponse(responseCode = "404", description = "Not found if no sentence type at given NOMIS type"),
    ],
  )
  @ResponseStatus(HttpStatus.OK)
  fun getLegacyAllSentenceTypes(): List<LegacySentenceType> = legacySentenceTypesService.getAllLegacySentences()

  @GetMapping("/all/summary")
  @Operation(
    summary = "Get summary of all historic NOMIS sentence types",
    description = "Returns a set of historic NOMIS sentence type information in a summary format",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "Returns historic NOMIS sentence type"),
      ApiResponse(responseCode = "401", description = "Unauthorised, requires a valid OAuth2 token"),
      ApiResponse(responseCode = "403", description = "Forbidden, requires an appropriate role"),
      ApiResponse(responseCode = "404", description = "Not found if no sentence type at given NOMIS type"),
    ],
  )
  @ResponseStatus(HttpStatus.OK)
  fun getLegacyAllSentenceTypesSummaries(): List<LegacySentenceTypeGroupingSummary> = legacySentenceTypesService.getGroupedLegacySummaries()
}
