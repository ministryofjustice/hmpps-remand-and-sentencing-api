package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.sentence

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.returnResult
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CourtCase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateCourtCase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.util.DataCreator
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.util.DataCreator.Factory.sentenceLegacyData
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.wiremock.AdjustmentsApiExtension.Companion.adjustmentsApi
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.RecallType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.audit.SentenceHistoryRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacyCreateSentence
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.DpsDataCreator
import java.time.LocalDate
import java.util.UUID
import java.util.concurrent.CompletableFuture

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

  @Test
  fun `race condition adding new charges to sentence at the same time`() {
    val sentence = DataCreator.migrationCreateSentence()
    val firstCharge = DataCreator.migrationCreateCharge(chargeNOMISId = 1, sentence = sentence)
    val secondCharge = DataCreator.migrationCreateCharge(chargeNOMISId = 2, sentence = sentence)
    val thirdCharge = DataCreator.migrationCreateCharge(chargeNOMISId = 3)
    val fourthCharge = DataCreator.migrationCreateCharge(chargeNOMISId = 4)
    val appearance = DataCreator.migrationCreateCourtAppearance(charges = listOf(firstCharge, secondCharge, thirdCharge, fourthCharge))
    val courtCase = DataCreator.migrationCreateCourtCase(appearances = listOf(appearance))
    val migrationResponse = migrateCases(DataCreator.migrationCreateCourtCases(courtCases = listOf(courtCase)))
    val firstChargeUuid = migrationResponse.charges.first { it.chargeNOMISId == firstCharge.chargeNOMISId }.chargeUuid
    val secondChargeUuid = migrationResponse.charges.first { it.chargeNOMISId == secondCharge.chargeNOMISId }.chargeUuid
    val thirdChargeUuid = migrationResponse.charges.first { it.chargeNOMISId == thirdCharge.chargeNOMISId }.chargeUuid
    val fourthChargeUuid = migrationResponse.charges.first { it.chargeNOMISId == fourthCharge.chargeNOMISId }.chargeUuid
    val sentenceUuid = migrationResponse.sentences.first().sentenceUuid
    val appearanceUuid = migrationResponse.appearances[0].appearanceUuid
    val legacyUpdate = DataCreator.legacyCreateSentence(
      chargeUuids = listOf(firstChargeUuid, secondChargeUuid, thirdChargeUuid, fourthChargeUuid),
      appearanceUuid = appearanceUuid,
    )

    val firstCall = CompletableFuture.supplyAsync {
      webTestClient.put()
        .uri("/legacy/sentence/$sentenceUuid")
        .bodyValue(legacyUpdate)
        .headers {
          it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_SENTENCE_RW"))
          it.contentType = MediaType.APPLICATION_JSON
        }.exchange()
        .expectStatus()
        .isNoContent
    }
    val secondCall = CompletableFuture.supplyAsync {
      webTestClient.put()
        .uri("/legacy/sentence/$sentenceUuid")
        .bodyValue(legacyUpdate)
        .headers {
          it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_SENTENCE_RW"))
          it.contentType = MediaType.APPLICATION_JSON
        }.exchange()
        .expectStatus()
        .isNoContent
    }
    firstCall.thenCombine<WebTestClient.ResponseSpec, Pair<WebTestClient.ResponseSpec, WebTestClient.ResponseSpec>>(secondCall) { a, b -> a to b }.join()
    val sentencesGroupedByStatus = sentenceRepository.findBySentenceUuid(sentenceUuid).groupBy { it.statusId to it.charge }
    assertThat(sentencesGroupedByStatus.values.map { it.size }).allMatch { it == 1 } // check there is only ever 1 sentence record for the status, charge combination, in this race condition before the fix there were two entries for the same status, charge combination which causes adverse side effects when retrieving the sentence for a given charge
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
