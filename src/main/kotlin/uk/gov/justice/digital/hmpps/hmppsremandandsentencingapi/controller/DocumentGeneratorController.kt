package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.core.io.InputStreamResource
import org.springframework.core.io.Resource
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.documents.CreateF986
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.F986Service
import java.util.UUID

@RestController
@RequestMapping("/document-generator", produces = [MediaType.APPLICATION_PDF_VALUE])
@Tag(
  name = "document-generation-controller",
  description = "Endpoint for generating documents",
)
class DocumentGeneratorController(private val f986Service: F986Service) {

  @PostMapping("/f986")
  @PreAuthorize("hasAnyRole('ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI')")
  @Operation(
    summary = "Create F986 Document",
    description = "This endpoint will create an F986 document",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "Returns F986 PDF document stream"),
      ApiResponse(responseCode = "400", description = "Bad request - no results found based on courtAppearanceUuid"),
      ApiResponse(responseCode = "401", description = "Unauthorised, requires a valid Oauth2 token"),
      ApiResponse(responseCode = "403", description = "Forbidden, requires an appropriate role"),
    ],
  )
  fun createF986Document(@RequestBody createF986: CreateF986): ResponseEntity<Resource> {
    val fileName = "f986-${UUID.randomUUID()}.pdf"
    val fileStream = f986Service.renderDocument(createF986)

    return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"$fileName\"")
      .contentType(MediaType.APPLICATION_PDF)
      .body(InputStreamResource(fileStream))
  }
}
