package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.courtcase

import org.hamcrest.text.MatchesPattern
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateCharge
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateCourtAppearance
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateCourtCase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import java.time.LocalDate
import java.util.UUID

class UpdateCourtCaseTests : IntegrationTestBase() {

  @Test
  fun `update court case`() {
    val courtCase = createCourtCase()
    val charge = CreateCharge(UUID.randomUUID(), "OFF123", LocalDate.now(), null, "OUT123")
    val appearance = CreateCourtAppearance(courtCase.first, courtCase.second.appearances.first().appearanceUuid, "OUT123", "COURT1", "ADIFFERENTCOURTCASEREFERENCE", LocalDate.now(), null, null, listOf(charge))
    val editedCourtCase = courtCase.second.copy(appearances = listOf(appearance))
    webTestClient
      .put()
      .uri("/court-case/${courtCase.first}")
      .bodyValue(editedCourtCase)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.courtCaseUuid")
      .value(MatchesPattern.matchesPattern("([a-f0-9]{8}(-[a-f0-9]{4}){4}[a-f0-9]{8})"))
  }

  @Test
  fun `cannot edit prisoner id of a court case`() {
    val courtCase = createCourtCase()
    val editedCourtCase = courtCase.second.copy(prisonerId = "ADIFFERENTPRISONER")

    webTestClient
      .put()
      .uri("/court-case/${courtCase.first}")
      .bodyValue(editedCourtCase)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isBadRequest
  }

  @Test
  fun `no token results in unauthorized`() {
    val charge = CreateCharge(UUID.randomUUID(), "OFF123", LocalDate.now(), null, "OUT123")
    val appearance = CreateCourtAppearance(UUID.randomUUID().toString(), UUID.randomUUID(), "OUT123", "COURT1", "GH123456789", LocalDate.now(), null, null, listOf(charge))
    val courtCase = CreateCourtCase("PRI123", listOf(appearance))
    webTestClient
      .put()
      .uri("/court-case/${UUID.randomUUID()}")
      .bodyValue(courtCase)
      .headers {
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `token with incorrect role is forbidden`() {
    val charge = CreateCharge(UUID.randomUUID(), "OFF123", LocalDate.now(), null, "OUT123")
    val appearance = CreateCourtAppearance(UUID.randomUUID().toString(), UUID.randomUUID(), "OUT123", "COURT1", "GH123456789", LocalDate.now(), null, null, listOf(charge))
    val courtCase = CreateCourtCase("PRI123", listOf(appearance))
    webTestClient
      .put()
      .uri("/court-case/${UUID.randomUUID()}")
      .bodyValue(courtCase)
      .headers {
        it.authToken(roles = listOf("ROLE_OTHER_FUNCTION"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isForbidden
  }
}
