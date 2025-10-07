package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.courtappearance

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.util.DataCreator
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacySentenceCreatedResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.DpsDataCreator
import java.time.LocalDate
import kotlin.collections.map

class DeleteCourtAppearanceTests : IntegrationTestBase() {
  @Test
  fun `delete court appearance should change court appearance status to be deleted and court case as well if no more court appearance is ACTIVE`() {
    // Given a court appearance exists
    val appearance = DpsDataCreator.dpsCreateCourtAppearance()
    val courtCase = createCourtCase(DpsDataCreator.dpsCreateCourtCase(appearances = listOf(appearance)))
    val createdAppearance = courtCase.second.appearances.first()

    // When the court appearance is deleted
    webTestClient.delete()
      .uri("/court-appearance/${createdAppearance.appearanceUuid}")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isNoContent
      .expectBody()

    val deletedAppearance = courtAppearanceRepository.findByAppearanceUuid(createdAppearance.appearanceUuid)!!
    assertEquals(EntityStatus.DELETED, deletedAppearance.statusId)

    val deletedCourtCase = courtCaseRepository.findByCaseUniqueIdentifier(courtCase.first)
    assertEquals(EntityStatus.DELETED, deletedCourtCase?.statusId)
  }

  @Test
  fun `court case status should still be active if any court appearance is active`() {
    // Given a court appearance exists
    val appearance1 = DpsDataCreator.dpsCreateCourtAppearance()
    val appearance2 = DpsDataCreator.dpsCreateCourtAppearance()
    val courtCase = createCourtCase(DpsDataCreator.dpsCreateCourtCase(appearances = listOf(appearance1, appearance2)))
    val createdAppearance = courtCase.second.appearances.first()

    // When the court appearance is deleted
    webTestClient.delete()
      .uri("/court-appearance/${createdAppearance.appearanceUuid}")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isNoContent
      .expectBody()

    // Then the court case status should still be active
    val deletedAppearance = courtAppearanceRepository.findByAppearanceUuid(createdAppearance.appearanceUuid)!!
    assertEquals(EntityStatus.DELETED, deletedAppearance.statusId)

    val deletedCourtCase = courtCaseRepository.findByCaseUniqueIdentifier(courtCase.first)
    assertEquals(EntityStatus.ACTIVE, deletedCourtCase?.statusId)
  }

  @Test
  fun `must delete inactive sentences`() {
    val (chargeLifetimeUuid, toCreateCharge) = createLegacyCharge()
    val legacySentence = DataCreator.legacyCreateSentence(chargeUuids = listOf(chargeLifetimeUuid), appearanceUuid = toCreateCharge.appearanceLifetimeUuid, active = false)
    val response = webTestClient
      .post()
      .uri("/legacy/sentence")
      .bodyValue(legacySentence)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_SENTENCE_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isCreated.returnResult(LegacySentenceCreatedResponse::class.java)
      .responseBody.blockFirst()!!

    webTestClient.delete()
      .uri("/court-appearance/${response.appearanceUuid}")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isNoContent
      .expectBody()

    val deletedSentence = sentenceRepository.findBySentenceUuid(response.lifetimeUuid)[0]
    assertEquals(EntityStatus.DELETED, deletedSentence.statusId)
  }

  @Test
  fun `deletes recalls for legacy recall sentences`() {
    val (chargeLifetimeUuid, toCreateCharge) = createLegacyCharge()
    val legacySentence = DataCreator.legacyCreateSentence(
      chargeUuids = listOf(chargeLifetimeUuid),
      appearanceUuid = toCreateCharge.appearanceLifetimeUuid,
      sentenceLegacyData = DataCreator.sentenceLegacyData(sentenceCalcType = "FTR_ORA", sentenceCategory = "2020"),
      returnToCustodyDate = LocalDate.of(2023, 1, 1),
    )
    val response = webTestClient
      .post()
      .uri("/legacy/sentence")
      .bodyValue(legacySentence)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_SENTENCE_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isCreated.returnResult(LegacySentenceCreatedResponse::class.java)
      .responseBody.blockFirst()!!

    val recalls = getRecallsByPrisonerId(response.prisonerId)
    assertThat(recalls).hasSize(1)

    webTestClient.delete()
      .uri("/court-appearance/${response.appearanceUuid}")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isNoContent
      .expectBody()

    assertThat(getRecallsByPrisonerId(response.prisonerId)).isEmpty()

    val recall = recalls.first()
    val historicalRecalls = recallHistoryRepository.findByRecallUuid(recall.recallUuid)
    assertThat(historicalRecalls).hasSize(1)
    assertThat(historicalRecalls[0].historyStatusId).isEqualTo(EntityStatus.DELETED)
    assertThat(historicalRecalls[0].historyCreatedAt).isNotNull()

    val historicalRecallSentences = recallSentenceHistoryRepository.findByRecallHistoryId(historicalRecalls[0].id)
    assertThat(historicalRecallSentences!!).hasSize(1)
    assertThat(historicalRecallSentences.map { it.sentence.sentenceUuid }).containsExactlyInAnyOrder(response.lifetimeUuid)
  }
}
