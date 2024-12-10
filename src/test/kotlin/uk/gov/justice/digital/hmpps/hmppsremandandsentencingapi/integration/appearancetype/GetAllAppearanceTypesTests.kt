package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.appearancetype

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase

class GetAllAppearanceTypesTests : IntegrationTestBase() {

  @Test
  fun `return all types`() {
    webTestClient.get()
      .uri("/appearance-type/all")
      .headers { it.authToken() }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.[?(@.appearanceTypeUuid == '63e8fce0-033c-46ad-9edf-391b802d547a')]")
      .exists()
      .jsonPath("$.[?(@.appearanceTypeUuid == '1da09b6e-55cb-4838-a157-ee6944f2094c')]")
      .exists()
  }

  @Test
  fun `no token results in unauthorized`() {
    webTestClient.get()
      .uri("/appearance-type/all")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }
}
