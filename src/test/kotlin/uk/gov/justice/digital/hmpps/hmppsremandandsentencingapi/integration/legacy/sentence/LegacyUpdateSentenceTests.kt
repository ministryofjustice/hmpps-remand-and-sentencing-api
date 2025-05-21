package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.sentence

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.util.DataCreator
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.RecallType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.PeriodLengthRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.SentenceRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.audit.SentenceHistoryRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacyCourtAppearance
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacyCreateSentence
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacySentence
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.SentenceLegacyData
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.DpsDataCreator
import java.time.LocalDate
import java.util.UUID

class LegacyUpdateSentenceTests : IntegrationTestBase() {

  @Autowired
  private lateinit var periodLengthRepository: PeriodLengthRepository

  @Autowired
  private lateinit var sentenceRepository: SentenceRepository

  @Autowired
  private lateinit var sentenceHistoryRepository: SentenceHistoryRepository

  @Test
  fun `update sentence for existing charge`() {
    val (sentenceLifetimeUuid, createdSentence) = createLegacySentence()
    val toUpdate = createdSentence.copy(chargeNumber = "6")
    webTestClient
      .put()
      .uri("/legacy/sentence/$sentenceLifetimeUuid")
      .bodyValue(toUpdate)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_SENTENCE_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isNoContent
    val message = getMessages(1)[0]
    assertThat(message.eventType).isEqualTo("sentence.updated")
    assertThat(message.additionalInformation.get("source").asText()).isEqualTo("NOMIS")
    val historyRecords = sentenceHistoryRepository.findAll().filter { it.sentenceUuid == sentenceLifetimeUuid }
    assertThat(historyRecords).extracting<String> { it.chargeNumber!! }.containsExactlyInAnyOrder(createdSentence.chargeNumber, toUpdate.chargeNumber)
  }

  @Test
  fun `update sentence for a recall sentence`() {
    // Create sentence with associated recall data.
    val (lifetimeUuid, createdSentence) = createLegacySentence(
      legacySentence = DataCreator.legacyCreateSentence(sentenceLegacyData = DataCreator.sentenceLegacyData(sentenceCalcType = "FTR_ORA", sentenceCategory = "2020"), returnToCustodyDate = LocalDate.of(2023, 1, 1)),
    )
    val toUpdate = createdSentence.copy(returnToCustodyDate = LocalDate.of(2024, 1, 1))
    webTestClient
      .put()
      .uri("/legacy/sentence/$lifetimeUuid")
      .bodyValue(toUpdate)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_SENTENCE_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isNoContent

    val recalls = getRecallsByPrisonerId(DpsDataCreator.DEFAULT_PRISONER_ID)
    assertThat(recalls).hasSize(1)
    assertThat(recalls[0].recallType).isEqualTo(RecallType.FTR_28)
    assertThat(recalls[0].sentences).hasSize(1)
    assertThat(recalls[0].returnToCustodyDate).isEqualTo(LocalDate.of(2024, 1, 1))
  }

  @Test
  fun `must not update sentence when no sentence exists`() {
    val toUpdate = DataCreator.legacyCreateSentence()
    webTestClient
      .put()
      .uri("/legacy/sentence/${UUID.randomUUID()}")
      .bodyValue(toUpdate)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_SENTENCE_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isNotFound
  }

  @Test
  fun `no token results in unauthorized`() {
    val toUpdate = DataCreator.legacyCreateSentence()
    webTestClient
      .put()
      .uri("/legacy/sentence/${UUID.randomUUID()}")
      .bodyValue(toUpdate)
      .headers {
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `token with incorrect role is forbidden`() {
    val toUpdate = DataCreator.legacyCreateSentence()
    webTestClient
      .put()
      .uri("/legacy/sentence/${UUID.randomUUID()}")
      .bodyValue(toUpdate)
      .headers {
        it.authToken(roles = listOf("ROLE_OTHER_FUNCTION"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  @Sql(
    "classpath:test_data/reset-database.sql",
    "classpath:test_data/single-sentence-case-with-appearance.sql",
  )
  fun `Update sentence - add a charge to an existing sentence (so a many-charges situation), ensure period-lengths are copied to new charge and sentence combination`() {
    val sentenceUuid = "8ba46b2d-ee44-48fe-a57f-ac51004e516e"
    val appearanceUuid = "4a5d8632-dd77-4fc8-8341-ec5fde0475fc"
    val sentence = getSentence(sentenceUuid)
    val appearance = getAppearance(appearanceUuid)

    val sentencesBefore = sentenceRepository.findBySentenceUuid(UUID.fromString(sentenceUuid))
    assertThat(sentencesBefore).hasSize(1)
    assertThat(sentencesBefore[0].charge.offenceCode).isEqualTo("COML020")
    val periodLengthsBefore = periodLengthRepository.findAllBySentenceEntitySentenceUuidAndStatusIdNot(UUID.fromString(sentenceUuid))
    assertThat(periodLengthsBefore).hasSize(1)
    assertThat(periodLengthsBefore[0].days).isEqualTo(99)

    val updatedSentence = LegacyCreateSentence(
      chargeUuids = appearance?.charges!!.map { it.lifetimeUuid },
      chargeNumber = sentence?.chargeNumber,
      active = sentence?.active!!,
      returnToCustodyDate = sentence.returnToCustodyDate,
      legacyData = SentenceLegacyData(
        postedDate = "2021-05-13",
      ),
    )

    updateSentence(sentenceUuid, updatedSentence)

    val sentencesAfter = sentenceRepository.findBySentenceUuid(UUID.fromString(sentenceUuid))
    assertThat(sentencesAfter).hasSize(2)
    assertThat(sentencesAfter.map { it.charge.offenceCode }).containsExactlyInAnyOrder("COML020", "SX03163A")
    val periodLengthsAfter = periodLengthRepository.findAllBySentenceEntitySentenceUuidAndStatusIdNot(UUID.fromString(sentenceUuid))
    assertThat(periodLengthsAfter).hasSize(2)
    assertThat(periodLengthsAfter.map { it.days }).containsExactly(99, 99)
    assertThat(periodLengthsAfter.map { it.sentenceEntity?.sentenceUuid.toString() }).containsExactly(sentenceUuid, sentenceUuid)
  }

  private fun updateSentence(
    sentenceUuid: String,
    updatedSentence: LegacyCreateSentence,
  ) {
    webTestClient
      .put()
      .uri("/legacy/sentence/$sentenceUuid")
      .bodyValue(updatedSentence)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_SENTENCE_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isNoContent
  }

  private fun getAppearance(appearanceUuid: String): LegacyCourtAppearance? = webTestClient
    .get()
    .uri("/legacy/court-appearance/$appearanceUuid")
    .headers { it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_APPEARANCE_RO")) }
    .exchange()
    .expectStatus()
    .isOk
    .expectBody(LegacyCourtAppearance::class.java)
    .returnResult().responseBody

  private fun getSentence(sentenceUuid: String): LegacySentence? = webTestClient
    .get()
    .uri("/legacy/sentence/$sentenceUuid")
    .headers { it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_SENTENCE_RO")) }
    .exchange()
    .expectStatus()
    .isOk
    .expectBody(LegacySentence::class.java)
    .returnResult().responseBody
}
