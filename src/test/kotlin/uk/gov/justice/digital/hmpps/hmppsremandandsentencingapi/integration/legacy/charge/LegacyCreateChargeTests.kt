package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.charge

import org.assertj.core.api.Assertions
import org.hamcrest.text.MatchesPattern
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.util.DataCreator

class LegacyCreateChargeTests : IntegrationTestBase() {

  @Test
  fun `create charge in existing court case`() {
    val (appearanceLifetimeUuid) = createLegacyCourtAppearance()
    val legacyCharge = DataCreator.legacyCreateCharge(appearanceLifetimeUuid = appearanceLifetimeUuid)
    val appearanceId = courtAppearanceRepository.findByAppearanceUuid(appearanceLifetimeUuid)!!.id
    val appearanceChargeHistoryBefore = appearanceChargeHistoryRepository.findAll().toList().filter { it.appearanceId == appearanceId }

    webTestClient
      .post()
      .uri("/legacy/charge")
      .bodyValue(legacyCharge)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_CHARGE_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isCreated
      .expectBody()
      .jsonPath("$.lifetimeUuid")
      .value(MatchesPattern.matchesPattern("([a-f0-9]{8}(-[a-f0-9]{4}){4}[a-f0-9]{8})"))
    val message = getMessages(1)[0]
    Assertions.assertThat(message.eventType).isEqualTo("charge.inserted")
    Assertions.assertThat(message.additionalInformation.get("source").asText()).isEqualTo("NOMIS")

    val appearanceChargeHistoryAfter = appearanceChargeHistoryRepository.findAll().toList().filter { it.appearanceId == appearanceId }
    val beforeIds = appearanceChargeHistoryBefore.map { it.id }.toSet()
    val newEntries = appearanceChargeHistoryAfter.filter { it.id !in beforeIds }
    assertEquals(1, newEntries.size)
    assertNull(newEntries[0].removedBy)
    assertEquals(legacyCharge.performedByUser, newEntries[0].createdBy)
  }

  @Test
  fun `must not charge when no court appearance exists`() {
    val legacyCharge = DataCreator.legacyCreateCharge()
    webTestClient
      .post()
      .uri("/legacy/charge")
      .bodyValue(legacyCharge)
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
    val legacyCharge = DataCreator.legacyCreateCharge()
    webTestClient
      .post()
      .uri("/legacy/charge")
      .bodyValue(legacyCharge)
      .headers {
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `token with incorrect role is forbidden`() {
    val legacyCharge = DataCreator.legacyCreateCharge()
    webTestClient
      .post()
      .uri("/legacy/charge")
      .bodyValue(legacyCharge)
      .headers {
        it.authToken(roles = listOf("ROLE_OTHER_FUNCTION"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isForbidden
  }
}
