package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.sentence

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacySearchSentence
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.DpsDataCreator
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.DpsDataCreator.Factory.dpsCreateSentence
import java.util.UUID

class LegacySearchSentenceTests : IntegrationTestBase() {

  @Test
  fun `get sentence by lifetime uuid`() {
    val firstCharge = DpsDataCreator.dpsCreateCharge(sentence = dpsCreateSentence())
    val secondCharge = DpsDataCreator.dpsCreateCharge(sentence = dpsCreateSentence())
    val appearance = DpsDataCreator.dpsCreateCourtAppearance(charges = listOf(firstCharge, secondCharge))
    val (_, courtCase) = createCourtCase(DpsDataCreator.dpsCreateCourtCase(appearances = listOf(appearance)))
    val sentenceOne = courtCase.appearances.first().charges.first().sentence!!
    val sentenceTwo = courtCase.appearances.first().charges[1].sentence!!

    webTestClient
      .post()
      .uri("/legacy/sentence/search")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_SENTENCE_RO"))
      }
      .bodyValue(LegacySearchSentence(listOf(sentenceOne.sentenceUuid!!, sentenceTwo.sentenceUuid!!)))
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$[0].lifetimeUuid")
      .isEqualTo(sentenceTwo.sentenceUuid.toString())
      .jsonPath("$[1].lifetimeUuid")
      .isEqualTo(sentenceOne.sentenceUuid.toString())
  }

  @Test
  fun `no token results in unauthorized`() {
    webTestClient
      .post()
      .uri("/legacy/sentence/search")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `token with incorrect role is forbidden`() {
    webTestClient
      .post()
      .uri("/legacy/sentence/search")
      .headers {
        it.authToken(roles = listOf("ROLE_OTHER_FUNCTION"))
      }
      .bodyValue(LegacySearchSentence(listOf(UUID.randomUUID())))
      .exchange()
      .expectStatus()
      .isForbidden
  }
}
