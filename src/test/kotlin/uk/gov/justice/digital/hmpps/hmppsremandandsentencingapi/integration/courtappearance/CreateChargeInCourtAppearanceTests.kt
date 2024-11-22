package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.courtappearance

import org.hamcrest.text.MatchesPattern
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.DpsDataCreator
import java.util.UUID

class CreateChargeInCourtAppearanceTests : IntegrationTestBase() {

  @Test
  fun `create charge in existing court appearance`() {
    val courtCase = createCourtCase()
    val createCharge = DpsDataCreator.dpsCreateCharge()
    webTestClient
      .post()
      .uri("/court-appearance/${courtCase.second.appearances.first().appearanceUuid!!}/charge")
      .bodyValue(createCharge)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_CHARGE_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isCreated
      .expectBody()
      .jsonPath("$.chargeUuid")
      .value(MatchesPattern.matchesPattern("([a-f0-9]{8}(-[a-f0-9]{4}){4}[a-f0-9]{8})"))
  }

  @Test
  fun `must not create charge when no court appearance exists`() {
    val createCharge = DpsDataCreator.dpsCreateCharge()
    webTestClient
      .post()
      .uri("/court-appearance/${UUID.randomUUID()}/charge")
      .bodyValue(createCharge)
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
    val courtCase = createCourtCase()
    val createCharge = DpsDataCreator.dpsCreateCharge()
    webTestClient
      .post()
      .uri("/court-appearance/${courtCase.second.appearances.first().appearanceUuid!!}/charge")
      .bodyValue(createCharge)
      .headers {
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `token with incorrect role is forbidden`() {
    val courtCase = createCourtCase()
    val createCharge = DpsDataCreator.dpsCreateCharge()
    webTestClient
      .post()
      .uri("/court-appearance/${courtCase.second.appearances.first().appearanceUuid!!}/charge")
      .bodyValue(createCharge)
      .headers {
        it.authToken(roles = listOf("ROLE_OTHER_FUNCTION"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isForbidden
  }
}
