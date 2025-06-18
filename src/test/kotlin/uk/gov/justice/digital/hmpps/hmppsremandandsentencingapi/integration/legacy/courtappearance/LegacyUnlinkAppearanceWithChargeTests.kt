package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.courtappearance

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateCharge
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.util.DataCreator
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.DpsDataCreator
import java.time.LocalDate
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
  fun `unlink older charge records from appearance`() {
    val charge = DpsDataCreator.dpsCreateCharge()
    val firstAppearance = DpsDataCreator.dpsCreateCourtAppearance(appearanceDate = LocalDate.now().minusMonths(6), charges = listOf(charge))
    val secondAppearance = DpsDataCreator.dpsCreateCourtAppearance(appearanceDate = LocalDate.now().minusMonths(3), charges = listOf(charge))
    val courtCase = DpsDataCreator.dpsCreateCourtCase(appearances = listOf(firstAppearance, secondAppearance))
    val (courtCaseUuid) = createCourtCase(courtCase)

    updateChargeInAppearance(charge, firstAppearance.appearanceUuid)
    updateChargeInAppearance(charge, secondAppearance.appearanceUuid)

    webTestClient
      .delete()
      .uri("/legacy/court-appearance/${firstAppearance.appearanceUuid}/charge/${charge.chargeUuid}")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_APPEARANCE_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isOk

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
      .exists()
  }

  private fun updateChargeInAppearance(charge: CreateCharge, appearanceUuid: UUID) {
    val toUpdate = DataCreator.legacyUpdateCharge(offenceStartDate = charge.offenceStartDate.minusDays(10))
    webTestClient
      .put()
      .uri("/legacy/charge/${charge.chargeUuid}/appearance/$appearanceUuid")
      .bodyValue(toUpdate)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_CHARGE_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isNoContent
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
