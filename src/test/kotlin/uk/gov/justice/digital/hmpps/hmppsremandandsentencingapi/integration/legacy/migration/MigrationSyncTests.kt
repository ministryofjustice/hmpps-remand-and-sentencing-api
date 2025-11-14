package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.migration

import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.util.DataCreator
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.util.DataCreator.Factory.migrationCreateCharge
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.util.DataCreator.Factory.migrationCreateCourtAppearance
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.util.DataCreator.Factory.migrationCreateCourtCase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacyCourtAppearanceCreatedResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.MigrationCreateCourtCasesResponse

class MigrationSyncTests : IntegrationTestBase() {

  @Test
  fun `charge outcome remains`() {
    val migratedCharge = migrationCreateCharge(legacyData = DataCreator.chargeLegacyData(nomisOutcomeCode = "4012", outcomeDescription = "Remand"))
    val migratedCourtAppearance = migrationCreateCourtAppearance(charges = listOf(migratedCharge), legacyData = DataCreator.courtAppearanceLegacyData(nomisOutcomeCode = "4531", outcomeDescription = "Remand"))
    val migratedCourtCase = migrationCreateCourtCase(appearances = listOf(migratedCourtAppearance))
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
    val courtAppearanceWithNoOutcome = DataCreator.legacyCreateCourtAppearance(courtCaseUuid = courtCaseResponse.courtCaseUuid, legacyData = DataCreator.courtAppearanceLegacyData(nomisOutcomeCode = null, outcomeDescription = null, outcomeDispositionCode = null, outcomeConvictionFlag = null))
    val courtAppearanceCreatedResponse = webTestClient
      .post()
      .uri("/legacy/court-appearance")
      .bodyValue(courtAppearanceWithNoOutcome)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_APPEARANCE_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isCreated
      .returnResult(LegacyCourtAppearanceCreatedResponse::class.java)
      .responseBody.blockFirst()!!

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
      .delete()
      .uri("/legacy/court-appearance/${courtAppearanceCreatedResponse.lifetimeUuid}/charge/${createdCharge.chargeUuid}")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_APPEARANCE_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isOk
  }
}
