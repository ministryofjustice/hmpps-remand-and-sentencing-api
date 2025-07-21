package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.sentence

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.DpsDataCreator

class HasSentenceAfterOnOtherCourtAppearanceTests : IntegrationTestBase() {

  @Test
  fun `when sentence on other case is after return true`() {
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
      .uri("/sentence/${sentence.sentenceUuid!!}/has-sentences-after-on-other-court-appearance")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.hasSentenceAfterOnOtherCourtAppearance")
      .isEqualTo(true)
  }

  @Test
  fun `when sentence on same case other court appearance is after return true`() {
    val sentence = DpsDataCreator.dpsCreateSentence()
    val charge = DpsDataCreator.dpsCreateCharge(sentence = sentence)
    val courtAppearance = DpsDataCreator.dpsCreateCourtAppearance(charges = listOf(charge))
    val otherCaseSentence = DpsDataCreator.dpsCreateSentence(consecutiveToSentenceUuid = sentence.sentenceUuid)
    val otherCaseCharge = DpsDataCreator.dpsCreateCharge(sentence = otherCaseSentence)
    val otherCaseAppearance = DpsDataCreator.dpsCreateCourtAppearance(charges = listOf(otherCaseCharge))
    createCourtCase(DpsDataCreator.dpsCreateCourtCase(appearances = listOf(courtAppearance, otherCaseAppearance)))
    webTestClient
      .get()
      .uri("/sentence/${sentence.sentenceUuid!!}/has-sentences-after-on-other-court-appearance")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.hasSentenceAfterOnOtherCourtAppearance")
      .isEqualTo(true)
  }

  @Test
  fun `when sentence on same case same appearance is after return false`() {
    val sentence = DpsDataCreator.dpsCreateSentence()
    val otherSentence = DpsDataCreator.dpsCreateSentence(sentenceReference = "1", consecutiveToSentenceReference = sentence.sentenceReference)
    val charge = DpsDataCreator.dpsCreateCharge(sentence = sentence)
    val otherCharge = DpsDataCreator.dpsCreateCharge(sentence = otherSentence)
    val courtAppearance = DpsDataCreator.dpsCreateCourtAppearance(charges = listOf(charge, otherCharge))
    createCourtCase(DpsDataCreator.dpsCreateCourtCase(appearances = listOf(courtAppearance)))
    webTestClient
      .get()
      .uri("/sentence/${sentence.sentenceUuid!!}/has-sentences-after-on-other-court-appearance")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.hasSentenceAfterOnOtherCourtAppearance")
      .isEqualTo(false)
  }

  @Test
  fun `no token results in unauthorized`() {
    val createdSentence = createCourtCase().second.appearances.first().charges.first().sentence!!
    webTestClient
      .get()
      .uri("/sentence/${createdSentence.sentenceUuid!!}/has-sentences-after-on-other-court-appearance")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `token with incorrect role is forbidden`() {
    val createdSentence = createCourtCase().second.appearances.first().charges.first().sentence!!
    webTestClient
      .get()
      .uri("/sentence/${createdSentence.sentenceUuid!!}/has-sentences-after-on-other-court-appearance")
      .headers {
        it.authToken(roles = listOf("ROLE_OTHER_FUNCTION"))
      }
      .exchange()
      .expectStatus()
      .isForbidden
  }
}
