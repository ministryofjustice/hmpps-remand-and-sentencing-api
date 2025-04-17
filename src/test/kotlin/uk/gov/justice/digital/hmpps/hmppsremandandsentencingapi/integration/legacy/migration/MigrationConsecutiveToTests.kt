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
    val firstSentenceId = DataCreator.migrationSentenceId(1, 1)
    val firstPeriodLengthId = DataCreator.nomisPeriodLengthId(firstSentenceId.offenderBookingId, firstSentenceId.sequence, 1)
    val firstSentence = DataCreator.migrationCreateSentence(
      sentenceId = firstSentenceId,
      legacyData = DataCreator.sentenceLegacyData(sentenceCalcType = "FTR_ORA"),
      periodLengths = listOf(
        DataCreator.migrationCreatePeriodLength(periodLengthId = firstPeriodLengthId),
      ),
    )

    val consecutiveToSentenceId = DataCreator.migrationSentenceId(1, 5)
    val consecutiveToPeriodLengthId = DataCreator.nomisPeriodLengthId(consecutiveToSentenceId.offenderBookingId, consecutiveToSentenceId.sequence, 1)
    val consecutiveToSentence = DataCreator.migrationCreateSentence(
      sentenceId = consecutiveToSentenceId,
      consecutiveToSentenceId = firstSentence.sentenceId,
      periodLengths = listOf(
        DataCreator.migrationCreatePeriodLength(periodLengthId = consecutiveToPeriodLengthId),
      ),
    )
    val charge = DataCreator.migrationCreateCharge(chargeNOMISId = 11, sentence = firstSentence)
    val consecutiveToCharge = DataCreator.migrationCreateCharge(chargeNOMISId = 22, sentence = consecutiveToSentence)
    val appearance = DataCreator.migrationCreateCourtAppearance(charges = listOf(consecutiveToCharge, charge))
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

  @Test
  fun `can still process a sentence where the consecutive to sentence is non existent (A NOMIS data issue)`() {
    val sentenceWithNonExistentConsecutiveTo = DataCreator.migrationCreateSentence(sentenceId = DataCreator.migrationSentenceId(1, 1), consecutiveToSentenceId = DataCreator.migrationSentenceId(66, 99), legacyData = DataCreator.sentenceLegacyData(sentenceCalcType = "FTR_ORA"))
    val charge = DataCreator.migrationCreateCharge(chargeNOMISId = 11, sentence = sentenceWithNonExistentConsecutiveTo)
    val appearance = DataCreator.migrationCreateCourtAppearance(charges = listOf(charge))
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
    val sentenceUuid = response.sentences.first { sentenceResponse -> sentenceResponse.sentenceNOMISId == sentenceWithNonExistentConsecutiveTo.sentenceId }.sentenceUuid
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
      .jsonPath("$.appearances[*].charges[*].sentence[?(@.sentenceUuid == '$sentenceUuid')].sentenceType.sentenceTypeUuid")
      .isEqualTo(LegacySentenceService.Companion.recallSentenceTypeBucketUuid.toString())
  }
}
