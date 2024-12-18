package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.charge

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.util.DataCreator
import java.util.UUID

class LegacyUpdateChargeInAppearanceTests : IntegrationTestBase() {

  @Test
  fun `update charge in existing court appearance`() {
    val createdCharge = createLegacyCharge()
    val toUpdate = DataCreator.legacyUpdateCharge()
    webTestClient
      .put()
      .uri("/legacy/charge/${createdCharge.first}/appearance/${createdCharge.second.appearanceLifetimeUuid}")
      .bodyValue(toUpdate)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_CHARGE_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isOk
    val message = getMessages(1)[0]
    Assertions.assertThat(message.eventType).isEqualTo("charge.updated")
    Assertions.assertThat(message.additionalInformation.get("source").asText()).isEqualTo("NOMIS")
  }

  @Test
  fun `must not update appearance when no court appearance exists`() {
    val toUpdate = DataCreator.legacyUpdateCharge()
    webTestClient
      .put()
      .uri("/legacy/charge/${UUID.randomUUID()}/appearance/${UUID.randomUUID()}")
      .bodyValue(toUpdate)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_CHARGE_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isNotFound
  }

  @Test
  fun `no token results in unauthorized`() {
    val toUpdate = DataCreator.legacyUpdateCharge()
    webTestClient
      .put()
      .uri("/legacy/charge/${UUID.randomUUID()}/appearance/${UUID.randomUUID()}")
      .bodyValue(toUpdate)
      .headers {
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `token with incorrect role is forbidden`() {
    val toUpdate = DataCreator.legacyUpdateCharge()
    webTestClient
      .put()
      .uri("/legacy/charge/${UUID.randomUUID()}/appearance/${UUID.randomUUID()}")
      .bodyValue(toUpdate)
      .headers {
        it.authToken(roles = listOf("ROLE_OTHER_FUNCTION"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isForbidden
  }
}
