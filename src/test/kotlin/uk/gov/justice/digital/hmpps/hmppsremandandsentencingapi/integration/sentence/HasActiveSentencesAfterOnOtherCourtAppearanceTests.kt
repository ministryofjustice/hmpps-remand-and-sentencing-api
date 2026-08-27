package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.sentence

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.SentenceUuidsWithActiveSentencesAfterResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.SentenceEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.DpsDataCreator
import java.util.UUID

class HasActiveSentencesAfterOnOtherCourtAppearanceTests : IntegrationTestBase() {

  @Test
  fun `returns the sentence uuid when it has an active consecutive sentence outside the selection`() {
    val sentence = DpsDataCreator.dpsCreateSentence()
    val charge = DpsDataCreator.dpsCreateCharge(sentence = sentence)
    val appearance = DpsDataCreator.dpsCreateCourtAppearance(charges = listOf(charge))
    createCourtCase(DpsDataCreator.dpsCreateCourtCase(appearances = listOf(appearance)))

    val consecutiveSentence = DpsDataCreator.dpsCreateSentence(consecutiveToSentenceUuid = sentence.sentenceUuid)
    val consecutiveCharge = DpsDataCreator.dpsCreateCharge(sentence = consecutiveSentence)
    val consecutiveAppearance = DpsDataCreator.dpsCreateCourtAppearance(charges = listOf(consecutiveCharge))
    createCourtCase(DpsDataCreator.dpsCreateCourtCase(appearances = listOf(consecutiveAppearance)))

    val result = getSentenceUuidsWithActiveSentencesAfter(listOf(sentence.sentenceUuid))

    assertThat(result).containsExactly(sentence.sentenceUuid)
  }

  @Test
  fun `does not return the sentence uuid when the consecutive sentence is also part of the selection`() {
    val sentence = DpsDataCreator.dpsCreateSentence()
    val charge = DpsDataCreator.dpsCreateCharge(sentence = sentence)
    val appearance = DpsDataCreator.dpsCreateCourtAppearance(charges = listOf(charge))
    createCourtCase(DpsDataCreator.dpsCreateCourtCase(appearances = listOf(appearance)))

    val consecutiveSentence = DpsDataCreator.dpsCreateSentence(consecutiveToSentenceUuid = sentence.sentenceUuid)
    val consecutiveCharge = DpsDataCreator.dpsCreateCharge(sentence = consecutiveSentence)
    val consecutiveAppearance = DpsDataCreator.dpsCreateCourtAppearance(charges = listOf(consecutiveCharge))
    createCourtCase(DpsDataCreator.dpsCreateCourtCase(appearances = listOf(consecutiveAppearance)))

    val result = getSentenceUuidsWithActiveSentencesAfter(listOf(sentence.sentenceUuid, consecutiveSentence.sentenceUuid))

    assertThat(result).isEmpty()
  }

  @Test
  fun `does not return the sentence uuid when the consecutive sentence is not active`() {
    val sentence = DpsDataCreator.dpsCreateSentence()
    val charge = DpsDataCreator.dpsCreateCharge(sentence = sentence)
    val appearance = DpsDataCreator.dpsCreateCourtAppearance(charges = listOf(charge))
    createCourtCase(DpsDataCreator.dpsCreateCourtCase(appearances = listOf(appearance)))

    val consecutiveSentence = DpsDataCreator.dpsCreateSentence(consecutiveToSentenceUuid = sentence.sentenceUuid)
    val consecutiveCharge = DpsDataCreator.dpsCreateCharge(sentence = consecutiveSentence)
    val consecutiveAppearance = DpsDataCreator.dpsCreateCourtAppearance(charges = listOf(consecutiveCharge))
    createCourtCase(DpsDataCreator.dpsCreateCourtCase(appearances = listOf(consecutiveAppearance)))

    val consecutiveSentenceEntity = sentenceRepository.findBySentenceUuid(consecutiveSentence.sentenceUuid).first()
    consecutiveSentenceEntity.statusId = SentenceEntityStatus.INACTIVE
    sentenceRepository.save(consecutiveSentenceEntity)

    val result = getSentenceUuidsWithActiveSentencesAfter(listOf(sentence.sentenceUuid))

    assertThat(result).isEmpty()
  }

  @Test
  fun `returns an empty list when none of the selected sentences have a consecutive sentence`() {
    val sentence = createCourtCase().second.appearances.first().charges.first().sentence!!

    val result = getSentenceUuidsWithActiveSentencesAfter(listOf(sentence.sentenceUuid))

    assertThat(result).isEmpty()
  }

  @Test
  fun `returns only the sentence uuids that have an active consecutive sentence outside the selection, out of multiple chosen sentences`() {
    val sentence1 = DpsDataCreator.dpsCreateSentence()
    val sentence2 = DpsDataCreator.dpsCreateSentence()
    val sentence3 = DpsDataCreator.dpsCreateSentence()
    val sentence4 = DpsDataCreator.dpsCreateSentence()
    val charge1 = DpsDataCreator.dpsCreateCharge(sentence = sentence1)
    val charge2 = DpsDataCreator.dpsCreateCharge(sentence = sentence2)
    val charge3 = DpsDataCreator.dpsCreateCharge(sentence = sentence3)
    val charge4 = DpsDataCreator.dpsCreateCharge(sentence = sentence4)
    val appearance = DpsDataCreator.dpsCreateCourtAppearance(charges = listOf(charge1, charge2, charge3, charge4))
    createCourtCase(DpsDataCreator.dpsCreateCourtCase(appearances = listOf(appearance)))

    // sentence1 and sentence3 each have an active sentence, outside the selection, consecutive to them
    val consecutiveToSentence1 = DpsDataCreator.dpsCreateSentence(consecutiveToSentenceUuid = sentence1.sentenceUuid)
    val consecutiveToSentence1Charge = DpsDataCreator.dpsCreateCharge(sentence = consecutiveToSentence1)
    val consecutiveToSentence1Appearance = DpsDataCreator.dpsCreateCourtAppearance(charges = listOf(consecutiveToSentence1Charge))
    createCourtCase(DpsDataCreator.dpsCreateCourtCase(appearances = listOf(consecutiveToSentence1Appearance)))

    val consecutiveToSentence3 = DpsDataCreator.dpsCreateSentence(consecutiveToSentenceUuid = sentence3.sentenceUuid)
    val consecutiveToSentence3Charge = DpsDataCreator.dpsCreateCharge(sentence = consecutiveToSentence3)
    val consecutiveToSentence3Appearance = DpsDataCreator.dpsCreateCourtAppearance(charges = listOf(consecutiveToSentence3Charge))
    createCourtCase(DpsDataCreator.dpsCreateCourtCase(appearances = listOf(consecutiveToSentence3Appearance)))

    // sentence2 and sentence4 have no consecutive sentence pointing to them at all

    val result = getSentenceUuidsWithActiveSentencesAfter(
      listOf(sentence1.sentenceUuid, sentence2.sentenceUuid, sentence3.sentenceUuid, sentence4.sentenceUuid),
    )

    assertThat(result).containsExactlyInAnyOrder(sentence1.sentenceUuid, sentence3.sentenceUuid)
  }

  @Test
  fun `no token results in unauthorized`() {
    val sentence = createCourtCase().second.appearances.first().charges.first().sentence!!
    webTestClient.get()
      .uri {
        it.path("/sentence/has-active-sentences-after-on-other-court-appearance")
          .queryParam("sentenceUuids", sentence.sentenceUuid)
          .build()
      }
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `token with incorrect role is forbidden`() {
    val sentence = createCourtCase().second.appearances.first().charges.first().sentence!!
    webTestClient.get()
      .uri {
        it.path("/sentence/has-active-sentences-after-on-other-court-appearance")
          .queryParam("sentenceUuids", sentence.sentenceUuid)
          .build()
      }
      .headers { it.authToken(roles = listOf("ROLE_OTHER_FUNCTION")) }
      .exchange()
      .expectStatus()
      .isForbidden
  }

  private fun getSentenceUuidsWithActiveSentencesAfter(sentenceUuids: List<UUID>): List<UUID> = webTestClient.get()
    .uri {
      it.path("/sentence/has-active-sentences-after-on-other-court-appearance")
        .queryParam("sentenceUuids", *sentenceUuids.toTypedArray())
        .build()
    }
    .headers { it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI")) }
    .exchange()
    .expectStatus()
    .isOk
    .returnResult(SentenceUuidsWithActiveSentencesAfterResponse::class.java)
    .responseBody
    .blockFirst()!!
    .sentenceUuidsWithActiveSentencesAfter
}
