package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.courtcase

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import java.time.format.DateTimeFormatter
import java.util.UUID

class GetCourtCaseTests : IntegrationTestBase() {

  @Test
  fun `get court case by uuid`() {
    val createdCase = createCourtCase()
    webTestClient
      .get()
      .uri("/court-case/${createdCase.first}")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING"))
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.courtCaseUuid")
      .isEqualTo(createdCase.first)
      .jsonPath("$.prisonerId")
      .isEqualTo(createdCase.second.prisonerId)
      .jsonPath("$.latestAppearance.appearanceDate")
      .isEqualTo(createdCase.second.appearances.first().appearanceDate.format(DateTimeFormatter.ISO_DATE))
      .jsonPath("$.latestAppearance.outcome")
      .isEqualTo(createdCase.second.appearances.first().outcome)
  }

  @Test
  fun `no court case exist for uuid results in not found`() {
    webTestClient
      .get()
      .uri("/court-case/${UUID.randomUUID()}")
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
      .uri("/court-case/${createdCase.first}")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `token with incorrect role is forbidden`() {
    val createdCase = createCourtCase()
    webTestClient
      .get()
      .uri("/court-case/${createdCase.first}")
      .headers {
        it.authToken(roles = listOf("ROLE_OTHER_FUNCTION"))
      }
      .exchange()
      .expectStatus()
      .isForbidden
  }
}
