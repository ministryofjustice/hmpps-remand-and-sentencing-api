package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.courtcase

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import java.time.format.DateTimeFormatter
import java.util.UUID

class GetLatestCourtAppearanceTests : IntegrationTestBase() {

  @Test
  fun `get latest court appearance by court case uuid`() {
    val createdCase = createCourtCase()
    webTestClient
      .get()
      .uri("/court-case/${createdCase.first}/latest-appearance")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING"))
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.appearanceDate")
      .isEqualTo(createdCase.second.appearances.first().appearanceDate.format(DateTimeFormatter.ISO_DATE))
      .jsonPath("$.outcome.outcomeUuid")
      .isEqualTo(createdCase.second.appearances.first().outcomeUuid.toString())
  }

  @Test
  fun `no court case exist for uuid results in not found`() {
    webTestClient
      .get()
      .uri("/court-case/${UUID.randomUUID()}/latest-appearance")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING"))
      }
      .exchange()
      .expectStatus()
      .isNotFound
  }

  @Test
  fun `no token results in unauthorized`() {
    val createdCase = createCourtCase()
    webTestClient
      .get()
      .uri("/court-case/${createdCase.first}/latest-appearance")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `token with incorrect role is forbidden`() {
    val createdCase = createCourtCase()
    webTestClient
      .get()
      .uri("/court-case/${createdCase.first}/latest-appearance")
      .headers {
        it.authToken(roles = listOf("ROLE_OTHER_FUNCTION"))
      }
      .exchange()
      .expectStatus()
      .isForbidden
  }
}
