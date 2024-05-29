package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.courtappearance

import org.hamcrest.text.MatchesPattern
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateCharge
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateCourtAppearance
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreatePeriodLength
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateSentence
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import java.time.LocalDate
import java.util.UUID

class CreateCourtAppearanceTests : IntegrationTestBase() {

  @Test
  fun `create appearance in existing court case`() {
    val courtCase = createCourtCase()
    val sentence = CreateSentence(null, "1", CreatePeriodLength(1, null, null, null, periodOrder = "years"), null, "FORTHWITH", null, "SDS (Standard Determinate Sentence)")
    val charge = CreateCharge(UUID.randomUUID(), "OFF123", LocalDate.now(), null, "OUT123", true, sentence)
    val appearance = CreateCourtAppearance(courtCase.first, UUID.randomUUID(), "OUT123", "COURT1", "GH123456789", LocalDate.now(), null, "REMAND", 1, null, null, listOf(charge))
    webTestClient
      .post()
      .uri("/court-appearance")
      .bodyValue(appearance)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isCreated
      .expectBody()
      .jsonPath("$.appearanceUuid")
      .value(MatchesPattern.matchesPattern("([a-f0-9]{8}(-[a-f0-9]{4}){4}[a-f0-9]{8})"))
  }

  @Test
  fun `create appearance with consecutive to sentences`() {
    val courtCase = createCourtCase()
    val forthWithSentence = CreateSentence(null, "1", CreatePeriodLength(1, null, null, null, periodOrder = "years"), null, "FORTHWITH", null, "SDS (Standard Determinate Sentence)")
    val forthWithCharge = CreateCharge(UUID.randomUUID(), "OFF123", LocalDate.now(), null, "OUT123", true, forthWithSentence)
    val concurrentSentence = CreateSentence(null, "2", CreatePeriodLength(1, null, null, null, periodOrder = "years"), null, "CONCURRENT", null, "SDS (Standard Determinate Sentence)")
    val concurrentCharge = CreateCharge(UUID.randomUUID(), "OFF123", LocalDate.now(), null, "OUT123", true, concurrentSentence)
    val consecutiveToSentence = CreateSentence(null, "3", CreatePeriodLength(1, null, null, null, periodOrder = "years"), null, "CONSECUTIVE_TO", "1", "SDS (Standard Determinate Sentence)")
    val consecutiveToCharge = CreateCharge(UUID.randomUUID(), "OFF123", LocalDate.now(), null, "OUT123", true, consecutiveToSentence)
    val appearance = CreateCourtAppearance(courtCase.first, UUID.randomUUID(), "OUT123", "COURT1", "GH123456789", LocalDate.now(), null, "SENTENCE", 1, CreatePeriodLength(6, null, null, null, "years"), null, listOf(consecutiveToCharge, concurrentCharge, forthWithCharge))
    webTestClient
      .post()
      .uri("/court-appearance")
      .bodyValue(appearance)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isCreated
      .expectBody()
      .jsonPath("$.appearanceUuid")
      .value(MatchesPattern.matchesPattern("([a-f0-9]{8}(-[a-f0-9]{4}){4}[a-f0-9]{8})"))
  }

  @Test
  fun `must not create appearance when no court case exists`() {
    val charge = CreateCharge(UUID.randomUUID(), "OFF123", LocalDate.now(), null, "OUT123", null, null)
    val appearance = CreateCourtAppearance(UUID.randomUUID().toString(), UUID.randomUUID(), "OUT123", "COURT1", "GH123456789", LocalDate.now(), null, "REMAND", 1, null, null, listOf(charge))
    webTestClient
      .post()
      .uri("/court-appearance")
      .bodyValue(appearance)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isNotFound
  }

  @Test
  fun `no token results in unauthorized`() {
    val charge = CreateCharge(UUID.randomUUID(), "OFF123", LocalDate.now(), null, "OUT123", null, null)
    val appearance = CreateCourtAppearance(UUID.randomUUID().toString(), UUID.randomUUID(), "OUT123", "COURT1", "GH123456789", LocalDate.now(), null, "REMAND", 1, null, null, listOf(charge))
    webTestClient
      .post()
      .uri("/court-appearance")
      .bodyValue(appearance)
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
    val appearance = CreateCourtAppearance(UUID.randomUUID().toString(), UUID.randomUUID(), "OUT123", "COURT1", "GH123456789", LocalDate.now(), null, "REMAND", 1, null, null, listOf(charge))
    webTestClient
      .post()
      .uri("/court-appearance")
      .bodyValue(appearance)
      .headers {
        it.authToken(roles = listOf("ROLE_OTHER_FUNCTION"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isForbidden
  }
}
