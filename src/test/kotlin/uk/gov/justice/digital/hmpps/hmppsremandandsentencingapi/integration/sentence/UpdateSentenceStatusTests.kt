package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.sentence

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.UpdateSentenceStatusRequest
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ChangeSource
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.SentenceEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.audit.SentenceHistoryRepository
import java.util.UUID

class UpdateSentenceStatusTests : IntegrationTestBase() {

  @Autowired
  private lateinit var sentenceHistoryRepository: SentenceHistoryRepository

  @Test
  fun `updates the status and reason of a sentence`() {
    val createdSentence = createCourtCase().second.appearances.first().charges.first().sentence!!

    webTestClient
      .patch()
      .uri("/sentence/status")
      .headers { it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI")) }
      .bodyValue(
        UpdateSentenceStatusRequest(
          sentenceUuids = listOf(createdSentence.sentenceUuid!!),
          status = SentenceEntityStatus.INACTIVE,
          reason = "Sentence was recorded in error",
        ),
      )
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.updatedSentenceUuids[0]")
      .isEqualTo(createdSentence.sentenceUuid.toString())

    val updatedSentence = sentenceRepository.findBySentenceUuidInAndStatusIdNot(listOf(createdSentence.sentenceUuid!!)).first()
    assertThat(updatedSentence.statusId).isEqualTo(SentenceEntityStatus.INACTIVE)
    assertThat(updatedSentence.reason).isEqualTo("Sentence was recorded in error")

    val historyRecords = sentenceHistoryRepository.findAll().filter { it.sentenceUuid == createdSentence.sentenceUuid }
    assertThat(historyRecords).anyMatch { it.statusId == SentenceEntityStatus.INACTIVE && it.reason == "Sentence was recorded in error" && it.changeSource == ChangeSource.DPS }
  }

  @Test
  fun `updates the status of multiple sentences with a null reason`() {
    val (sentenceOne, sentenceTwo) = createCourtCaseTwoSentences()
    val sentenceUuids = listOf(sentenceOne.sentenceUuid!!, sentenceTwo.sentenceUuid!!)

    webTestClient
      .patch()
      .uri("/sentence/status")
      .headers { it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI")) }
      .bodyValue(
        UpdateSentenceStatusRequest(
          sentenceUuids = sentenceUuids,
          status = SentenceEntityStatus.INACTIVE,
          reason = null,
        ),
      )
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.updatedSentenceUuids.length()")
      .isEqualTo(sentenceUuids.size)

    val updatedSentences = sentenceRepository.findBySentenceUuidInAndStatusIdNot(sentenceUuids, SentenceEntityStatus.DELETED)
    assertThat(updatedSentences).allMatch { it.statusId == SentenceEntityStatus.INACTIVE && it.reason == null }
  }

  @Test
  fun `no token results in unauthorized`() {
    webTestClient
      .patch()
      .uri("/sentence/status")
      .bodyValue(
        UpdateSentenceStatusRequest(
          sentenceUuids = listOf(UUID.randomUUID()),
          status = SentenceEntityStatus.INACTIVE,
          reason = null,
        ),
      )
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `token with incorrect role is forbidden`() {
    webTestClient
      .patch()
      .uri("/sentence/status")
      .headers { it.authToken(roles = listOf("ROLE_OTHER_FUNCTION")) }
      .bodyValue(
        UpdateSentenceStatusRequest(
          sentenceUuids = listOf(UUID.randomUUID()),
          status = SentenceEntityStatus.INACTIVE,
          reason = null,
        ),
      )
      .exchange()
      .expectStatus()
      .isForbidden
  }
}
