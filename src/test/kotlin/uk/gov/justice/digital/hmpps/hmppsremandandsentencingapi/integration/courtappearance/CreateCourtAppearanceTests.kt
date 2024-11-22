package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.courtappearance

import org.hamcrest.text.MatchesPattern
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.DpsDataCreator
import java.util.UUID

class CreateCourtAppearanceTests : IntegrationTestBase() {

  @Test
  fun `create appearance in existing court case`() {
    val courtCase = createCourtCase()
    val createCourtAppearance = DpsDataCreator.dpsCreateCourtAppearance(courtCaseUuid = courtCase.first)
    webTestClient
      .post()
      .uri("/court-appearance")
      .bodyValue(createCourtAppearance)
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
    val forthWithSentence = DpsDataCreator.dpsCreateSentence()
    val forthWithCharge = DpsDataCreator.dpsCreateCharge(sentence = forthWithSentence)
    val concurrentSentence = DpsDataCreator.dpsCreateSentence(chargeNumber = "2", sentenceServeType = "CONCURRENT")
    val concurrentCharge = DpsDataCreator.dpsCreateCharge(sentence = concurrentSentence)
    val consecutiveToSentence = DpsDataCreator.dpsCreateSentence(chargeNumber = "3", sentenceServeType = "CONSECUTIVE_TO", consecutiveToChargeNumber = "1")
    val consecutiveToCharge = DpsDataCreator.dpsCreateCharge(sentence = consecutiveToSentence)
    val appearance = DpsDataCreator.dpsCreateCourtAppearance(courtCaseUuid = courtCase.first, charges = listOf(consecutiveToCharge, concurrentCharge, forthWithCharge), overallSentenceLength = DpsDataCreator.dpsCreatePeriodLength(years = 6))
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
    val createCourtAppearance = DpsDataCreator.dpsCreateCourtAppearance(courtCaseUuid = UUID.randomUUID().toString())
    webTestClient
      .post()
      .uri("/court-appearance")
      .bodyValue(createCourtAppearance)
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
    val createCourtAppearance = DpsDataCreator.dpsCreateCourtAppearance()
    webTestClient
      .post()
      .uri("/court-appearance")
      .bodyValue(createCourtAppearance)
      .headers {
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `token with incorrect role is forbidden`() {
    val createCourtAppearance = DpsDataCreator.dpsCreateCourtAppearance()
    webTestClient
      .post()
      .uri("/court-appearance")
      .bodyValue(createCourtAppearance)
      .headers {
        it.authToken(roles = listOf("ROLE_OTHER_FUNCTION"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isForbidden
  }
}
