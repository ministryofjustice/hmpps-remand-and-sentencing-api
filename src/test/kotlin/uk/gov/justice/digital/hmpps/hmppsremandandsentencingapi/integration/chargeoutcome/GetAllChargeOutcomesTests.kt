package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.chargeoutcome

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase

class GetAllChargeOutcomesTests : IntegrationTestBase() {

  @Test
  fun `return all outcomes`() {
    webTestClient.get()
      .uri("/charge-outcome/all")
      .headers { it.authToken() }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.[?(@.outcomeUuid == 'f17328cf-ceaa-43c2-930a-26cf74480e18')]")
      .exists()
      .jsonPath("$.[?(@.outcomeUuid == '315280e5-d53e-43b3-8ba6-44da25676ce2')]")
      .exists()
  }

  @Test
  fun `no token results in unauthorized`() {
    webTestClient.get()
      .uri("/charge-outcome/all")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }
}