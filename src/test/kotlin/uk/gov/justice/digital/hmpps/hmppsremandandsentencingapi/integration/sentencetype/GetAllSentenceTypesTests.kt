package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.sentencetype

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase

class GetAllSentenceTypesTests : IntegrationTestBase() {

  @Test
  fun `get all sentence types`() {
    webTestClient.get()
      .uri("/sentence-type/all")
      .headers { it.authToken() }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.sentenceTypes[?(@.sentenceTypeUuid == '8d04557c-8e54-4e2a-844f-272163fca833')].description")
      .isEqualTo("SDS (Standard Determinate Sentence)")
  }
}
