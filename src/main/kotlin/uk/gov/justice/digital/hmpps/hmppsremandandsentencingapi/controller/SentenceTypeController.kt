package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller

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
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.SentenceType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.SentenceTypeService
import java.time.LocalDate

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
  fun searchSentenceTypes(@RequestParam("age") age: Int, @RequestParam("convictionDate") convictionDate: LocalDate): List<SentenceType> {
    return sentenceTypesService.search(age, convictionDate)
  }
}
