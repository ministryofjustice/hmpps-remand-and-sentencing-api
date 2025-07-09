package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.documentupload

import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase

class CleanupDocumentTest : IntegrationTestBase() {

  @Test
  fun `cleanup endpoint is authorized and returns expected status`() {
    webTestClient
      .post()
      .uri("/document-admin/cleanup")
      .headers {
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isOk
  }
}
