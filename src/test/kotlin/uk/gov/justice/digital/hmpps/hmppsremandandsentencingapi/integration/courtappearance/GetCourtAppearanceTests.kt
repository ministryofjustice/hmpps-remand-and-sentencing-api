package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.courtappearance

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import java.time.format.DateTimeFormatter
import java.util.UUID

class GetCourtAppearanceTests : IntegrationTestBase() {

  @Test
  fun `get appearance by uuid`() {
    val createdAppearance = createCourtCase().second.appearances.first()
    webTestClient
      .get()
      .uri("/court-appearance/${createdAppearance.appearanceUuid!!}")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING"))
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.appearanceUuid")
      .isEqualTo(createdAppearance.appearanceUuid!!.toString())
      .jsonPath("$.courtCaseReference")
      .isEqualTo(createdAppearance.courtCaseReference)
      .jsonPath("$.appearanceDate")
      .isEqualTo(createdAppearance.appearanceDate.format(DateTimeFormatter.ISO_DATE))
      .jsonPath("$.outcome")
      .isEqualTo(createdAppearance.outcome)
      .jsonPath("$.nextCourtAppearance.appearanceDate")
      .isEqualTo(createdAppearance.nextCourtAppearance!!.appearanceDate.format(DateTimeFormatter.ISO_DATE))
      .jsonPath("$.nextCourtAppearance.courtCode")
      .isEqualTo(createdAppearance.nextCourtAppearance!!.courtCode)
      .jsonPath("$.warrantType")
      .isEqualTo(createdAppearance.warrantType)
  }

  @Test
  fun `no appearance exist for uuid results in not found`() {
    webTestClient
      .get()
      .uri("/court-appearance/${UUID.randomUUID()}")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING"))
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
      .uri("/court-appearance/${createdAppearance.appearanceUuid!!}")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `token with incorrect role is forbidden`() {
    val createdAppearance = createCourtCase().second.appearances.first()
    webTestClient
      .get()
      .uri("/court-appearance/${createdAppearance.appearanceUuid!!}")
      .headers {
        it.authToken(roles = listOf("ROLE_OTHER_FUNCTION"))
      }
      .exchange()
      .expectStatus()
      .isForbidden
  }
}
