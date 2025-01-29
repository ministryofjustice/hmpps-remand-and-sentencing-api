package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.persistence.EntityNotFoundException
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.SentenceType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.SentenceTypeService
import java.time.LocalDate
import java.util.UUID

@RestController
@RequestMapping("/sentence-type", produces = [MediaType.APPLICATION_JSON_VALUE])
@Tag(name = "sentence-type-controller", description = "Sentence Type")
class SentenceTypeController(private val sentenceTypesService: SentenceTypeService) {

  @GetMapping("/search")
  @Operation(
    summary = "Search all sentence types",
    description = "This endpoint will search all sentence types",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "Returns sentence types"),
      ApiResponse(responseCode = "401", description = "Unauthorised, requires a valid Oauth2 token"),
      ApiResponse(responseCode = "403", description = "Forbidden, requires an appropriate role"),
    ],
  )
  @ResponseStatus(HttpStatus.OK)
  fun searchSentenceTypes(@RequestParam("age") age: Int, @RequestParam("convictionDate") convictionDate: LocalDate): List<SentenceType> = sentenceTypesService.search(age, convictionDate)

  @GetMapping("/{sentenceTypeUuid}")
  @Operation(
    summary = "Get Sentence type by UUID",
    description = "This endpoint will retrieve sentence type by UUID",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "Returns sentence"),
      ApiResponse(responseCode = "401", description = "Unauthorised, requires a valid Oauth2 token"),
      ApiResponse(responseCode = "403", description = "Forbidden, requires an appropriate role"),
      ApiResponse(responseCode = "404", description = "Not found if no sentence type at uuid"),
    ],
  )
  @ResponseStatus(HttpStatus.OK)
  fun getSentenceTypeByUuid(@PathVariable sentenceTypeUuid: UUID): SentenceType = sentenceTypesService.findByUuid(sentenceTypeUuid) ?: throw EntityNotFoundException("No sentence type found at $sentenceTypeUuid")

  @GetMapping("/uuid/multiple")
  @Operation(
    summary = "get all sentence types by uuids",
    description = "This endpoint will get all sentence types by uuids",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "Returns sentence types"),
      ApiResponse(responseCode = "401", description = "Unauthorised, requires a valid Oauth2 token"),
      ApiResponse(responseCode = "403", description = "Forbidden, requires an appropriate role"),
    ],
  )
  @ResponseStatus(HttpStatus.OK)
  fun getSentenceTypesByIds(@RequestParam("uuids") uuids: List<UUID>): List<SentenceType> = sentenceTypesService.findByUuids(uuids)
}
