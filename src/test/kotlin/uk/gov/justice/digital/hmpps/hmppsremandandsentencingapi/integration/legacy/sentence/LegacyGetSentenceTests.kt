package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.sentence

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.DpsDataCreator
import java.util.UUID

class LegacyGetSentenceTests : IntegrationTestBase() {

  @Test
  fun `get sentence by lifetime uuid`() {
    val sentencedAppearance = DpsDataCreator.dpsCreateCourtAppearance(
      outcomeUuid = UUID.fromString("62412083-9892-48c9-bf01-7864af4a8b3c"),
      warrantType = "SENTENCING",
    )
    val (_, createdCourtCase) = createCourtCase(DpsDataCreator.dpsCreateCourtCase(appearances = listOf(sentencedAppearance)))
    val sentence = createdCourtCase.appearances.first().charges.first().sentence!!

    webTestClient
      .get()
      .uri("/legacy/sentence/${sentence.lifetimeSentenceUuid}")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_SENTENCE_RO"))
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.lifetimeUuid")
      .isEqualTo(sentence.lifetimeSentenceUuid.toString())
  }

  @Test
  fun `no charge exist for uuid results in not found`() {
    webTestClient
      .get()
      .uri("/legacy/sentence/${UUID.randomUUID()}")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_SENTENCE_RO"))
      }
      .exchange()
      .expectStatus()
      .isNotFound
  }

  @Test
  fun `no token results in unauthorized`() {
    val (_, createdCourtCase) = createCourtCase()
    val lifetimeUuid = createdCourtCase.appearances.first().charges.first().sentence!!.lifetimeSentenceUuid

    webTestClient
      .get()
      .uri("/legacy/sentence/$lifetimeUuid")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `token with incorrect role is forbidden`() {
    val (_, createdCourtCase) = createCourtCase()
    val lifetimeUuid = createdCourtCase.appearances.first().charges.first().sentence!!.lifetimeSentenceUuid
    webTestClient
      .get()
      .uri("/legacy/sentence/$lifetimeUuid")
      .headers {
        it.authToken(roles = listOf("ROLE_OTHER_FUNCTION"))
      }
      .exchange()
      .expectStatus()
      .isForbidden
  }
}
