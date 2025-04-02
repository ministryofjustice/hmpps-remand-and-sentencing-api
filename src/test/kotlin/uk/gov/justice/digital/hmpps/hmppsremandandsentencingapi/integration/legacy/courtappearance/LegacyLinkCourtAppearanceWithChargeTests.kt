package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.courtappearance

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.util.DataCreator
import java.util.UUID

class LegacyLinkCourtAppearanceWithChargeTests : IntegrationTestBase() {

  @Test
  fun `link existing appearance with existing charge`() {
    val courtAppearance = createLegacyCourtAppearance()
    val charge = createLegacyCharge()
    val toUpdateCharge = DataCreator.legacyUpdateCharge()
    webTestClient
      .put()
      .uri("/legacy/court-appearance/${courtAppearance.first}/charge/${charge.first}")
      .bodyValue(toUpdateCharge)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_APPEARANCE_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isNoContent
    val messages = getMessages(2)
    Assertions.assertThat(messages).hasSize(2).extracting<String> { it.eventType }.containsExactlyInAnyOrder("charge.updated", "court-appearance.updated")
    Assertions.assertThat(messages).hasSize(2).extracting<String> { it.additionalInformation.get("source").asText() }.containsOnly("NOMIS")
  }

  @Test
  fun `not found when no charge exists`() {
    val courtAppearance = createLegacyCourtAppearance()
    val toUpdateCharge = DataCreator.legacyUpdateCharge()
    webTestClient
      .put()
      .uri("/legacy/court-appearance/${courtAppearance.first}/charge/${UUID.randomUUID()}")
      .bodyValue(toUpdateCharge)
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
    val toUpdateCharge = DataCreator.legacyUpdateCharge()
    webTestClient
      .put()
      .uri("/legacy/court-appearance/${UUID.randomUUID()}/charge/${UUID.randomUUID()}")
      .bodyValue(toUpdateCharge)
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
    val toUpdateCharge = DataCreator.legacyUpdateCharge()
    webTestClient
      .put()
      .uri("/legacy/court-appearance/${courtAppearance.first}/charge/${charge.first}")
      .bodyValue(toUpdateCharge)
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
    val toUpdateCharge = DataCreator.legacyUpdateCharge()
    webTestClient
      .put()
      .uri("/legacy/court-appearance/${courtAppearance.first}/charge/${charge.first}")
      .bodyValue(toUpdateCharge)
      .headers {
        it.authToken(roles = listOf("ROLE_OTHER_FUNCTION"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isForbidden
  }
}
