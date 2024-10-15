package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.appearanceoutcome

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import java.util.UUID

class GetAppearanceOutcomeByUuidTests : IntegrationTestBase() {

  @Test
  fun `get appearance outcome by uuid`() {
    webTestClient
      .get()
      .uri("/appearance-outcome/2f585681-7b1a-44fb-a0cb-f9a4b1d9cda8")
      .headers {
        it.authToken()
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.outcomeUuid")
      .isEqualTo("2f585681-7b1a-44fb-a0cb-f9a4b1d9cda8")
      .jsonPath("$.outcomeName")
      .isEqualTo("Remand in custody")
      .jsonPath("$.nomisCode")
      .isEqualTo("4531")
      .jsonPath("$.outcomeType")
      .isEqualTo("REMAND")
      .jsonPath("$.displayOrder")
      .isEqualTo(20)
      .jsonPath("$.isSubList")
      .isEqualTo(false)
  }

  @Test
  fun `no appearance outcome exist for uuid results in not found`() {
    webTestClient
      .get()
      .uri("/appearance-outcome/${UUID.randomUUID()}")
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
      .uri("/appearance-outcome/2f585681-7b1a-44fb-a0cb-f9a4b1d9cda8")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }
}
