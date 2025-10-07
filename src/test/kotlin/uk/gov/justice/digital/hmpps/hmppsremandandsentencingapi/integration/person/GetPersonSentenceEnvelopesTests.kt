package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.person

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.sentenceenvelopes.PrisonerSentenceEnvelopes
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.DpsDataCreator
import java.util.UUID

class GetPersonSentenceEnvelopesTests : IntegrationTestBase() {

  @Test
  fun `get all sentence envelopes for a person`() {
    val forthwithSentence = DpsDataCreator.dpsCreateSentence()
    val secondSentence = DpsDataCreator.dpsCreateSentence(chargeNumber = "2", sentenceServeType = "CONSECUTIVE", consecutiveToSentenceUuid = forthwithSentence.sentenceUuid)
    val thirdSentence = DpsDataCreator.dpsCreateSentence(chargeNumber = "3", sentenceServeType = "CONSECUTIVE", consecutiveToSentenceUuid = secondSentence.sentenceUuid)

    val forthWithCharge = DpsDataCreator.dpsCreateCharge(sentence = forthwithSentence)
    val secondCharge = DpsDataCreator.dpsCreateCharge(sentence = secondSentence)
    val thirdCharge = DpsDataCreator.dpsCreateCharge(sentence = thirdSentence)

    val appearance = DpsDataCreator.dpsCreateCourtAppearance(charges = listOf(forthWithCharge, secondCharge, thirdCharge))
    val (_, courtCase) = createCourtCase(DpsDataCreator.dpsCreateCourtCase(appearances = listOf(appearance)))

    val sentenceEnvelopes = webTestClient
      .get()
      .uri("/person/${courtCase.prisonerId}/sentence-envelopes")
      .headers { it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI")) }
      .exchange()
      .expectStatus()
      .isOk
      .returnResult(PrisonerSentenceEnvelopes::class.java)
      .responseBody.blockFirst()!!

    Assertions.assertThat(sentenceEnvelopes.sentenceEnvelopes).size().isEqualTo(1)
    val sentenceEnvelope = sentenceEnvelopes.sentenceEnvelopes[0]
    Assertions.assertThat(sentenceEnvelope.envelopeStartDate).isEqualTo(appearance.appearanceDate)
    Assertions.assertThat(sentenceEnvelope.sentences).extracting<UUID> { it.sentenceUuid }.containsExactlyInAnyOrder(forthwithSentence.sentenceUuid, secondSentence.sentenceUuid, thirdSentence.sentenceUuid)
  }
}
