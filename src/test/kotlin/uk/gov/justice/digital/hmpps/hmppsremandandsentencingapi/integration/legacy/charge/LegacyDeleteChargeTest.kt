package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.charge

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.DpsDataCreator
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
      .isNoContent
    val message = getMessages(1)[0]
    Assertions.assertThat(message.eventType).isEqualTo("charge.deleted")
    Assertions.assertThat(message.additionalInformation.get("source").asText()).isEqualTo("NOMIS")
  }

  @Test
  fun `delete charge in all appearances`() {
    val charge = DpsDataCreator.dpsCreateCharge()
    val firstAppearance = DpsDataCreator.dpsCreateCourtAppearance(charges = listOf(charge))
    val chargeInSecondAppearance = charge.copy(offenceStartDate = charge.offenceStartDate.plusDays(10))
    val secondAppearance = DpsDataCreator.dpsCreateCourtAppearance(charges = listOf(chargeInSecondAppearance))
    val courtCase = DpsDataCreator.dpsCreateCourtCase(appearances = listOf(firstAppearance, secondAppearance))
    val (courtCaseUuid) = createCourtCase(courtCase)
    webTestClient
      .delete()
      .uri("/legacy/charge/${charge.chargeUuid}")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_CHARGE_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isNoContent

    webTestClient
      .get()
      .uri("/court-case/$courtCaseUuid")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING"))
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.appearances[?(@.appearanceUuid == '${firstAppearance.appearanceUuid}')].charges[?(@.chargeUuid == '${charge.chargeUuid}')]")
      .doesNotExist()
      .jsonPath("$.appearances[?(@.appearanceUuid == '${secondAppearance.appearanceUuid}')].charges[?(@.chargeUuid == '${charge.chargeUuid}')]")
      .doesNotExist()
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
