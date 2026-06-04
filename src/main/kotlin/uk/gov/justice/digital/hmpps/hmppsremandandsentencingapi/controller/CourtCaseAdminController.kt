package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.BulkFixManyChargesToSentenceService

@RestController
@RequestMapping("/court-case-admin", produces = [MediaType.APPLICATION_JSON_VALUE])
@Tag(name = "court-case-admin-controller", description = "Court case")
class CourtCaseAdminController(
  private val bulkFixManyChargesToSentenceService: BulkFixManyChargesToSentenceService,
  @Value("\${multiple-charges-single-sentence-fix-queue.limit:500}") private val limit: Int,
) {

  @PostMapping("/cleanup-many-charges-to-sentence")
  @ResponseStatus(HttpStatus.ACCEPTED)
  @Operation(
    summary = "Fixes the single sentence to many charges issue",
    description = """
      NOMIS contains an erroneous relationship between sentences and charges where a single sentence is 
      linked to multiple charges. This endpoint identifies all court cases with this issue and creates 
      new sentences for each charge, ensuring that each sentence is correctly linked to its respective charge.
      """,
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "202", description = "Cleanup tiggered"),
    ],
  )
  fun cleanupManyChargesToSentence() {
    bulkFixManyChargesToSentenceService.fixCourtCaseSentences(limit)
  }
}
