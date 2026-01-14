package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.sentence

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.util.DataCreator
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.wiremock.AdjustmentsApiExtension.Companion.adjustmentsApi
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.DpsDataCreator
import java.util.UUID

class LegacyDeleteSentenceTests : IntegrationTestBase() {

  @Test
  fun `can delete sentence`() {
    val (lifetimeUuid) = createLegacySentence()
    webTestClient
      .delete()
      .uri("/legacy/sentence/$lifetimeUuid")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_SENTENCE_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isNoContent
    val message = getMessages(1)[0]
    assertThat(message.eventType).isEqualTo("sentence.deleted")
    assertThat(message.additionalInformation.get("source").asText()).isEqualTo("NOMIS")
  }

  @Test
  fun `can delete recall sentence`() {
    // Create sentence with associated recall data.
    val (lifetimeUuid) = createLegacySentence(
      legacySentence = DataCreator.legacyCreateSentence(sentenceLegacyData = DataCreator.sentenceLegacyData(sentenceCalcType = "FTR_ORA", sentenceCategory = "2020")),
    )

    adjustmentsApi.stubGetAdjustmentsDefaultToNone()
    val recallsBeforeDelete = getRecallsByPrisonerId(DpsDataCreator.DEFAULT_PRISONER_ID)
    assertThat(recallsBeforeDelete).isNotEmpty

    // Delete sentence
    webTestClient
      .delete()
      .uri("/legacy/sentence/$lifetimeUuid")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_SENTENCE_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isNoContent

    // Check associated recall has been deleted and recall history updated.
    assertThat(getRecallsByPrisonerId(DpsDataCreator.DEFAULT_PRISONER_ID)).isEmpty()

    val historicalRecalls = recallHistoryRepository.findByRecallUuid(recallsBeforeDelete[0].recallUuid)
    assertThat(historicalRecalls).hasSize(2)
  }

  @Test
  fun `can delete sentence where court appearance has no outcome`() {
    val (lifetimeUuid) = createLegacySentence(
      legacyCreateCourtAppearance = DataCreator.legacyCreateCourtAppearance(
        legacyData = DataCreator.courtAppearanceLegacyData(
          outcomeConvictionFlag = null,
          outcomeDispositionCode = null,
          outcomeDescription = null,
          nomisOutcomeCode = null,
        ),
      ),
    )

    webTestClient
      .delete()
      .uri("/legacy/sentence/$lifetimeUuid")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_SENTENCE_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isNoContent

    val message = getMessages(1)[0]
    assertThat(message.eventType).isEqualTo("sentence.deleted")
    assertThat(message.additionalInformation.get("source").asText()).isEqualTo("NOMIS")
  }

  @Test
  fun `no token results in unauthorized`() {
    webTestClient
      .delete()
      .uri("/legacy/sentence/${UUID.randomUUID()}")
      .headers {
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `token with incorrect role is forbidden`() {
    webTestClient
      .delete()
      .uri("/legacy/sentence/${UUID.randomUUID()}")
      .headers {
        it.authToken(roles = listOf("ROLE_OTHER_FUNCTION"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isForbidden
  }
}
