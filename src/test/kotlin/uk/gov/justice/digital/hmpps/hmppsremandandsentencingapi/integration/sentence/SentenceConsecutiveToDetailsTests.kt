package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.sentence

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.SentenceConsecutiveToDetailsResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase

class SentenceConsecutiveToDetailsTests : IntegrationTestBase() {

  @Test
  fun `returns sentences to chain to grouped by appearance when there is an active sentence in the past`() {
    val (_, createCourtCase) = createCourtCase()
    val appearance = createCourtCase.appearances.first()
    val charge = appearance.charges.first()
    val sentence = charge.sentence!!
    val result = webTestClient.get()
      .uri {
        it.path("/sentence/consecutive-to-details")
          .queryParam("sentenceUuids", sentence.sentenceUuid)
          .build()
      }
      .headers { it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI")) }
      .exchange()
      .expectStatus()
      .isOk
      .returnResult(SentenceConsecutiveToDetailsResponse::class.java)
      .responseBody.blockFirst()!!

    Assertions.assertThat(result.sentences).hasSize(1)
    val sentenceConsecutiveToDetails = result.sentences[0]
    Assertions.assertThat(sentenceConsecutiveToDetails.appearanceDate).isEqualTo(appearance.appearanceDate)
    Assertions.assertThat(sentenceConsecutiveToDetails.courtCode).isEqualTo(appearance.courtCode)
    Assertions.assertThat(sentenceConsecutiveToDetails.courtCaseReference).isEqualTo(appearance.courtCaseReference)
    Assertions.assertThat(sentenceConsecutiveToDetails.sentenceUuid).isEqualTo(sentence.sentenceUuid!!)
    Assertions.assertThat(sentenceConsecutiveToDetails.offenceCode).isEqualTo(charge.offenceCode)
    Assertions.assertThat(sentenceConsecutiveToDetails.offenceStartDate).isEqualTo(charge.offenceStartDate)
    Assertions.assertThat(sentenceConsecutiveToDetails.offenceEndDate).isEqualTo(charge.offenceEndDate)
    Assertions.assertThat(sentenceConsecutiveToDetails.countNumber).isEqualTo(sentence.chargeNumber)
  }

  @Test
  fun `no token results in unauthorized`() {
    val (_, createCourtCase) = createCourtCase()
    val appearance = createCourtCase.appearances.first()
    val sentence = appearance.charges.first().sentence!!
    webTestClient.get()
      .uri {
        it.path("/sentence/consecutive-to-details")
          .queryParam("sentenceUuids", sentence.sentenceUuid)
          .build()
      }
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `token with incorrect role is forbidden`() {
    val (_, createCourtCase) = createCourtCase()
    val appearance = createCourtCase.appearances.first()
    val sentence = appearance.charges.first().sentence!!
    webTestClient.get()
      .uri {
        it.path("/sentence/consecutive-to-details")
          .queryParam("sentenceUuids", sentence.sentenceUuid)
          .build()
      }
      .headers { it.authToken(roles = listOf("ROLE_OTHER_FUNCTION")) }
      .exchange()
      .expectStatus()
      .isForbidden
  }
}
