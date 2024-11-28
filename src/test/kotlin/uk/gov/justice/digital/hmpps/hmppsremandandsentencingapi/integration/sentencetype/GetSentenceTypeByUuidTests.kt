package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.sentencetype

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import java.util.UUID

class GetSentenceTypeByUuidTests : IntegrationTestBase() {

  @Test
  fun `get sentence type by uuid`() {
    webTestClient
      .get()
      .uri("/sentence-type/1104e683-5467-4340-b961-ff53672c4f39")
      .headers {
        it.authToken()
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.sentenceTypeUuid")
      .isEqualTo("1104e683-5467-4340-b961-ff53672c4f39")
      .jsonPath("$.description")
      .isEqualTo("SDS (Standard determinate sentence)")
      .jsonPath("$.classification")
      .isEqualTo("STANDARD")
  }

  @Test
  fun `no appearance exist for uuid results in not found`() {
    webTestClient
      .get()
      .uri("/sentence-type/${UUID.randomUUID()}")
      .headers {
        it.authToken()
      }
      .exchange()
      .expectStatus()
      .isNotFound
  }

  @Test
  fun `no token results in unauthorized`() {
    webTestClient
      .get()
      .uri("/sentence-type/1104e683-5467-4340-b961-ff53672c4f39")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }
}
