package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.charge

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import java.util.UUID

class LegacyGetChargeTests : IntegrationTestBase() {

  @Test
  fun `get charge by lifetime uuid`() {
    val (lifetimeUuid, createdCharge) = createLegacyCharge()

    webTestClient
      .get()
      .uri("/legacy/charge/$lifetimeUuid")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_CHARGE_RO"))
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.lifetimeUuid")
      .isEqualTo(lifetimeUuid.toString())
      .jsonPath("$.offenceCode")
      .isEqualTo(createdCharge.offenceCode)
  }

  @Test
  fun `no charge exist for uuid results in not found`() {
    webTestClient
      .get()
      .uri("/legacy/charge/${UUID.randomUUID()}")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_CHARGE_RO"))
      }
      .exchange()
      .expectStatus()
      .isNotFound
  }

  @Test
  fun `no token results in unauthorized`() {
    val (lifetimeUuid) = createLegacyCharge()
    webTestClient
      .get()
      .uri("/legacy/charge/$lifetimeUuid")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `token with incorrect role is forbidden`() {
    val (lifetimeUuid) = createLegacyCharge()
    webTestClient
      .get()
      .uri("/legacy/charge/$lifetimeUuid")
      .headers {
        it.authToken(roles = listOf("ROLE_OTHER_FUNCTION"))
      }
      .exchange()
      .expectStatus()
      .isForbidden
  }
}
