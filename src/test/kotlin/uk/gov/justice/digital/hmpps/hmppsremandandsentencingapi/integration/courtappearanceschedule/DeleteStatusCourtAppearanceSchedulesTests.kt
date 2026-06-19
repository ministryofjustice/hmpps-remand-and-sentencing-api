package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.courtappearanceschedule

import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.DpsDataCreator
import java.util.UUID

class DeleteStatusCourtAppearanceSchedulesTests : IntegrationTestBase() {

  @Test
  fun `can delete a court appearance without a sentence`() {
    val charge = DpsDataCreator.dpsCreateCharge(sentence = null)
    val courtAppearance = DpsDataCreator.dpsCreateCourtAppearance(charges = listOf(charge))
    createCourtCase(DpsDataCreator.dpsCreateCourtCase(appearances = listOf(courtAppearance)))
    webTestClient
      .get()
      .uri("/court-appearance-schedule/${courtAppearance.appearanceUuid}/delete-status")
      .headers {
        it.authToken(roles = listOf("ROLE_COURT_APPEARANCES__COURT_APPEARANCE_SCHEDULER__RO"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.status")
      .isEqualTo("SUPPORTED")
  }

  @Test
  fun `cannot delete a court appearance with a sentence`() {
    val (_, createdCourtCase) = createCourtCase()
    val courtAppearance = createdCourtCase.appearances.first()
    webTestClient
      .get()
      .uri("/court-appearance-schedule/${courtAppearance.appearanceUuid}/delete-status")
      .headers {
        it.authToken(roles = listOf("ROLE_COURT_APPEARANCES__COURT_APPEARANCE_SCHEDULER__RO"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.status")
      .isEqualTo("NOT_SUPPORTED")
  }

  @Test
  fun `no appearance exist for uuid results in not found`() {
    webTestClient
      .get()
      .uri("/court-appearance-schedule/${UUID.randomUUID()}/delete-status")
      .headers {
        it.authToken(roles = listOf("ROLE_COURT_APPEARANCES__COURT_APPEARANCE_SCHEDULER__RO"))
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
      .uri("/court-appearance-schedule/${createdAppearance.appearanceUuid}/delete-status")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `token with incorrect role is forbidden`() {
    val createdAppearance = createCourtCase().second.appearances.first()
    webTestClient
      .get()
      .uri("/court-appearance-schedule/${createdAppearance.appearanceUuid}/delete-status")
      .headers {
        it.authToken(roles = listOf("ROLE_OTHER_FUNCTION"))
      }
      .exchange()
      .expectStatus()
      .isForbidden
  }
}
