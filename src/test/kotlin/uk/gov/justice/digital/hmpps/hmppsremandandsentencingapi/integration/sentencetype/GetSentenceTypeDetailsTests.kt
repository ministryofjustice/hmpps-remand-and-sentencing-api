package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.sentencetype

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase

class GetSentenceTypeDetailsTests : IntegrationTestBase() {

  @Test
  fun `get sentence type details`() {
    webTestClient.get()
      .uri("/sentence-type/8d04557c-8e54-4e2a-844f-272163fca833/details")
      .headers { it.authToken() }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.description")
      .isEqualTo("SDS (Standard Determinate Sentence)")
  }
}
