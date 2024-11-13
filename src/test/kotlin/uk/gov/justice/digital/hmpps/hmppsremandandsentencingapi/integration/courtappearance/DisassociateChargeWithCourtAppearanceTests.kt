package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.courtappearance

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import java.util.UUID

class DisassociateChargeWithCourtAppearanceTests : IntegrationTestBase() {

  @Test
  fun `can disassociate charge with court appearance`() {
    val courtCase = createCourtCase()
    val createdAppearance = courtCase.second.appearances.first()
    webTestClient
      .delete()
      .uri("/court-appearance/${createdAppearance.appearanceUuid!!}/charge/${createdAppearance.charges.first().chargeUuid}")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_APPEARANCE_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isNoContent
    val messages = getMessages(1)
    Assertions.assertThat(messages).hasSize(1).extracting<String> { it.eventType }.contains("charge.deleted")
  }

  @Test
  fun `no token results in unauthorized`() {
    webTestClient
      .delete()
      .uri("/court-appearance/${UUID.randomUUID()}/charge/${UUID.randomUUID()}")
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
      .uri("/court-appearance/${UUID.randomUUID()}/charge/${UUID.randomUUID()}")
      .headers {
        it.authToken(roles = listOf("ROLE_OTHER_FUNCTION"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isForbidden
  }
}