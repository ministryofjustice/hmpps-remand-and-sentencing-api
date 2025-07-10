package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.documentupload

import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.mockito.Mockito.verify
import org.mockito.Mockito.mock
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.DocumentManagementApiClient
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.UploadedDocumentEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.UploadedDocumentRepository
import java.time.ZonedDateTime
import java.util.UUID

class CleanupDocumentTest : IntegrationTestBase() {

  @Autowired
  lateinit var uploadedDocumentRepository: UploadedDocumentRepository

  @Autowired
  lateinit var documentManagementApiClient: DocumentManagementApiClient

  @Test
  fun `cleanup deletes old documents without appearance`() {
    val documentUuid = UUID.randomUUID()
    val oldDocument = UploadedDocumentEntity(
      id = 0,
      documentUuid = documentUuid,
      appearance = null,
      createdBy = "test",
      createdAt = ZonedDateTime.now().minusDays(11),
      updatedBy = null,
      updatedAt = null,
      documentType = "HMCTS_WARRANT",
      fileName = "old_warrant.pdf"
    )
    uploadedDocumentRepository.save(oldDocument)

    webTestClient
      .post()
      .uri("/document-admin/cleanup")
      .headers { it.contentType = MediaType.APPLICATION_JSON }
      .exchange()
      .expectStatus().isOk

    val found = uploadedDocumentRepository.findByDocumentUuid(documentUuid)
    assertNull(found)

    verify(documentManagementApiClient).deleteDocument(documentUuid.toString())
  }

  @TestConfiguration
  class MockConfig {
    @Bean
    fun documentManagementApiClient(): DocumentManagementApiClient =
      mock(DocumentManagementApiClient::class.java)
  }
}