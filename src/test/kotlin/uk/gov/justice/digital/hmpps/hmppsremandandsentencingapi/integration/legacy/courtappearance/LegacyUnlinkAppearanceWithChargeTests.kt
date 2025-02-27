package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.courtappearance

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import java.util.UUID

class LegacyUnlinkAppearanceWithChargeTests : IntegrationTestBase() {

  @Test
  fun `unlink existing appearance with charge`() {
    val (lifetimeChargeUuid, charge) = createLegacyCharge()
    webTestClient
      .delete()
      .uri("/legacy/court-appearance/${charge.appearanceLifetimeUuid}/charge/$lifetimeChargeUuid")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_APPEARANCE_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isOk
    val messages = getMessages(2)
    Assertions.assertThat(messages).hasSize(2).extracting<String> { message -> message.eventType }.containsAll(listOf("court-appearance.updated", "charge.deleted"))
    Assertions.assertThat(messages).hasSize(2).extracting<String> { message -> message.additionalInformation.get("source").asText() }.isEqualTo(listOf("NOMIS", "NOMIS"))
  }

  @Test
  fun `not found when no court appearance exists`() {
    webTestClient
      .delete()
      .uri("/legacy/court-appearance/${UUID.randomUUID()}/charge/${UUID.randomUUID()}")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_APPEARANCE_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isNotFound
  }

  @Test
  fun `no token results in unauthorized`() {
    val (lifetimeChargeUuid, charge) = createLegacyCharge()
    webTestClient
      .delete()
      .uri("/legacy/court-appearance/${charge.appearanceLifetimeUuid}/charge/$lifetimeChargeUuid")
      .headers {
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `token with incorrect role is forbidden`() {
    val (lifetimeChargeUuid, charge) = createLegacyCharge()
    webTestClient
      .delete()
      .uri("/legacy/court-appearance/${charge.appearanceLifetimeUuid}/charge/$lifetimeChargeUuid")
      .headers {
        it.authToken(roles = listOf("ROLE_OTHER_FUNCTION"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isForbidden
  }
}
