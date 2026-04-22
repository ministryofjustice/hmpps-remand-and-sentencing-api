package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.sentencetype

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase

class GetAllSentenceTypeChargeOutcomesTests : IntegrationTestBase() {

  @Test
  fun `get all sentence type charge outcomes`() {
    webTestClient.get()
      .uri("/sentence-type/charge-outcome/all")
      .headers { it.authToken() }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.chargeOutcomes[?(@.outcomeUuid == '3a1d8f72-7b2e-4f5c-8d4a-6c9e1b7f2d03')]")
      .exists()
  }
}
