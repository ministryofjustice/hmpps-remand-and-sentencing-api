package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.migration

import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.util.DataCreator
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.MigrationCreateCourtCasesResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.service.LegacySentenceService

class MigrationConsecutiveToTests : IntegrationTestBase() {

  @Test
  fun `can create sentence when consecutive to another in the same court case`() {
    val firstSentence = DataCreator.Factory.migrationCreateSentence(sentenceId = DataCreator.Factory.migrationSentenceId(1, 1), legacyData = DataCreator.Factory.sentenceLegacyData(sentenceCalcType = "FTR_ORA"))
    val consecutiveToSentence = DataCreator.Factory.migrationCreateSentence(sentenceId = DataCreator.Factory.migrationSentenceId(1, 5), consecutiveToSentenceId = firstSentence.sentenceId)
    val charge = DataCreator.Factory.migrationCreateCharge(chargeNOMISId = 11, sentence = firstSentence)
    val consecutiveToCharge = DataCreator.Factory.migrationCreateCharge(chargeNOMISId = 22, sentence = consecutiveToSentence)
    val appearance = DataCreator.Factory.migrationCreateCourtAppearance(charges = listOf(consecutiveToCharge, charge))
    val migrationCourtCase = DataCreator.Factory.migrationCreateCourtCase(appearances = listOf(appearance))
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

    val consecutiveToSentenceUuid = response.sentences.first { sentenceResponse -> sentenceResponse.sentenceNOMISId == consecutiveToSentence.sentenceId }.sentenceUuid
    val firstSentenceUuid = response.sentences.first { sentenceResponse -> sentenceResponse.sentenceNOMISId == firstSentence.sentenceId }.sentenceUuid
    webTestClient
      .get()
      .uri("/court-case/${response.courtCases.first().courtCaseUuid}")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING"))
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.appearances[*].charges[*].sentence[?(@.sentenceUuid == '$consecutiveToSentenceUuid')].consecutiveToChargeNumber")
      .isEqualTo(firstSentence.chargeNumber!!)
      .jsonPath("$.appearances[*].charges[*].sentence[?(@.sentenceUuid == '$firstSentenceUuid')].sentenceType.sentenceTypeUuid")
      .isEqualTo(LegacySentenceService.Companion.recallSentenceTypeBucketUuid.toString())
  }
}
