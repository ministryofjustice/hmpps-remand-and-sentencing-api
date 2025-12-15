package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.sentence

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.Sentence
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.DpsDataCreator
import java.util.UUID

class GetSentencesWithUnknownRecallSentenceTypeTests : IntegrationTestBase() {

  @Test
  fun `correctly filter sentences with at least one unknown recall type`() {
    val unknownRecallSentenceTypeId = UUID.fromString("f9a1551e-86b1-425b-96f7-23465a0f05fc")

    val sentenceWithUnknownRecallType = DpsDataCreator.dpsCreateSentence(sentenceTypeId = unknownRecallSentenceTypeId)
    val sentenceWithOtherType = DpsDataCreator.dpsCreateSentence()

    val charge1 = DpsDataCreator.dpsCreateCharge(sentence = sentenceWithUnknownRecallType)
    val charge2 = DpsDataCreator.dpsCreateCharge(sentence = sentenceWithOtherType)

    val appearance = DpsDataCreator.dpsCreateCourtAppearance(charges = listOf(charge1, charge2))
    val (_, createdCourtCase) = createCourtCase(DpsDataCreator.dpsCreateCourtCase(appearances = listOf(appearance)))

    val sentenceWithUnknownRecallTypeUuid = createdCourtCase.appearances.first().charges[0].sentence!!.sentenceUuid
    val sentenceWithOtherTypeUuid = createdCourtCase.appearances.first().charges[1].sentence!!.sentenceUuid

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
      .expectBodyList(Sentence::class.java)
      .returnResult()
      .responseBody

    assert(response != null)
    assert(response!!.size == 1)
    assert(response[0].sentenceUuid == sentenceWithUnknownRecallTypeUuid)
  }

  @Test
  fun `return empty list when no sentences have an unknown recall type`() {
    val knownSentenceTypeId = UUID.fromString("1104e683-5467-4340-b961-ff53672c4f39") // Known recall type

    val sentence1 = DpsDataCreator.dpsCreateSentence(sentenceTypeId = knownSentenceTypeId)
    val sentence2 = DpsDataCreator.dpsCreateSentence(sentenceTypeId = knownSentenceTypeId)

    val charge1 = DpsDataCreator.dpsCreateCharge(sentence = sentence1)
    val charge2 = DpsDataCreator.dpsCreateCharge(sentence = sentence2)

    val appearance = DpsDataCreator.dpsCreateCourtAppearance(charges = listOf(charge1, charge2))
    val (_, createdCourtCase) = createCourtCase(DpsDataCreator.dpsCreateCourtCase(appearances = listOf(appearance)))

    val sentence1Uuid = createdCourtCase.appearances.first().charges[0].sentence!!.sentenceUuid
    val sentence2Uuid = createdCourtCase.appearances.first().charges[1].sentence!!.sentenceUuid

    val sentenceUuids = listOf(sentence1Uuid, sentence2Uuid)

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
      .expectBodyList(Sentence::class.java)
      .returnResult()
      .responseBody

    assert(response != null)
    assert(response!!.isEmpty())
  }
}
