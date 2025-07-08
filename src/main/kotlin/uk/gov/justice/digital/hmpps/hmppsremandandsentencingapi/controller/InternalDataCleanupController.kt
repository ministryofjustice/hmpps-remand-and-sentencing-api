package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.UploadedDocumentService

@RestController
@RequestMapping("/internal", produces = [MediaType.APPLICATION_JSON_VALUE])
@Tag(
  name = "internal-data-cleanup-controller",
  description = "Endpoint for internal data cleanup operations",
)
class InternalDataCleanupController(private val uploadedDocumentService: UploadedDocumentService) {
  @PostMapping("/cleanup-document-table")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "Deletes uploaded documents without an appearance ID",
    description = "Deletes all uploaded documents where the appearance ID is null"
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "Cleanup completed"),
    ]
  )
  fun cleanup() {
    uploadedDocumentService.deleteDocumentsWithoutAppearanceId()
  }
}