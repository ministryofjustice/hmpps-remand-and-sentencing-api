package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.migration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.util.DataCreator
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.wiremock.AdjustmentsApiExtension.Companion.adjustmentsApi
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.RecallType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.MigrationCreateCourtCasesResponse
import java.time.LocalDate

class MigrationRecallTests : IntegrationTestBase() {

  @Test
  fun `can create sentences and associated recall entities`() {
    val firstSentence = DataCreator.migrationCreateSentence(
      sentenceId = DataCreator.migrationSentenceId(sequence = 1),
      legacyData = DataCreator.sentenceLegacyData(sentenceCalcType = "FTR_ORA", sentenceCategory = "2020"),
      returnToCustodyDate = LocalDate.of(2024, 1, 1),
    )
    val secondSentence = DataCreator.migrationCreateSentence(
      sentenceId = DataCreator.migrationSentenceId(sequence = 2),
      legacyData = DataCreator.sentenceLegacyData(sentenceCalcType = "FTR_ORA", sentenceCategory = "2020"),
      returnToCustodyDate = LocalDate.of(2024, 1, 1),
    )
    val firstCharge = DataCreator.migrationCreateCharge(chargeNOMISId = 5453, sentence = firstSentence)
    val secondCharge = DataCreator.migrationCreateCharge(chargeNOMISId = 5454, sentence = secondSentence)
    val appearance = DataCreator.migrationCreateCourtAppearance(charges = listOf(firstCharge, secondCharge))
    val migrationCourtCase = DataCreator.migrationCreateCourtCase(appearances = listOf(appearance))
    val migrationCourtCases = DataCreator.migrationCreateCourtCases(courtCases = listOf(migrationCourtCase))
    webTestClient
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

    adjustmentsApi.stubGetAdjustmentsDefaultToNone()
    val response = getPrisonerRecallsResponse(migrationCourtCases.prisonerId)
    // NOMIS recalls with the same type and arrest date are grouped into one recall
    assertThat(response.recalls).hasSize(1)
    assertThat(response.prisonerRecallTotal).isEqualTo(1)
    assertThat(response.recalls[0].recallType).isEqualTo(RecallType.FTR_28)
    assertThat(response.recalls[0].courtCases[0].sentences).hasSize(2)
    assertThat(response.recalls[0].returnToCustodyDate).isEqualTo(LocalDate.of(2024, 1, 1))
  }

  @Test
  fun `return to custody date not set for non FTR sentences`() {
    val firstSentence = DataCreator.migrationCreateSentence(
      sentenceId = DataCreator.migrationSentenceId(sequence = 1),
      legacyData = DataCreator.sentenceLegacyData(sentenceCalcType = "LR", sentenceCategory = "2020"),
      returnToCustodyDate = LocalDate.of(2024, 1, 1),
    )
    val secondSentence = DataCreator.migrationCreateSentence(
      sentenceId = DataCreator.migrationSentenceId(sequence = 2),
      legacyData = DataCreator.sentenceLegacyData(sentenceCalcType = "LR", sentenceCategory = "2020"),
      returnToCustodyDate = LocalDate.of(2024, 1, 1),
    )
    val firstCharge = DataCreator.migrationCreateCharge(chargeNOMISId = 5453, sentence = firstSentence)
    val secondCharge = DataCreator.migrationCreateCharge(chargeNOMISId = 5454, sentence = secondSentence)
    val appearance = DataCreator.migrationCreateCourtAppearance(charges = listOf(firstCharge, secondCharge))
    val migrationCourtCase = DataCreator.migrationCreateCourtCase(appearances = listOf(appearance))
    val migrationCourtCases = DataCreator.migrationCreateCourtCases(courtCases = listOf(migrationCourtCase))
    webTestClient
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

    adjustmentsApi.stubGetAdjustmentsDefaultToNone()
    val response = getPrisonerRecallsResponse(migrationCourtCases.prisonerId)
    // NOMIS recalls get grouped
    assertThat(response.recalls).hasSize(1)
    assertThat(response.prisonerRecallTotal).isEqualTo(1)
    assertThat(response.recalls[0].recallType).isEqualTo(RecallType.LR)
    assertThat(response.recalls[0].courtCases[0].sentences).hasSize(2)
    assertThat(response.recalls[0].returnToCustodyDate).isNull()
  }
}
