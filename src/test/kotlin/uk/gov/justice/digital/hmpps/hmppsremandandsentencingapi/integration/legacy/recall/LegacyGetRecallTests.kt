package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.recall

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.DpsDataCreator
import java.util.UUID

class LegacyGetRecallTests : IntegrationTestBase() {

  @Test
  fun `get recall by uuid`() {
    val (sentenceOne, sentenceTwo) = createCourtCaseTwoSentences()
    val recall = DpsDataCreator.dpsCreateRecall(
      createdByUsername = "username1",
      sentenceIds = listOf(
        sentenceOne.sentenceUuid!!,
        sentenceTwo.sentenceUuid!!,
      ),
    )
    val createRecall = createRecall(recall)

    webTestClient
      .get()
      .uri("/legacy/recall/${createRecall.recallUuid}")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_SENTENCE_RO"))
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.recallUuid")
      .isEqualTo(createRecall.recallUuid.toString())
      .jsonPath("$.returnToCustodyDate")
      .isEqualTo(recall.returnToCustodyDate.toString())
      .jsonPath("$.revocationDate")
      .isEqualTo(recall.revocationDate.toString())
      .jsonPath("$.recallBy")
      .isEqualTo("username1")
      .jsonPath("$.recallType")
      .isEqualTo("FTR_14")
  }

  @Test
  fun `no token results in unauthorized`() {
    webTestClient
      .get()
      .uri("/legacy/recall/${UUID.randomUUID()}")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `token with incorrect role is forbidden`() {
    webTestClient
      .get()
      .uri("/legacy/recall/${UUID.randomUUID()}")
      .headers {
        it.authToken(roles = listOf("ROLE_OTHER_FUNCTION"))
      }
      .exchange()
      .expectStatus()
      .isForbidden
  }
}
