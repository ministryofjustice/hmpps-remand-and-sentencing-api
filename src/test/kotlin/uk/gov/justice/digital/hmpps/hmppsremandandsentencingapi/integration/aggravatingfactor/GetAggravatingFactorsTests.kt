package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.aggravatingfactor

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.AggravatingFactor
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase

class GetAggravatingFactorsTests : IntegrationTestBase() {

  @Test
  fun `get aggravating factors returns all factors ordered by display order`() {
    val response = webTestClient
      .get()
      .uri("/aggravating-factors/status?statuses=ACTIVE")
      .headers {
        it.authToken()
      }
      .exchange()
      .expectStatus()
      .isOk
      .returnResult(AggravatingFactor::class.java)
      .responseBody
      .collectList()
      .block()!!

    assertThat(response).isNotEmpty
    assertThat(response.map { it.displayOrder }).isSorted
    assertThat(response).allSatisfy {
      assertThat(it.code).isNotBlank()
      assertThat(it.title).isNotBlank()
    }
  }

  @Test
  fun `get aggravating factors includes expected known factors`() {
    webTestClient
      .get()
      .uri("/aggravating-factors/status?statuses=ACTIVE")
      .headers {
        it.authToken()
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$[?(@.code == 'OATC')].title")
      .exists()
      .jsonPath("$[?(@.code == 'OAFPC')].title")
      .exists()
  }

  @Test
  fun `no token results in unauthorized`() {
    webTestClient
      .get()
      .uri("/aggravating-factors/status?statuses=ACTIVE")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }
}
