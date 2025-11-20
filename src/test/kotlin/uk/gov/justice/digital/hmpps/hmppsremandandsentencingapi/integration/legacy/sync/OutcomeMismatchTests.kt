package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.sync

import org.hamcrest.Matchers.everyItem
import org.hamcrest.core.IsNull
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.util.DataCreator
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacyCourtAppearanceCreatedResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.MigrationCreateCourtCasesResponse
import java.time.LocalDate

class OutcomeMismatchTests : IntegrationTestBase() {

  @Test
  fun `simulate outcome mismatch`() {
    val migratedCharge = DataCreator.migrationCreateCharge(legacyData = DataCreator.chargeLegacyData(nomisOutcomeCode = null, outcomeDescription = null), sentence = null)
    val migratedCourtAppearance = DataCreator.migrationCreateCourtAppearance(charges = listOf(migratedCharge), legacyData = DataCreator.courtAppearanceLegacyData(nomisOutcomeCode = "4531", outcomeDescription = "Remand"))
    val migratedCourtCase = DataCreator.migrationCreateCourtCase(appearances = listOf(migratedCourtAppearance))
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
    val createdAppearance = response.appearances.first()
    val futureCourtAppearanceWithNoOutcome = DataCreator.legacyCreateCourtAppearance(courtCaseUuid = courtCaseResponse.courtCaseUuid, appearanceDate = LocalDate.now().plusDays(5), legacyData = DataCreator.courtAppearanceLegacyData(nomisOutcomeCode = null, outcomeDescription = null, outcomeDispositionCode = null, outcomeConvictionFlag = null))
    val courtAppearanceCreatedResponse = webTestClient
      .post()
      .uri("/legacy/court-appearance")
      .bodyValue(futureCourtAppearanceWithNoOutcome)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_APPEARANCE_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isCreated
      .returnResult(LegacyCourtAppearanceCreatedResponse::class.java)
      .responseBody.blockFirst()!!

    val updateChargeWithOutcome = DataCreator.legacyUpdateCharge(
      offenceStartDate = migratedCharge.offenceStartDate,
      offenceEndDate = migratedCharge.offenceEndDate,
      legacyData = DataCreator.chargeLegacyData(nomisOutcomeCode = "4565", outcomeDescription = "Some outcome description"),
      performedByUser = "USER1",
    )
    webTestClient
      .put()
      .uri("/legacy/charge/${createdCharge.chargeUuid}/appearance/${createdAppearance.appearanceUuid}")
      .bodyValue(updateChargeWithOutcome)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_CHARGE_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isNoContent

    val chargeWithNoOutcome = DataCreator.legacyUpdateCharge(migratedCharge.offenceStartDate, migratedCharge.offenceEndDate, legacyData = DataCreator.chargeLegacyData(nomisOutcomeCode = null, outcomeDescription = null, outcomeDispositionCode = null, outcomeConvictionFlag = null))
    webTestClient
      .put()
      .uri("/legacy/court-appearance/${courtAppearanceCreatedResponse.lifetimeUuid}/charge/${createdCharge.chargeUuid}")
      .bodyValue(chargeWithNoOutcome)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_APPEARANCE_RW"))
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
      .jsonPath("$.appearances[?(@.appearanceUuid == '${createdAppearance.appearanceUuid}')].charges[?(@.chargeUuid == '${createdCharge.chargeUuid}')].nomisOutcomeCode")
      .isEqualTo(updateChargeWithOutcome.legacyData.nomisOutcomeCode)
      .jsonPath("$.appearances[?(@.appearanceUuid == '${courtAppearanceCreatedResponse.lifetimeUuid}')].charges[?(@.chargeUuid == '${createdCharge.chargeUuid}')].nomisOutcomeCode")
      .value(everyItem(IsNull.nullValue()))
  }
}
