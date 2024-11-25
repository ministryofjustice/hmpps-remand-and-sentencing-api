package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.courtappearance

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import java.util.UUID

class LegacyLinkCourtAppearanceWithChargeTests : IntegrationTestBase() {

  @Test
  fun `link existing appearance with existing charge`() {
    val courtAppearance = createLegacyCourtAppearance()
    val charge = createLegacyCharge()
    webTestClient
      .put()
      .uri("/legacy/court-appearance/${courtAppearance.first}/charge/${charge.first}")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_APPEARANCE_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isOk
    val message = getMessages(1)[0]
    Assertions.assertThat(message.eventType).isEqualTo("court-appearance.updated")
    Assertions.assertThat(message.additionalInformation.get("source").asText()).isEqualTo("NOMIS")
  }

  @Test
  fun `not found when no charge exists`() {
    val courtAppearance = createLegacyCourtAppearance()
    webTestClient
      .put()
      .uri("/legacy/court-appearance/${courtAppearance.first}/charge/${UUID.randomUUID()}")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_APPEARANCE_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isNotFound
  }

  @Test
  fun `not found when no court appearance exists`() {
    webTestClient
      .put()
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
    val courtAppearance = createLegacyCourtAppearance()
    val charge = createLegacyCharge()
    webTestClient
      .put()
      .uri("/legacy/court-appearance/${courtAppearance.first}/charge/${charge.first}")
      .headers {
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `token with incorrect role is forbidden`() {
    val courtAppearance = createLegacyCourtAppearance()
    val charge = createLegacyCharge()
    webTestClient
      .put()
      .uri("/legacy/court-appearance/${courtAppearance.first}/charge/${charge.first}")
      .headers {
        it.authToken(roles = listOf("ROLE_OTHER_FUNCTION"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isForbidden
  }
}
