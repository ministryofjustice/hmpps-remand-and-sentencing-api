package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.sentence

import org.junit.jupiter.api.Test
import org.springframework.test.context.jdbc.Sql
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import java.time.format.DateTimeFormatter
import java.util.UUID

private const val TEST_SENTENCE_UUID_1 = "550e8400-e29b-41d4-a716-446655440000"
private const val TEST_SENTENCE_UUID_2 = "550e8400-e29b-41d4-a716-446655449999"

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
  @Sql("classpath:test_data/insert-sentences-to-recall.sql")
  fun `get sentences by uuids`() {
    webTestClient
      .get()
      .uri {
        it.path("/sentence")
          .queryParam("sentenceUuids", listOf(TEST_SENTENCE_UUID_1, TEST_SENTENCE_UUID_2))
          .build()
      }
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING"))
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.[0]sentenceUuid")
      .isEqualTo(TEST_SENTENCE_UUID_1)
      .jsonPath("$.[1]sentenceUuid")
      .isEqualTo(TEST_SENTENCE_UUID_2)
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
}
