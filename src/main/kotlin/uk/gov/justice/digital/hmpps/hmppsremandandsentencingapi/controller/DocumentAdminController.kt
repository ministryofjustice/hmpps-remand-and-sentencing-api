package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.UploadedDocumentService

@RestController
@RequestMapping("/document-admin", produces = [MediaType.APPLICATION_JSON_VALUE])
@Tag(
  name = "document-admin-controller",
  description = "Endpoints for document admin operations",
)
class DocumentAdminController(private val uploadedDocumentService: UploadedDocumentService) {
  @DeleteMapping("/cleanup")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "Deletes uploaded documents without an appearance ID",
    description = "Deletes all uploaded documents where the appearance ID is null",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "Cleanup completed"),
    ],
  )
  fun cleanupDocument() {
    uploadedDocumentService.deleteDocumentsWithoutAppearanceId()
  }
}
