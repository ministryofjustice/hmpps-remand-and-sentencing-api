package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.courtcase

import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.util.DataCreator
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.MigrationCreateCourtCasesResponse
import java.time.format.DateTimeFormatter

class GetUnconfirmedSentenceFixCourtCasesByUuidsTests : IntegrationTestBase() {

  @Test
  fun `return unconfirmed sentence fix court cases by uuids`() {
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

    webTestClient.get()
      .uri {
        it.path("/court-case/multiple/unconfirmed-sentence-fix-court-cases")
          .queryParam("courtCaseUuids", courtCaseUuid)
          .build()
      }
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.courtCases[?(@.courtCaseUuid == '$courtCaseUuid')].caseReference")
      .isEqualTo(migrationCourtCase.courtCaseLegacyData.caseReferences.first().offenderCaseReference)
      .jsonPath("$.courtCases[?(@.courtCaseUuid == '$courtCaseUuid')].courtCode")
      .isEqualTo(appearance.courtCode)
      .jsonPath("$.courtCases[?(@.courtCaseUuid == '$courtCaseUuid')].sentencingHearingDate")
      .isEqualTo(appearance.appearanceDate.format(DateTimeFormatter.ISO_DATE))
  }
}
