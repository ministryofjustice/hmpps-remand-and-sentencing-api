package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.sentencetype

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.core.ParameterizedTypeReference
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.SentenceType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase

class GetSentenceTypesByUuidsTests : IntegrationTestBase() {

  private inline fun <reified T> typeReference() = object : ParameterizedTypeReference<T>() {}

  @Test
  fun `providing no parameters results in bad request`() {
    webTestClient.get()
      .uri("/sentence-type/uuid/multiple")
      .headers { it.authToken() }
      .exchange()
      .expectStatus()
      .isBadRequest
  }

  @Test
  fun `retrieve multiple sentence types by uuids`() {
    val result = webTestClient.get()
      .uri("/sentence-type/uuid/multiple?uuids=1104e683-5467-4340-b961-ff53672c4f39,0da58738-2db6-4fba-8f65-438284019756")
      .headers { it.authToken() }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody(typeReference<List<SentenceType>>())
      .returnResult().responseBody!!

    val descriptions = result.map { it.description }
    Assertions.assertThat(descriptions).containsExactlyInAnyOrderElementsOf(listOf("EDS (Extended Determinate Sentence)", "SDS (Standard Determinate Sentence)"))
  }
}
