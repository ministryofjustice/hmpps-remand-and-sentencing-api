package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.sentence

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.test.web.reactive.server.expectBody
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.DpsDataCreator
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.DpsDataCreator.Factory.DEFAULT_PRISONER_ID
import java.time.format.DateTimeFormatter
import java.util.UUID

class GetSentenceTests : IntegrationTestBase() {

  @Test
  fun `get sentence by uuid`() {
    val createdSentence = createCourtCase().second.appearances.first().charges.first().sentence!!
    webTestClient
      .get()
      .uri("/sentence/${createdSentence.sentenceUuid!!}")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING"))
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.sentenceUuid")
      .isEqualTo(createdSentence.sentenceUuid.toString())
      .jsonPath("$.chargeNumber")
      .isEqualTo(createdSentence.chargeNumber)
      .jsonPath("$.sentenceServeType")
      .isEqualTo(createdSentence.sentenceServeType)
      .jsonPath("$.convictionDate")
      .isEqualTo(createdSentence.convictionDate!!.format(DateTimeFormatter.ISO_DATE))
      .jsonPath("$.sentenceType.sentenceTypeUuid")
      .isEqualTo(createdSentence.sentenceTypeId.toString())
  }

  @Test
  fun `no sentence exist for uuid results in not found`() {
    webTestClient
      .get()
      .uri("/sentence/${UUID.randomUUID()}")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING"))
      }
      .exchange()
      .expectStatus()
      .isNotFound
  }

  @Test
  fun `no token results in unauthorized`() {
    val createdSentence = createCourtCase().second.appearances.first().charges.first().sentence!!
    webTestClient
      .get()
      .uri("/sentence/${createdSentence.sentenceUuid!!}")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `token with incorrect role is forbidden`() {
    val createdSentence = createCourtCase().second.appearances.first().charges.first().sentence!!
    webTestClient
      .get()
      .uri("/sentence/${createdSentence.sentenceUuid!!}")
      .headers {
        it.authToken(roles = listOf("ROLE_OTHER_FUNCTION"))
      }
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `has-sentences endpoint returns true if sentences exist`() {
    createCourtCase()

    val result = hasSentencesForPrisoner(DEFAULT_PRISONER_ID)

    assertThat(result).isTrue()
  }

  @Test
  fun `has-sentences endpoint returns false if no sentences exist`() {
    val appearance1 = DpsDataCreator.dpsCreateCourtAppearance(
      charges = emptyList(),
    )
    val courtCase1 = DpsDataCreator.dpsCreateCourtCase(appearances = listOf(appearance1))
    createCourtCase(courtCase1)

    val result = hasSentencesForPrisoner(DEFAULT_PRISONER_ID)

    assertThat(result).isFalse()
  }

  private fun hasSentencesForPrisoner(prisonerId: String): Boolean = webTestClient
    .get()
    .uri("/sentence/has-sentences/$prisonerId")
    .headers { it.authToken(roles = listOf("ROLE_REMAND_SENTENCING__RECORD_RECALL_RW")) }
    .exchange()
    .expectStatus().isOk
    .expectBody<Boolean>()
    .returnResult()
    .responseBody!!
}
