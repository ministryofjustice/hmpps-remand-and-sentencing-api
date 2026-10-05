package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.core.io.InputStreamResource
import org.springframework.core.io.Resource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.documents.CreateLodgeWarrants986
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.LodgeWarrants986Service
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.UploadedDocumentService

@RestController
@RequestMapping("/document-admin", produces = [MediaType.APPLICATION_JSON_VALUE])
@Tag(
  name = "document-admin-controller",
  description = "Endpoints for document admin operations",
)
class DocumentAdminController(
  private val uploadedDocumentService: UploadedDocumentService,
  private val lodgeWarrants986Service: LodgeWarrants986Service,
) {
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

  @GetMapping("/test")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "Get Doc",
    description = "Gets a document by ID",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "Got document"),
    ],
  )
  fun deleteMe(): ResponseEntity<Resource> {
    val fileName = "dummy-doc.pdf"
    val fileStream = lodgeWarrants986Service.renderDocument(CreateLodgeWarrants986("G0612UQ", "COURT NAME", "PREMISE", "STREET", "TOWN", "COUNTY", "POSTCODE", "0123456789"))
    val resource = InputStreamResource(fileStream)

    return ResponseEntity.ok().header(
      HttpHeaders.CONTENT_DISPOSITION,
      "attachment; filename=\"$fileName\"",
    )
      .contentType(MediaType.APPLICATION_PDF)
      .body(resource)
  }
}
