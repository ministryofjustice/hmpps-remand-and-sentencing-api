package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.migration

import org.junit.jupiter.api.Assertions
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
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.appearances[*].charges[*].sentence[?(@.sentenceUuid == '$consecutiveToSentenceUuid')].consecutiveToSentenceUuid")
      .isEqualTo(firstSentenceUuid.toString())
      .jsonPath("$.appearances[*].charges[*].sentence[?(@.sentenceUuid == '$consecutiveToSentenceUuid')].sentenceServeType")
      .isEqualTo("CONSECUTIVE")
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
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.appearances[*].charges[*].sentence[?(@.sentenceUuid == '$sentenceUuid')].sentenceType.sentenceTypeUuid")
      .isEqualTo(LegacySentenceService.Companion.recallSentenceTypeBucketUuid.toString())
  }

  @Test
  fun `mark consecutive to court cases when many charges fix`() {
    val otherCaseSentenceId = DataCreator.migrationSentenceId(1, 2)
    val otherCasePeriodLengthId = DataCreator.nomisPeriodLengthId(otherCaseSentenceId.offenderBookingId, otherCaseSentenceId.sequence, 1)
    val otherSentence = DataCreator.migrationCreateSentence(
      sentenceId = otherCaseSentenceId,
      periodLengths = listOf(
        DataCreator.migrationCreatePeriodLength(periodLengthId = otherCasePeriodLengthId),
      ),
    )
    val otherFirstCharge = DataCreator.migrationCreateCharge(chargeNOMISId = 44, sentence = otherSentence)
    val otherSecondCharge = DataCreator.migrationCreateCharge(chargeNOMISId = 33, sentence = otherSentence)
    val otherAppearance = DataCreator.migrationCreateCourtAppearance(charges = listOf(otherFirstCharge, otherSecondCharge), eventId = 1)
    val otherCase = DataCreator.migrationCreateCourtCase(appearances = listOf(otherAppearance), caseId = 1)
    val firstSentenceId = DataCreator.migrationSentenceId(1, 1)
    val firstPeriodLengthId = DataCreator.nomisPeriodLengthId(firstSentenceId.offenderBookingId, firstSentenceId.sequence, 1)
    val firstSentence = DataCreator.migrationCreateSentence(
      sentenceId = firstSentenceId,
      legacyData = DataCreator.sentenceLegacyData(sentenceCalcType = "FTR_ORA"),
      consecutiveToSentenceId = otherCaseSentenceId,
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
    val firstCharge = DataCreator.migrationCreateCharge(chargeNOMISId = 11, sentence = firstSentence)
    val secondCharge = DataCreator.migrationCreateCharge(chargeNOMISId = 33, sentence = firstSentence)
    val consecutiveToCharge = DataCreator.migrationCreateCharge(chargeNOMISId = 22, sentence = consecutiveToSentence)
    val appearance = DataCreator.migrationCreateCourtAppearance(charges = listOf(consecutiveToCharge, firstCharge, secondCharge), eventId = 2)
    val migrationCourtCase = DataCreator.migrationCreateCourtCase(appearances = listOf(appearance), caseId = 2)
    val migrationCourtCases = DataCreator.migrationCreateCourtCases(courtCases = listOf(otherCase, migrationCourtCase))
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
    val migrationCourtCaseUuid = response.courtCases.first { it.caseId == migrationCourtCase.caseId }.courtCaseUuid
    val otherCourtCaseUuid = response.courtCases.first { it.caseId == otherCase.caseId }.courtCaseUuid
    webTestClient
      .get()
      .uri {
        it.path("/court-case/paged/search")
          .queryParam("prisonerId", migrationCourtCases.prisonerId)
          .build()
      }
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
      }
      .exchange()
      .expectStatus()
      .isOk
    val migrationCourtCaseEntity = courtCaseRepository.findByCaseUniqueIdentifier(migrationCourtCaseUuid)!!
    Assertions.assertTrue(migrationCourtCaseEntity.hasUnconfirmedConsecutiveToFix!!)

    val otherCourtCaseEntity = courtCaseRepository.findByCaseUniqueIdentifier(otherCourtCaseUuid)!!
    Assertions.assertTrue(otherCourtCaseEntity.hasUnconfirmedConsecutiveToFix!!)
  }
}
