package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.sync

import org.hamcrest.Matchers.everyItem
import org.hamcrest.core.IsNull
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.util.DataCreator
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.MigrationCreateCourtCasesResponse
import java.time.LocalDate

class MoveAppearanceSentenceTests : IntegrationTestBase() {

  @Test
  fun `can move sentence to another appearance`() {
    val migratedSentence = DataCreator.migrationCreateSentence(fine = null)
    val migratedCharge = DataCreator.migrationCreateCharge(legacyData = DataCreator.chargeLegacyData(nomisOutcomeCode = "1002", outcomeDescription = "Imprisonment"), sentence = migratedSentence)
    val migratedCourtAppearance = DataCreator.migrationCreateCourtAppearance(charges = listOf(migratedCharge), legacyData = DataCreator.courtAppearanceLegacyData(nomisOutcomeCode = "1002", outcomeDescription = "Imprisonment"))
    val otherCourtAppearance = DataCreator.migrationCreateCourtAppearance(eventId = 5, appearanceDate = LocalDate.now().minusDays(10), legacyData = DataCreator.courtAppearanceLegacyData(nomisOutcomeCode = "1501", outcomeConvictionFlag = true, outcomeDispositionCode = "F"), charges = listOf(migratedCharge.copy(sentence = null, legacyData = DataCreator.chargeLegacyData())))
    val migratedCourtCase = DataCreator.migrationCreateCourtCase(appearances = listOf(migratedCourtAppearance, otherCourtAppearance))
    val migrationCourtCases = DataCreator.migrationCreateSentenceCourtCases(courtCases = listOf(migratedCourtCase))
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
    val courtCaseResponse = response.courtCases.first()
    val createdCharge = response.charges.first()
    val createdAppearance = response.appearances.first { it.eventId == migratedCourtAppearance.eventId }
    val otherCreatedCourtAppearance = response.appearances.first { it.eventId == otherCourtAppearance.eventId }
    val createdSentence = response.sentences.first()
    val createdPeriodLength = response.sentenceTerms.first()

    val updateSentenceToOtherAppearance = DataCreator.legacyCreateSentence(
      chargeUuids = listOf(createdCharge.chargeUuid),
      appearanceUuid = otherCreatedCourtAppearance.appearanceUuid,
      fine = null,
      sentenceLegacyData = migratedSentence.legacyData,
    )

    webTestClient
      .put()
      .uri("/legacy/sentence/${createdSentence.sentenceUuid}")
      .bodyValue(updateSentenceToOtherAppearance)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_SENTENCE_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isNoContent

    webTestClient
      .get()
      .uri("/legacy/court-case/${courtCaseResponse.courtCaseUuid}/reconciliation")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_COURT_CASE_RO"))
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.appearances[?(@.appearanceUuid == '${createdAppearance.appearanceUuid}')].charges[?(@.chargeUuid == '${createdCharge.chargeUuid}')].sentence")
      .value(everyItem(IsNull.nullValue()))
      .jsonPath("$.appearances[?(@.appearanceUuid == '${otherCreatedCourtAppearance.appearanceUuid}')].charges[?(@.chargeUuid == '${createdCharge.chargeUuid}')].sentence.sentenceUuid")
      .isEqualTo(createdSentence.sentenceUuid.toString())
      .jsonPath("$.appearances[?(@.appearanceUuid == '${otherCreatedCourtAppearance.appearanceUuid}')].charges[?(@.chargeUuid == '${createdCharge.chargeUuid}')].sentence.periodLengths[?(@.periodLengthUuid == '${createdPeriodLength.periodLengthUuid}')].periodLengthUuid")
      .isEqualTo(createdPeriodLength.periodLengthUuid.toString())
  }
}
