package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.courtappearance

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import java.time.format.DateTimeFormatter
import java.util.UUID

class LegacyGetCourtAppearanceTests : IntegrationTestBase() {

  @Test
  fun `get appearance by lifetime uuid`() {
    val (lifetimeUuid, createdAppearance) = createLegacyCourtAppearance()

    webTestClient
      .get()
      .uri("/legacy/court-appearance/$lifetimeUuid")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_APPEARANCE_RO"))
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.lifetimeUuid")
      .isEqualTo(lifetimeUuid.toString())
      .jsonPath("$.appearanceDate")
      .isEqualTo(createdAppearance.appearanceDate.format(DateTimeFormatter.ISO_DATE))
      .jsonPath("$.nomisOutcomeCode")
      .isEqualTo(createdAppearance.legacyData.nomisOutcomeCode!!)
  }

  @Test
  fun `no appearance exist for uuid results in not found`() {
    webTestClient
      .get()
      .uri("/legacy/court-appearance/${UUID.randomUUID()}")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_APPEARANCE_RO"))
      }
      .exchange()
      .expectStatus()
      .isNotFound
  }

  @Test
  fun `no token results in unauthorized`() {
    val createdAppearance = createCourtCase().second.appearances.first()
    webTestClient
      .get()
      .uri("/legacy/court-appearance/${createdAppearance.lifetimeUuid}")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `token with incorrect role is forbidden`() {
    val createdAppearance = createCourtCase().second.appearances.first()
    webTestClient
      .get()
      .uri("/legacy/court-appearance/${createdAppearance.lifetimeUuid}")
      .headers {
        it.authToken(roles = listOf("ROLE_OTHER_FUNCTION"))
      }
      .exchange()
      .expectStatus()
      .isForbidden
  }
}