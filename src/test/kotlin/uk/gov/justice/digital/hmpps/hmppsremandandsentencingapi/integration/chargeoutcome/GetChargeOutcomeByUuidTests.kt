package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.chargeoutcome

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import java.util.UUID

class GetChargeOutcomeByUuidTests : IntegrationTestBase() {

  @Test
  fun `get charge outcome by uuid`() {
    webTestClient
      .get()
      .uri("/charge-outcome/315280e5-d53e-43b3-8ba6-44da25676ce2")
      .headers {
        it.authToken()
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.outcomeUuid")
      .isEqualTo("315280e5-d53e-43b3-8ba6-44da25676ce2")
      .jsonPath("$.outcomeName")
      .isEqualTo("Remand in custody")
      .jsonPath("$.nomisCode")
      .isEqualTo("4531")
      .jsonPath("$.outcomeType")
      .isEqualTo("REMAND")
      .jsonPath("$.displayOrder")
      .isEqualTo(70)
  }

  @Test
  fun `no charge outcome exist for uuid results in not found`() {
    webTestClient
      .get()
      .uri("/charge-outcome/${UUID.randomUUID()}")
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
      .uri("/charge-outcome/315280e5-d53e-43b3-8ba6-44da25676ce2")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }
}
