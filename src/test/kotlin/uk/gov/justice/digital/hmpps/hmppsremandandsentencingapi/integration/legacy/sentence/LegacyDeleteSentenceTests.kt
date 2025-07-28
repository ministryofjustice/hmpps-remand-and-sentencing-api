package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.sentence

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.util.DataCreator
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.audit.RecallHistoryRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.audit.RecallSentenceHistoryRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.DpsDataCreator
import java.time.LocalDate
import java.util.UUID

class LegacyDeleteSentenceTests : IntegrationTestBase() {

  @Autowired
  private lateinit var recallHistoryRepository: RecallHistoryRepository

  @Autowired
  private lateinit var recallSentenceHistoryRepository: RecallSentenceHistoryRepository

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
    assertThat(historicalRecalls).hasSize(1)
    assertThat(historicalRecalls[0].historyStatusId).isEqualTo(EntityStatus.DELETED)
    assertThat(historicalRecalls[0].historyCreatedAt).isNotNull()

    val historicalRecallSentences = recallSentenceHistoryRepository.findByRecallHistoryId(historicalRecalls[0].id)
    assertThat(historicalRecallSentences!!).hasSize(1)
    assertThat(historicalRecallSentences.map { it.sentence.sentenceUuid }).containsExactlyInAnyOrder(lifetimeUuid)
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

  @Test
  fun `should not show deleted sentences when querying recalls`() {
    // This test verifies that deleted sentences are properly filtered out when querying recalls
    val prisonerId = "DEL001"

    // Step 1: Create court case and appearance
    val courtCase = DataCreator.legacyCreateCourtCase(prisonerId = prisonerId)
    val appearance = DataCreator.legacyCreateCourtAppearance()

    // Step 2: Create two recall sentences
    val firstCharge = DataCreator.legacyCreateCharge()
    val firstRecallSentence = DataCreator.legacyCreateSentence(
      returnToCustodyDate = LocalDate.now(),
      sentenceLegacyData = DataCreator.sentenceLegacyData(
        sentenceCalcType = "FTR", // Fixed Term Recall
        sentenceCategory = "2003",
      ),
    )
    val (firstSentenceUuid, _) = createLegacySentence(courtCase, appearance, firstCharge, firstRecallSentence)

    val secondCharge = DataCreator.legacyCreateCharge()
    val secondRecallSentence = DataCreator.legacyCreateSentence(
      returnToCustodyDate = LocalDate.now(),
      sentenceLegacyData = DataCreator.sentenceLegacyData(
        sentenceCalcType = "FTR", // Fixed Term Recall
        sentenceCategory = "2003",
      ),
    )
    createLegacySentence(courtCase, appearance, secondCharge, secondRecallSentence)

    // Step 3: Delete the first sentence
    webTestClient
      .delete()
      .uri("/legacy/sentence/$firstSentenceUuid")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_SENTENCE_RW"))
      }
      .exchange()
      .expectStatus().isNoContent

    // Step 4: Query recalls
    val recalls = getRecallsByPrisonerId(prisonerId)

    // ASSERTION: Should have recalls but only with active sentences
    assertThat(recalls).isNotEmpty

    // Count total sentences across all recalls
    val totalSentences = recalls.flatMap { it.sentences ?: emptyList() }.size
    assertThat(totalSentences).isEqualTo(1)
      .withFailMessage("Expected only 1 active sentence but found $totalSentences. Deleted sentences are being returned.")
  }
}
