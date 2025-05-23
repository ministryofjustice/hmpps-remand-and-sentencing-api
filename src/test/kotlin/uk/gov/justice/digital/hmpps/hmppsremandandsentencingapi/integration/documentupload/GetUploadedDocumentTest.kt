package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.documentupload

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.UploadedDocument
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.UploadedDocumentRepository
import java.util.*

class GetUploadedDocumentTest : IntegrationTestBase() {
  @Autowired
  private lateinit var uploadedDocumentRepository: UploadedDocumentRepository

  @Test
  fun `retrieve uploaded documents by appearance UUID and warrant type`() {
    val createdCourtAppearance = createLegacyCourtAppearance()
    val documentUuid = UUID.randomUUID()
    createUploadedDocument(documentUuid, createdCourtAppearance.first)

    val uploadedDocuments = webTestClient
      .get()
      .uri { uriBuilder ->
        uriBuilder.path("/uploaded-documents")
          .queryParam("warrantType", "SENTENCING")
          .queryParam("appearanceUuid", createdCourtAppearance.first)
          .build()
      }
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_UPLOADED_DOCUMENT_RW"))
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBodyList(UploadedDocument::class.java)
      .returnResult()
      .responseBody

    assertThat(uploadedDocuments).isNotNull
    assertThat(uploadedDocuments).hasSize(1)
    assertThat(uploadedDocuments?.first()?.documentUUID).isEqualTo(documentUuid)
  }

  @Test
  fun `no token results is unauthorized`() {
    val createdCourtAppearance = createLegacyCourtAppearance()
    val documentUuid = UUID.randomUUID()
    createUploadedDocument(documentUuid, createdCourtAppearance.first)

    webTestClient
      .get()
      .uri { uriBuilder ->
        uriBuilder.path("/uploaded-documents")
          .queryParam("warrantType", "SENTENCING")
          .queryParam("appearanceUuid", createdCourtAppearance.first)
          .build()
      }
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `token with incorrect role is forbidden`() {
    val createdCourtAppearance = createLegacyCourtAppearance()
    val documentUuid = UUID.randomUUID()
    createUploadedDocument(documentUuid, createdCourtAppearance.first)

    webTestClient
      .get()
      .uri { uriBuilder ->
        uriBuilder.path("/uploaded-documents")
          .queryParam("warrantType", "SENTENCING")
          .queryParam("appearanceUuid", createdCourtAppearance.first)
          .build()
      }
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_UPLOADED_DOCUMENT_RO"))
      }
      .exchange()
      .expectStatus()
      .isForbidden
  }
}
