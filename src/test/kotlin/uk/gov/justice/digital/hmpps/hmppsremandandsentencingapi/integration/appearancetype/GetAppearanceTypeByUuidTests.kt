package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.appearancetype

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import java.util.UUID

class GetAppearanceTypeByUuidTests : IntegrationTestBase() {

  @Test
  fun `get appearance type by uuid`() {
    webTestClient
      .get()
      .uri("/appearance-type/63e8fce0-033c-46ad-9edf-391b802d547a")
      .headers {
        it.authToken()
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.appearanceTypeUuid")
      .isEqualTo("63e8fce0-033c-46ad-9edf-391b802d547a")
      .jsonPath("$.description")
      .isEqualTo("Court appearance")
      .jsonPath("$.displayOrder")
      .isEqualTo(10)
  }

  @Test
  fun `no appearance type exist for uuid results in not found`() {
    webTestClient
      .get()
      .uri("/appearance-type/${UUID.randomUUID()}")
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
      .uri("/appearance-type/63e8fce0-033c-46ad-9edf-391b802d547a")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }
}
