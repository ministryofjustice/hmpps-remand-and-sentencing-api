package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.courtAppearanceSubtype

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import java.util.UUID

class GetCourtAppearanceSubtypeByUuidTests : IntegrationTestBase() {

  @Test
  fun `get court appearance subtype by uuid`() {
    webTestClient
      .get()
      .uri("/court-appearance-subtype/3f1c9e42-7c8a-4c1e-9a5d-2f6b8d1a9e73")
      .headers {
        it.authToken()
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.appearanceSubtypeUuid")
      .isEqualTo("3f1c9e42-7c8a-4c1e-9a5d-2f6b8d1a9e73")
      .jsonPath("$.description")
      .isEqualTo("Discharged to court")
      .jsonPath("$.displayOrder")
      .isEqualTo(10)
      .jsonPath("$.appearanceTypeUuid")
      .isEqualTo("63e8fce0-033c-46ad-9edf-391b802d547a")
  }

  @Test
  fun `no court appearance subtype exist for uuid results in not found`() {
    webTestClient
      .get()
      .uri("/court-appearance-subtype/${UUID.randomUUID()}")
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
      .uri("/court-appearance-subtype/63e8fce0-033c-46ad-9edf-391b802d547a")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }
}
