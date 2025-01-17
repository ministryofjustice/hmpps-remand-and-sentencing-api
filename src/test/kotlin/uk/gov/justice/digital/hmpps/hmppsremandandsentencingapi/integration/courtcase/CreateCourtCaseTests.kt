package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.courtcase

import org.hamcrest.text.MatchesPattern
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.PeriodLengthType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.DpsDataCreator

class CreateCourtCaseTests : IntegrationTestBase() {

  @Test
  fun `Successfully create court case`() {
    val createCourtCase = DpsDataCreator.dpsCreateCourtCase()
    webTestClient
      .post()
      .uri("/court-case")
      .bodyValue(createCourtCase)
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
      .jsonPath("$.appearances[0].appearanceUuid")
      .isEqualTo(createCourtCase.appearances.first().appearanceUuid.toString())
      .jsonPath("$.charges[0].chargeUuid")
      .isEqualTo(createCourtCase.appearances.first().charges.first().chargeUuid.toString())
    expectInsertedMessages(createCourtCase.prisonerId)
  }

  @Test
  fun `create multiple consecutive to sentences for one forthwith sentence`() {
    val forthwithSentence = DpsDataCreator.dpsCreateSentence(
      chargeNumber = "1",
      sentenceServeType = "FORTHWITH",
      periodLengths = listOf(
        DpsDataCreator.dpsCreatePeriodLength(years = 4, type = PeriodLengthType.SENTENCE_LENGTH),
      ),
    )
    val firstConcurrentGroupOne = DpsDataCreator.dpsCreateSentence(
      chargeNumber = "2",
      sentenceServeType = "CONCURRENT",
      periodLengths = listOf(
        DpsDataCreator.dpsCreatePeriodLength(months = 27, periodOrder = "months", type = PeriodLengthType.SENTENCE_LENGTH),
      ),
    )
    val secondConcurrentGroupOne = DpsDataCreator.dpsCreateSentence(
      chargeNumber = "3",
      sentenceServeType = "CONCURRENT",
      periodLengths = listOf(
        DpsDataCreator.dpsCreatePeriodLength(months = 27, periodOrder = "months", type = PeriodLengthType.SENTENCE_LENGTH),
      ),
    )

    val firstConcurrentGroupTwo = DpsDataCreator.dpsCreateSentence(
      chargeNumber = "4",
      sentenceServeType = "CONSECUTIVE",
      consecutiveToChargeNumber = "1",
      periodLengths = listOf(
        DpsDataCreator.dpsCreatePeriodLength(months = 27, periodOrder = "months", type = PeriodLengthType.SENTENCE_LENGTH),
      ),
    )

    val secondConcurrentGroupTwo = DpsDataCreator.dpsCreateSentence(
      chargeNumber = "5",
      sentenceServeType = "CONSECUTIVE",
      consecutiveToChargeNumber = "1",
      periodLengths = listOf(
        DpsDataCreator.dpsCreatePeriodLength(months = 27, periodOrder = "months", type = PeriodLengthType.SENTENCE_LENGTH),
      ),
    )

    val thirdConcurrentGroupTwo = DpsDataCreator.dpsCreateSentence(
      chargeNumber = "6",
      sentenceServeType = "CONSECUTIVE",
      consecutiveToChargeNumber = "1",
      periodLengths = listOf(
        DpsDataCreator.dpsCreatePeriodLength(months = 27, periodOrder = "months", type = PeriodLengthType.SENTENCE_LENGTH),
      ),
    )

    val firstCharge = DpsDataCreator.dpsCreateCharge(sentence = forthwithSentence)
    val secondCharge = DpsDataCreator.dpsCreateCharge(sentence = firstConcurrentGroupOne)
    val thirdCharge = DpsDataCreator.dpsCreateCharge(sentence = secondConcurrentGroupOne)
    val fourthCharge = DpsDataCreator.dpsCreateCharge(sentence = firstConcurrentGroupTwo)
    val fifthCharge = DpsDataCreator.dpsCreateCharge(sentence = secondConcurrentGroupTwo)
    val sixthCharge = DpsDataCreator.dpsCreateCharge(sentence = thirdConcurrentGroupTwo)

    val appearance = DpsDataCreator.dpsCreateCourtAppearance(warrantType = "SENTENCING", charges = listOf(firstCharge, secondCharge, thirdCharge, fourthCharge, fifthCharge, sixthCharge))
    val courtCase = DpsDataCreator.dpsCreateCourtCase(appearances = listOf(appearance))

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
  }

  @Test
  fun `no token results in unauthorized`() {
    val createCourtCase = DpsDataCreator.dpsCreateCourtCase()
    webTestClient
      .post()
      .uri("/court-case")
      .bodyValue(createCourtCase)
      .headers {
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `token with incorrect role is forbidden`() {
    val createCourtCase = DpsDataCreator.dpsCreateCourtCase()
    webTestClient
      .post()
      .uri("/court-case")
      .bodyValue(createCourtCase)
      .headers {
        it.authToken(roles = listOf("ROLE_OTHER_FUNCTION"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isForbidden
  }
}
