package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.documentupload

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateUploadedDocument
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.UploadedDocument
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import java.util.UUID

class CreateUploadedDocumentTest : IntegrationTestBase() {

  @Test
  fun `create uploaded document`() {
    val documentUuid = UUID.randomUUID()
    val createUploadedDocument = CreateUploadedDocument(
      appearanceUUID = null,
      documents = listOf(
        UploadedDocument(
          documentUUID = documentUuid,
          documentType = "HMCTS_WARRANT",
          fileName = "warrant.pdf",
        ),
      ),
    )

    webTestClient
      .post()
      .uri("/uploaded-documents")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .bodyValue(createUploadedDocument)
      .exchange()
      .expectStatus()
      .isCreated

    val uploadedDocument = uploadedDocumentRepository.findByDocumentUuid(documentUuid)
    assertThat(uploadedDocument).isNotNull
    assertThat(uploadedDocument?.documentType).isEqualTo(createUploadedDocument.documents.first().documentType)
  }

  @Test
  fun `no token results is unauthorized`() {
    val documentUuid = UUID.randomUUID()
    val createUploadedDocument = CreateUploadedDocument(
      appearanceUUID = null,
      documents = listOf(
        UploadedDocument(
          documentUUID = documentUuid,
          documentType = "HMCTS_WARRANT",
          fileName = "warrant.pdf",
        ),
      ),
    )

    webTestClient
      .post()
      .uri("/uploaded-documents")
      .headers {
        it.contentType = MediaType.APPLICATION_JSON
      }
      .bodyValue(createUploadedDocument)
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `token with incorrect role is forbidden`() {
    val documentUuid = UUID.randomUUID()
    val createUploadedDocument = CreateUploadedDocument(
      appearanceUUID = null,
      documents = listOf(
        UploadedDocument(
          documentUUID = documentUuid,
          documentType = "HMCTS_WARRANT",
          fileName = "warrant.pdf",
        ),
      ),
    )

    webTestClient
      .post()
      .uri("/uploaded-documents")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_UPLOADED_DOCUMENT_RO"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .bodyValue(createUploadedDocument)
      .exchange()
      .expectStatus()
      .isForbidden
  }
}
