package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.charge

import org.assertj.core.api.Assertions
import org.hamcrest.text.MatchesPattern
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.DpsDataCreator

class UpdateChargeTests : IntegrationTestBase() {

  @Test
  fun `can update charge supplying appearance uuid`() {
    val courtCase = createCourtCase()
    val createdAppearance = courtCase.second.appearances.first()
    val createdCharge = createdAppearance.charges.first()
    val toUpdateCharge = DpsDataCreator.dpsCreateCharge(appearanceUuid = createdAppearance.appearanceUuid, chargeUuid = createdCharge.chargeUuid, offenceCode = "OFF567")

    webTestClient
      .put()
      .uri("/charge/${createdCharge.chargeUuid}")
      .bodyValue(toUpdateCharge)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_CHARGE_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.chargeUuid")
      .value(MatchesPattern.matchesPattern("([a-f0-9]{8}(-[a-f0-9]{4}){4}[a-f0-9]{8})"))
    val messages = getMessages(2)
    Assertions.assertThat(messages).hasSize(2).extracting<String> { it.eventType }.contains("charge.updated", "sentence.inserted")
  }

  @Test
  fun `cannot create a charge without court appearance and court case`() {
    val toUpdateCharge = DpsDataCreator.dpsCreateCharge()
    webTestClient
      .put()
      .uri("/charge/${toUpdateCharge.chargeUuid}")
      .bodyValue(toUpdateCharge)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_CHARGE_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isBadRequest
  }

  @Test
  fun `no token results in unauthorized`() {
    val toUpdateCharge = DpsDataCreator.dpsCreateCharge()
    webTestClient
      .put()
      .uri("/charge/${toUpdateCharge.chargeUuid}")
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
    val toUpdateCharge = DpsDataCreator.dpsCreateCharge()
    webTestClient
      .put()
      .uri("/charge/${toUpdateCharge.chargeUuid}")
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
