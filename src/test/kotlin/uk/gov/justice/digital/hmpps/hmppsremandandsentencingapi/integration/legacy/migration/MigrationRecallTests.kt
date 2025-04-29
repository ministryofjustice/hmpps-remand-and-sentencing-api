package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.migration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.Recall
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.util.DataCreator
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.RecallType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.MigrationCreateCourtCasesResponse

class MigrationRecallTests : IntegrationTestBase() {

  @Test
  fun `can create sentences and associated recall entities`() {
    val firstSentence = DataCreator.migrationCreateSentence(
      legacyData = DataCreator.sentenceLegacyData(sentenceCalcType = "FTR_ORA", sentenceCategory = "2020"),
    )
    val secondSentence = DataCreator.migrationCreateSentence(
      legacyData = DataCreator.sentenceLegacyData(sentenceCalcType = "FTR_ORA", sentenceCategory = "2020"),
    )
    val firstCharge = DataCreator.migrationCreateCharge(sentence = firstSentence)
    val secondCharge = DataCreator.migrationCreateCharge(sentence = secondSentence)
    val appearance = DataCreator.migrationCreateCourtAppearance(charges = listOf(firstCharge, secondCharge))
    val migrationCourtCase = DataCreator.migrationCreateCourtCase(appearances = listOf(appearance))
    val migrationCourtCases = DataCreator.migrationCreateCourtCases(courtCases = listOf(migrationCourtCase))
    val response = webTestClient
      .post()
      .uri("/legacy/court-case/migration")
      .bodyValue(migrationCourtCases)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_COURT_CASE_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isCreated
      .returnResult(MigrationCreateCourtCasesResponse::class.java)
      .responseBody.blockFirst()!!

    val recalls = getRecallsByPrisonerId(migrationCourtCases.prisonerId)
    assertThat(recalls).hasSize(2)
    assertThat(recalls[0].recallType).isEqualTo(RecallType.FTR_28)
    assertThat(recalls[0].sentences).hasSize(1)
    assertThat(recalls[1].recallType).isEqualTo(RecallType.FTR_28)
    assertThat(recalls[1].sentences).hasSize(1)
  }

  private fun getRecallsByPrisonerId(prisonerId: String): List<Recall> = webTestClient
    .get()
    .uri("/recall/person/$prisonerId")
    .headers {
      it.authToken(roles = listOf("ROLE_REMAND_SENTENCING__RECORD_RECALL_RW"))
    }
    .exchange()
    .expectStatus()
    .isOk
    .expectBodyList(Recall::class.java)
    .returnResult().responseBody!!
}
