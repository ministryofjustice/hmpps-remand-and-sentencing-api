package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.sentence

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CourtCase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateCourtCase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.util.DataCreator
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.util.DataCreator.Factory.sentenceLegacyData
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.wiremock.AdjustmentsApiExtension.Companion.adjustmentsApi
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.RecallEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.RecallType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.audit.SentenceHistoryRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacyCreateSentence
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.DpsDataCreator
import java.time.LocalDate
import java.util.UUID
import kotlin.collections.map

class LegacyUpdateSentenceTests : IntegrationTestBase() {

  @Autowired
  private lateinit var sentenceHistoryRepository: SentenceHistoryRepository

  @Test
  fun `update sentence for a recall sentence`() {
    // Create sentence with associated recall data.
    val (lifetimeUuid, createdSentence) = createLegacySentence(
      legacySentence = DataCreator.legacyCreateSentence(sentenceLegacyData = sentenceLegacyData(sentenceCalcType = "FTR_ORA", sentenceCategory = "2020"), returnToCustodyDate = LocalDate.of(2023, 1, 1)),
    )
    val toUpdate = createdSentence.copy(returnToCustodyDate = LocalDate.of(2024, 1, 1))
    putLegacySentence(lifetimeUuid, toUpdate)

    adjustmentsApi.stubGetAdjustmentsDefaultToNone()
    val recalls = getRecallsByPrisonerId(DpsDataCreator.DEFAULT_PRISONER_ID)
    assertThat(recalls).hasSize(1)
    assertThat(recalls[0].recallType).isEqualTo(RecallType.FTR_28)
    assertThat(recalls[0].courtCases[0].sentences).hasSize(1)
    assertThat(recalls[0].returnToCustodyDate).isEqualTo(LocalDate.of(2024, 1, 1))

    val historicalRecalls = recallHistoryRepository.findByRecallUuid(recalls[0].recallUuid)
    assertThat(historicalRecalls).hasSize(2)
  }

  @Test
  fun `update sentence sentence type cannot change`() {
    val (lifetimeUuid, createdSentence) = createLegacySentence(
      legacySentence = DataCreator.legacyCreateSentence(
        sentenceLegacyData = sentenceLegacyData(
          sentenceCalcType = "ADIMP_ORA",
          sentenceCategory = "2020",
          nomisLineReference = "67",
        ),
        returnToCustodyDate = LocalDate.of(2023, 1, 1),
      ),
    )
    val toUpdate = createdSentence.copy(
      legacyData = sentenceLegacyData(sentenceCalcType = "FTR_ORA", sentenceCategory = "2020", nomisLineReference = "67"),
    )
    putLegacySentence(lifetimeUuid, toUpdate)
    val message = getMessages(1)[0]
    assertThat(message.eventType).isEqualTo("sentence.updated")
    assertThat(message.additionalInformation.get("source").asText()).isEqualTo("NOMIS")
    val sentences = sentenceRepository.findBySentenceUuid(lifetimeUuid)
    assertThat(sentences).hasSize(1).extracting<String> { it.sentenceType?.nomisSentenceCalcType!! }.containsExactlyInAnyOrder("ADIMP_ORA")
    val historyRecords = sentenceHistoryRepository.findAll().filter { it.sentenceUuid == lifetimeUuid }
    assertThat(historyRecords).extracting<String> { it.legacyData?.nomisLineReference }.containsExactly("67")
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
  fun `Updating legacy data for a FORTHWITH sentence preserves the FORTHWITH of the sentence (if its not consecutive)`() {
    val createCourtCase: Pair<String, CreateCourtCase> = createCourtCase()
    val caseUuid = createCourtCase.first
    createCourtCase.second.appearances

    val courtCase = getCourtCase(caseUuid)
    val sentenceUuid = courtCase.appearances[0].charges[0].sentence?.sentenceUuid!!
    assertThat(courtCase.appearances[0].charges[0].sentence?.sentenceServeType).isEqualTo("FORTHWITH")

    val legacyUpdate = LegacyCreateSentence(
      chargeUuids = listOf(courtCase.appearances[0].charges[0].chargeUuid),
      appearanceUuid = courtCase.appearances[0].appearanceUuid,
      active = true,
      legacyData = sentenceLegacyData(),
      consecutiveToLifetimeUuid = null, // Not consecutive to anything so should preserve the FORTHWITH status
      performedByUser = null,
    )

    putLegacySentence(sentenceUuid, legacyUpdate)

    val courtCaseAfterLegacyUpdate = getCourtCase(caseUuid)
    assertThat(courtCaseAfterLegacyUpdate.appearances[0].charges[0].sentence?.sentenceServeType).isEqualTo("FORTHWITH")
  }

  private fun putLegacySentence(
    sentenceUuid: UUID,
    legacyUpdate: LegacyCreateSentence,
  ) {
    webTestClient
      .put()
      .uri("/legacy/sentence/$sentenceUuid")
      .bodyValue(legacyUpdate)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_SENTENCE_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isNoContent
  }

  private fun getCourtCase(caseUuid: String): CourtCase = webTestClient
    .get()
    .uri("/court-case/$caseUuid")
    .headers {
      it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
    }
    .exchange()
    .expectStatus()
    .isOk
    .returnResult(CourtCase::class.java)
    .responseBody.blockFirst()!!
}
