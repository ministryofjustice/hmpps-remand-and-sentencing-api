package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.appearanceoutcome

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase

class GetAllAppearanceOutcomesTests : IntegrationTestBase() {

  @Test
  fun `return all outcomes`() {
    webTestClient.get()
      .uri("/appearance-outcome/all")
      .headers { it.authToken() }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.[?(@.outcomeUuid == '62412083-9892-48c9-bf01-7864af4a8b3c')]")
      .exists()
      .jsonPath("$.[?(@.outcomeUuid == '2f585681-7b1a-44fb-a0cb-f9a4b1d9cda8')]")
      .exists()
  }

  @Test
  fun `no token results in unauthorized`() {
    webTestClient.get()
      .uri("/appearance-outcome/all")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }
}
