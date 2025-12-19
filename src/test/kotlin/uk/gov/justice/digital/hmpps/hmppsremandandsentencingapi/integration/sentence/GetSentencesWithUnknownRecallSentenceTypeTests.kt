package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.sentence

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.MissingSentenceAppearance
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.PeriodLengthType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.DpsDataCreator
import java.util.UUID

class GetSentencesWithUnknownRecallSentenceTypeTests : IntegrationTestBase() {

  @Test
  fun `correctly filter sentences with multiple period lengths for a single unknown recall type`() {
    val unknownRecallSentenceTypeId = UUID.fromString("f9a1551e-86b1-425b-96f7-23465a0f05fc")

    // 1. Create a sentence that has multiple period lengths (Sentence length + License period)
    val sentenceWithMultiplePeriods = DpsDataCreator.dpsCreateSentence(
      sentenceTypeId = unknownRecallSentenceTypeId,
      periodLengths = listOf(
        DpsDataCreator.dpsCreatePeriodLength(years = 10, type= PeriodLengthType.SENTENCE_LENGTH),
        DpsDataCreator.dpsCreatePeriodLength(years = 4, type = PeriodLengthType.LICENCE_PERIOD)
      )
    )

    val sentenceWithOtherType = DpsDataCreator.dpsCreateSentence()

    val charge1 = DpsDataCreator.dpsCreateCharge(sentence = sentenceWithMultiplePeriods)
    val charge2 = DpsDataCreator.dpsCreateCharge(sentence = sentenceWithOtherType)

    val appearance = DpsDataCreator.dpsCreateCourtAppearance(charges = listOf(charge1, charge2))
    val (_, createdCourtCase) = createCourtCase(DpsDataCreator.dpsCreateCourtCase(appearances = listOf(appearance)))

    val firstAppearance = createdCourtCase.appearances.first()
    val sentenceWithUnknownRecallTypeUuid = firstAppearance.charges[0].sentence!!.sentenceUuid
    val sentenceWithOtherTypeUuid = firstAppearance.charges[1].sentence!!.sentenceUuid

    val sentenceUuids = listOf(sentenceWithUnknownRecallTypeUuid, sentenceWithOtherTypeUuid)

    val response = webTestClient
      .get()
      .uri { uriBuilder ->
        uriBuilder
          .path("/sentence/unknown-recall-type")
          .queryParam("sentenceUuids", sentenceUuids)
          .build()
      }
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBodyList(MissingSentenceAppearance::class.java)
      .returnResult()
      .responseBody

    // Assertions
    assertThat(response).isNotNull
    assertThat(response).hasSize(1)

    val appearanceResult = response!![0]
    assertThat(appearanceResult.sentences).hasSize(1) // Should only be 1 sentence object

    val sentenceResult = appearanceResult.sentences[0]
    assertThat(sentenceResult.sentenceUuid).isEqualTo(sentenceWithUnknownRecallTypeUuid)

    // THE CRITICAL TEST: Verify the list of period lengths was merged correctly
    assertThat(sentenceResult.periodLengths).hasSize(2)
    assertThat(sentenceResult.periodLengths.map { it.periodLengthType.name })
      .containsExactlyInAnyOrder("SENTENCE_LENGTH", "LICENCE_PERIOD")

    assertThat(sentenceResult.periodLengths.find { it.periodLengthType.name == "SENTENCE_LENGTH" }?.years).isEqualTo(10)
    assertThat(sentenceResult.periodLengths.find { it.periodLengthType.name == "LICENCE_PERIOD" }?.years).isEqualTo(4)
  }

  @Test
  fun `return empty list when no sentences have an unknown recall type`() {
    val knownSentenceTypeId = UUID.fromString("1104e683-5467-4340-b961-ff53672c4f39")

    val sentence1 = DpsDataCreator.dpsCreateSentence(sentenceTypeId = knownSentenceTypeId)
    val charge1 = DpsDataCreator.dpsCreateCharge(sentence = sentence1)
    val appearance = DpsDataCreator.dpsCreateCourtAppearance(charges = listOf(charge1))
    val (_, createdCourtCase) = createCourtCase(DpsDataCreator.dpsCreateCourtCase(appearances = listOf(appearance)))

    val sentenceUuid = createdCourtCase.appearances.first().charges[0].sentence!!.sentenceUuid

    val response = webTestClient
      .get()
      .uri { it.path("/sentence/unknown-recall-type").queryParam("sentenceUuids", listOf(sentenceUuid)).build() }
      .headers { it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI")) }
      .exchange()
      .expectStatus().isOk
      .expectBodyList(MissingSentenceAppearance::class.java)
      .returnResult().responseBody

    assertThat(response).isEmpty()
  }
}