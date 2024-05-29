package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.courtcase

import org.hamcrest.text.MatchesPattern
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateCharge
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateCourtAppearance
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateCourtCase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreatePeriodLength
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateSentence
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import java.time.LocalDate
import java.util.UUID

class CreateCourtCaseTests : IntegrationTestBase() {

  @Test
  fun `Successfully create court case`() {
    val sentence = CreateSentence(null, "1", CreatePeriodLength(1, null, null, null, periodOrder = "years"), null)
    val charge = CreateCharge(UUID.randomUUID(), "OFF123", LocalDate.now(), null, "OUT123", true, sentence)
    val appearance = CreateCourtAppearance(null, UUID.randomUUID(), "OUT123", "COURT1", "GH123456789", LocalDate.now(), null, "REMAND", 1, null, null, listOf(charge))
    val courtCase = CreateCourtCase("PRI123", listOf(appearance))
    webTestClient
      .post()
      .uri("/court-case")
      .bodyValue(courtCase)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isCreated
      .expectBody()
      .jsonPath("$.courtCaseUuid")
      .value(MatchesPattern.matchesPattern("([a-f0-9]{8}(-[a-f0-9]{4}){4}[a-f0-9]{8})"))
  }

  @Test
  fun `no token results in unauthorized`() {
    val charge = CreateCharge(UUID.randomUUID(), "OFF123", LocalDate.now(), null, "OUT123", null, null)
    val appearance = CreateCourtAppearance(null, UUID.randomUUID(), "OUT123", "COURT1", "GH123456789", LocalDate.now(), null, "REMAND", 1, null, null, listOf(charge))
    val courtCase = CreateCourtCase("PRI123", listOf(appearance))
    webTestClient
      .post()
      .uri("/court-case")
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
    val charge = CreateCharge(UUID.randomUUID(), "OFF123", LocalDate.now(), null, "OUT123", null, null)
    val appearance = CreateCourtAppearance(null, UUID.randomUUID(), "OUT123", "COURT1", "GH123456789", LocalDate.now(), null, "REMAND", 1, null, null, listOf(charge))
    val courtCase = CreateCourtCase("PRI123", listOf(appearance))
    webTestClient
      .post()
      .uri("/court-case")
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
