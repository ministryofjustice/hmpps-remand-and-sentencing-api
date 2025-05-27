package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateUploadedDocument
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.UploadedDocumentService
import java.util.*

@RestController
@RequestMapping("/uploaded-documents", produces = [MediaType.APPLICATION_JSON_VALUE])
@Tag(
  name = "uploaded-documents-controller",
  description = "CRUD operations for keeping a record of  uploaded documents to document service",
)
class UploadedDocumentController(private val uploadedDocumentService: UploadedDocumentService) {

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Creates an uploaded document entry",
    description = "Creates an uploaded document entry",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "201", description = "uploaded document created"),
      ApiResponse(responseCode = "401", description = "Unauthorised, requires a valid Oauth2 token"),
      ApiResponse(responseCode = "403", description = "Forbidden, requires an appropriate role"),
    ],
  )
  @PreAuthorize("hasRole('ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI')")
  fun create(@RequestBody createUploadedDocument: CreateUploadedDocument) =
    uploadedDocumentService.create(createUploadedDocument)
}
