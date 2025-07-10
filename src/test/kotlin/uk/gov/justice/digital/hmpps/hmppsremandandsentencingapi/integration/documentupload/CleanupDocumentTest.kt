package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.documentupload

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.wiremock.DocumentManagementApiExtension
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.wiremock.DocumentManagementApiExtension.Companion.documentManagementApi
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.UploadedDocumentEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.UploadedDocumentRepository
import java.time.ZonedDateTime
import java.util.UUID
import com.github.tomakehurst.wiremock.client.WireMock.verify as wireMockVerify

@ExtendWith(DocumentManagementApiExtension::class)
class CleanupDocumentTest : IntegrationTestBase() {

  @Autowired
  lateinit var uploadedDocumentRepository: UploadedDocumentRepository

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
      fileName = "old_warrant.pdf",
    )
    uploadedDocumentRepository.save(oldDocument)

    documentManagementApi.stubDeleteDocument(documentUuid.toString())

    webTestClient
      .delete()
      .uri("/document-admin/cleanup")
      .headers { it.contentType = MediaType.APPLICATION_JSON }
      .exchange()
      .expectStatus().isOk

    val found = uploadedDocumentRepository.findByDocumentUuid(documentUuid)
    assertNull(found)

    WireMock.configureFor("localhost", 8442)
    wireMockVerify(deleteRequestedFor(urlEqualTo("/documents/$documentUuid")))
  }
}
