package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.charge

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import java.util.UUID

class LegacyDeleteChargeTest : IntegrationTestBase() {

  @Test
  fun `can delete charge`() {
    val (lifetimeUuid) = createLegacyCharge()
    webTestClient
      .delete()
      .uri("/legacy/charge/$lifetimeUuid")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_CHARGE_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isOk
    val message = getMessages(1)[0]
    Assertions.assertThat(message.eventType).isEqualTo("charge.deleted")
    Assertions.assertThat(message.additionalInformation.get("source").asText()).isEqualTo("NOMIS")
  }

  @Test
  fun `no token results in unauthorized`() {
    webTestClient
      .delete()
      .uri("/legacy/charge/${UUID.randomUUID()}")
      .headers {
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `token with incorrect role is forbidden`() {
    webTestClient
      .delete()
      .uri("/legacy/charge/${UUID.randomUUID()}")
      .headers {
        it.authToken(roles = listOf("ROLE_OTHER_FUNCTION"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isForbidden
  }
}
