package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.sentence

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.unconfirmedsentencefix.ConfirmConsecutiveToSentenceFixes
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.util.DataCreator
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.MigrationCreateCourtCasesResponse

class ConfirmConsecutiveToSentenceFixTests : IntegrationTestBase() {

  @Test
  fun `confirm consecutive to sentence fixes`() {
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
    val firstCharge = DataCreator.migrationCreateCharge(chargeNOMISId = 11, sentence = firstSentence)
    val secondCharge = DataCreator.migrationCreateCharge(chargeNOMISId = 33, sentence = firstSentence)
    val consecutiveToCharge = DataCreator.migrationCreateCharge(chargeNOMISId = 22, sentence = consecutiveToSentence)
    val appearance = DataCreator.migrationCreateCourtAppearance(charges = listOf(consecutiveToCharge, firstCharge, secondCharge))
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
    val courtCaseUuid = response.courtCases.first().courtCaseUuid
    webTestClient
      .get()
      .uri("/court-case/$courtCaseUuid")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
      }
      .exchange()
      .expectStatus()
      .isOk

    val sentenceUuid = response.sentences.first { it.sentenceNOMISId == firstSentenceId }.sentenceUuid
    val sentenceEntityBefore = sentenceRepository.findFirstBySentenceUuidAndStatusIdNotOrderByUpdatedAtDesc(sentenceUuid)!!
    Assertions.assertTrue(sentenceEntityBefore.hasUnconfirmedConsecutiveToFix!!)
    val confirmConsecutiveToSentenceFixes = ConfirmConsecutiveToSentenceFixes(listOf(sentenceUuid))
    webTestClient.put()
      .uri("/sentence/confirm-consecutive-to-sentence-fix")
      .bodyValue(confirmConsecutiveToSentenceFixes)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
      }
      .exchange()
      .expectStatus()
      .isNoContent
    val sentenceEntityAfter = sentenceRepository.findFirstBySentenceUuidAndStatusIdNotOrderByUpdatedAtDesc(sentenceUuid)!!
    Assertions.assertNull(sentenceEntityAfter.hasUnconfirmedConsecutiveToFix)
  }
}
