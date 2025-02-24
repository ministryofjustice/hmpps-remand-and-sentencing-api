package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.courtappearance

import org.assertj.core.api.Assertions
import org.hamcrest.text.MatchesPattern
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateCourtAppearanceResponse
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

    val historyRecords = courtAppearanceHistoryRepository.findAll().filter { it.appearanceUuid == createCourtAppearance.appearanceUuid }
    Assertions.assertThat(historyRecords).hasSize(1)
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
  fun `create charge with different outcome in second appearance keeps the first appearance outcome the same`() {
    val (courtCaseUuid, courtCase) = createCourtCase()
    val appearance = courtCase.appearances.first()
    val charge = appearance.charges.first()
    val oldOutcome = charge.outcomeUuid!! // f17328cf-ceaa-43c2-930a-26cf74480e18
    val newOutcome = UUID.fromString("315280e5-d53e-43b3-8ba6-44da25676ce2")
    val chargeWithNewOutcome = charge.copy(outcomeUuid = newOutcome)
    val newAppearance = DpsDataCreator.dpsCreateCourtAppearance(courtCaseUuid = courtCaseUuid, charges = listOf(chargeWithNewOutcome))
    val newAppearanceResponse = webTestClient
      .post()
      .uri("/court-appearance")
      .bodyValue(newAppearance)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isCreated
      .returnResult(CreateCourtAppearanceResponse::class.java)
      .responseBody.blockFirst()!!

    webTestClient
      .get()
      .uri("/court-appearance/${appearance.appearanceUuid}")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.charges[0].outcome.outcomeUuid")
      .isEqualTo(oldOutcome)

    webTestClient
      .get()
      .uri("/court-appearance/${newAppearanceResponse.appearanceUuid}")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.charges[0].outcome.outcomeUuid")
      .isEqualTo(newOutcome)
  }

  @Test
  fun `update charge with different offence code in second appearance results in charge created`() {
    val (courtCaseUuid, courtCase) = createCourtCase()
    val appearance = courtCase.appearances.first()
    val charge = appearance.charges.first()
    val chargeWithOffenceCode = charge.copy(offenceCode = "OFF634624")
    val newAppearance = DpsDataCreator.dpsCreateCourtAppearance(courtCaseUuid = courtCaseUuid, charges = listOf(chargeWithOffenceCode))
    val newAppearanceResponse = webTestClient
      .post()
      .uri("/court-appearance")
      .bodyValue(newAppearance)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isCreated
      .returnResult(CreateCourtAppearanceResponse::class.java)
      .responseBody.blockFirst()!!

    val messages = getMessages(4)
    Assertions.assertThat(messages).hasSize(4).extracting<String> { it.eventType }.contains("court-appearance.inserted", "charge.inserted", "charge.updated")

    webTestClient
      .get()
      .uri("/court-appearance/${appearance.appearanceUuid}")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.charges[0].lifetimeUuid")
      .isEqualTo(charge.lifetimeChargeUuid)

    webTestClient
      .get()
      .uri("/court-appearance/${newAppearanceResponse.appearanceUuid}")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.charges[?(@.lifetimeUuid == '${charge.lifetimeChargeUuid}')].offenceCode")
      .isEqualTo(charge.offenceCode)
      .jsonPath("$.charges[?(@.lifetimeUuid == '${charge.lifetimeChargeUuid}')].outcome.outcomeUuid")
      .isEqualTo("68e56c1f-b179-43da-9d00-1272805a7ad3") // replaced by another outcome
      .jsonPath("$.charges[?(@.lifetimeUuid != '${charge.lifetimeChargeUuid}')].offenceCode")
      .isEqualTo(chargeWithOffenceCode.offenceCode)
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
