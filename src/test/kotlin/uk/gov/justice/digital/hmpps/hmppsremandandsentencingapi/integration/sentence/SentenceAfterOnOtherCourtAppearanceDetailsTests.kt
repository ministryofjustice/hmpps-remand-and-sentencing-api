package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.sentence

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.DpsDataCreator
import java.time.format.DateTimeFormatter

class SentenceAfterOnOtherCourtAppearanceDetailsTests : IntegrationTestBase() {

  @Test
  fun `return appearance details of other appearance`() {
    val sentence = DpsDataCreator.dpsCreateSentence()
    val charge = DpsDataCreator.dpsCreateCharge(sentence = sentence)
    val courtAppearance = DpsDataCreator.dpsCreateCourtAppearance(charges = listOf(charge))
    createCourtCase(DpsDataCreator.dpsCreateCourtCase(appearances = listOf(courtAppearance)))
    val otherCaseSentence = DpsDataCreator.dpsCreateSentence(consecutiveToSentenceUuid = sentence.sentenceUuid)
    val otherCaseCharge = DpsDataCreator.dpsCreateCharge(sentence = otherCaseSentence)
    val otherCaseAppearance = DpsDataCreator.dpsCreateCourtAppearance(charges = listOf(otherCaseCharge))
    createCourtCase(DpsDataCreator.dpsCreateCourtCase(appearances = listOf(otherCaseAppearance)))

    webTestClient
      .get()
      .uri("/sentence/${sentence.sentenceUuid!!}/sentences-after-on-other-court-appearance-details")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.appearances[?(@.appearanceUuid == '${otherCaseAppearance.appearanceUuid}')].caseReference")
      .isEqualTo(otherCaseAppearance.courtCaseReference!!)
      .jsonPath("$.appearances[?(@.appearanceUuid == '${otherCaseAppearance.appearanceUuid}')].appearanceDate")
      .isEqualTo(otherCaseAppearance.appearanceDate.format(DateTimeFormatter.ISO_DATE))
      .jsonPath("$.appearances[?(@.appearanceUuid == '${otherCaseAppearance.appearanceUuid}')].courtCode")
      .isEqualTo(otherCaseAppearance.courtCode)
  }

  @Test
  fun `no token results in unauthorized`() {
    val createdSentence = createCourtCase().second.appearances.first().charges.first().sentence!!
    webTestClient
      .get()
      .uri("/sentence/${createdSentence.sentenceUuid!!}/sentences-after-on-other-court-appearance-details")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `token with incorrect role is forbidden`() {
    val createdSentence = createCourtCase().second.appearances.first().charges.first().sentence!!
    webTestClient
      .get()
      .uri("/sentence/${createdSentence.sentenceUuid!!}/sentences-after-on-other-court-appearance-details")
      .headers {
        it.authToken(roles = listOf("ROLE_OTHER_FUNCTION"))
      }
      .exchange()
      .expectStatus()
      .isForbidden
  }
}
