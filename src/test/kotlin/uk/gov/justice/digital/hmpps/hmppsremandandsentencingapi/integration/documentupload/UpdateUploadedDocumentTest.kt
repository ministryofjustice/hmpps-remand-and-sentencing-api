package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.documentupload

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.UploadedDocumentRepository
import java.util.*

class UpdateUploadedDocumentTest : IntegrationTestBase() {
  @Autowired
  lateinit var uploadedDocumentRepository: UploadedDocumentRepository

  @Test
  fun `update uploaded document`() {
    val createdCourtAppearance = createLegacyCourtAppearance()
    val documentUuid = UUID.randomUUID()
    createUploadedDocument(documentUuid)
    webTestClient
      .put()
      .uri { uriBuilder ->
        uriBuilder.path("/uploaded-documents")
          .queryParam("documentUuids", listOf(documentUuid))
          .queryParam("appearanceUuid", createdCourtAppearance.first)
          .build()
      }
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_UPLOADED_DOCUMENT_RW"))
      }
      .exchange()
      .expectStatus()
      .isOk

    val updatedDocument = uploadedDocumentRepository.findByDocumentUuid(documentUuid).orElseThrow()
    assertThat(updatedDocument.appearance?.appearanceUuid).isEqualTo(createdCourtAppearance.first)
  }

  @Test
  fun `no token results is unauthorized`() {
    val documentUuid = UUID.randomUUID()
    val createdCourtAppearance = createLegacyCourtAppearance()
    createUploadedDocument(documentUuid)
    webTestClient
      .put()
      .uri { uriBuilder ->
        uriBuilder.path("/uploaded-documents")
          .queryParam("documentUuids", listOf(documentUuid))
          .queryParam("appearanceUuid", createdCourtAppearance.first)
          .build()
      }
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `token with incorrect role is forbidden`() {
    val documentUuid = UUID.randomUUID()
    val createdCourtAppearance = createLegacyCourtAppearance()
    createUploadedDocument(documentUuid)
    webTestClient
      .put()
      .uri { uriBuilder ->
        uriBuilder.path("/uploaded-documents")
          .queryParam("documentUuids", listOf(documentUuid))
          .queryParam("appearanceUuid", createdCourtAppearance.first)
          .build()
      }
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_UPLOADED_DOCUMENT_R"))
      }
      .exchange()
      .expectStatus()
      .isForbidden
  }


}


