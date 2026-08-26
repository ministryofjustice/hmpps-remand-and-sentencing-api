package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.sentence

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.SentenceEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.DpsDataCreator
import java.util.UUID

class SentencesBlockingMarkAsInactiveTests : IntegrationTestBase() {

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

    val result = getSentencesBlockingMarkAsInactive(listOf(sentence.sentenceUuid))

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

    val result = getSentencesBlockingMarkAsInactive(listOf(sentence.sentenceUuid, consecutiveSentence.sentenceUuid))

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

    val result = getSentencesBlockingMarkAsInactive(listOf(sentence.sentenceUuid))

    assertThat(result).isEmpty()
  }

  @Test
  fun `returns an empty list when none of the selected sentences have a consecutive sentence`() {
    val sentence = createCourtCase().second.appearances.first().charges.first().sentence!!

    val result = getSentencesBlockingMarkAsInactive(listOf(sentence.sentenceUuid))

    assertThat(result).isEmpty()
  }

  @Test
  fun `no token results in unauthorized`() {
    val sentence = createCourtCase().second.appearances.first().charges.first().sentence!!
    webTestClient.get()
      .uri {
        it.path("/sentence/sentences-blocking-mark-as-inactive")
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
        it.path("/sentence/sentences-blocking-mark-as-inactive")
          .queryParam("sentenceUuids", sentence.sentenceUuid)
          .build()
      }
      .headers { it.authToken(roles = listOf("ROLE_OTHER_FUNCTION")) }
      .exchange()
      .expectStatus()
      .isForbidden
  }

  private fun getSentencesBlockingMarkAsInactive(sentenceUuids: List<UUID>): List<UUID> = webTestClient.get()
    .uri {
      it.path("/sentence/sentences-blocking-mark-as-inactive")
        .queryParam("sentenceUuids", *sentenceUuids.toTypedArray())
        .build()
    }
    .headers { it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI")) }
    .exchange()
    .expectStatus()
    .isOk
    .returnResult(UUID::class.java)
    .responseBody
    .collectList()
    .block()!!
}
