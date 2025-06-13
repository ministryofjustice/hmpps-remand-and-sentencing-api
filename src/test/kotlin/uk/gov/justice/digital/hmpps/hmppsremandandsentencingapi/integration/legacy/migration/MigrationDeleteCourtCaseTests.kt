package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.migration

import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.util.DataCreator
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.MigrationCreateCourtCases
import java.time.LocalDate

class MigrationDeleteCourtCaseTests : IntegrationTestBase() {

  @Test
  fun `Migrate a case twice - ensure the original one is fully deleted, and the new one is persisted - other cases should not be deleted`() {
    val migrationCourtCases = DataCreator.migrationCreateCourtCases()
    migrateCase(migrationCourtCases)
    val extraPrisonerMigratedOnce = "ANOTHER"
    migrateCase(migrationCourtCases.copy(prisonerId = extraPrisonerMigratedOnce))

    // Check that the first prisoner data is migrated correctly
    getPagedSearchResponse(migrationCourtCases.prisonerId)
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.content.length()").isEqualTo(1)
      .jsonPath("$.content[0].prisonerId").isEqualTo(migrationCourtCases.prisonerId)
      .jsonPath("$.content[0].legacyData.caseReferences[0].offenderCaseReference").isEqualTo("NOMIS123")
      .jsonPath("$.content[0].appearanceCount").isEqualTo(1)
      .jsonPath("$.content[0].latestCourtAppearance.caseReference").isEqualTo("NOMIS123")
      .jsonPath("$.content[0].latestCourtAppearance.courtCode").isEqualTo("COURT1")
      .jsonPath("$.content[0].latestCourtAppearance.warrantDate").isEqualTo(LocalDate.now().toString())
      .jsonPath("$.content[0].latestCourtAppearance.warrantType").isEqualTo("REMAND")
      .jsonPath("$.content[0].latestCourtAppearance.outcome").isEqualTo("Outcome Description")
      .jsonPath("$.content[0].latestCourtAppearance.charges[0].offenceCode").isEqualTo("OFF1")

    // Check that the second prisoner data is migrated correctly
    getPagedSearchResponse(extraPrisonerMigratedOnce)
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.content.length()").isEqualTo(1)
      .jsonPath("$.content[0].prisonerId").isEqualTo(extraPrisonerMigratedOnce)
      .jsonPath("$.content[0].legacyData.caseReferences[0].offenderCaseReference").isEqualTo("NOMIS123")

    // Create new data for the first prisoner to migrate
    val originalCase = migrationCourtCases.courtCases[0]
    val originalReference = originalCase.courtCaseLegacyData.caseReferences[0]
    val updatedReference = originalReference.copy(offenderCaseReference = "NEW-REFERENCE")

    val originalAppearance = originalCase.appearances[0]
    val originalCharge = originalAppearance.charges[0]
    val originalSentence = originalCharge.sentence!!
    val updatedLegacyData = originalAppearance.legacyData.copy(
      outcomeDescription = "New Outcome",
      outcomeConvictionFlag = true,
      outcomeDispositionCode = "F",
    )

    val updatedSentence = originalSentence.copy(
      legacyData = originalSentence.legacyData.copy(sentenceTypeDesc = "Updated Sentence Type"),
    )

    val updatedCharge = originalCharge.copy(
      offenceCode = "NEW-OFFENCE",
      sentence = updatedSentence,
    )

    val newWarrantDate = LocalDate.now().minusDays(1)
    val updatedAppearance = originalAppearance.copy(
      courtCode = "NEWCOURT",
      appearanceDate = newWarrantDate,
      legacyData = updatedLegacyData,
      charges = listOf(updatedCharge),
    )

    val newCourtCase = originalCase.copy(
      courtCaseLegacyData = originalCase.courtCaseLegacyData.copy(
        caseReferences = mutableListOf(updatedReference),
      ),
      appearances = listOf(updatedAppearance),
    )

    // Migrate the first prisoner again
    val newMigrationRequest = migrationCourtCases.copy(courtCases = listOf(newCourtCase))
    migrateCase(newMigrationRequest)

    // Ensure the first prisoner details have been completely refreshed (deleted and recreated)
    getPagedSearchResponse(migrationCourtCases.prisonerId)
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.content.length()").isEqualTo(1)
      .jsonPath("$.content[0].prisonerId").isEqualTo(migrationCourtCases.prisonerId)
      .jsonPath("$.content[0].legacyData.caseReferences[0].offenderCaseReference").isEqualTo("NEW-REFERENCE")
      .jsonPath("$.content[0].appearanceCount").isEqualTo(1)
      .jsonPath("$.content[0].latestCourtAppearance.caseReference").isEqualTo("NEW-REFERENCE")
      .jsonPath("$.content[0].latestCourtAppearance.courtCode").isEqualTo("NEWCOURT")
      .jsonPath("$.content[0].latestCourtAppearance.warrantDate").isEqualTo(newWarrantDate.toString())
      .jsonPath("$.content[0].latestCourtAppearance.warrantType").isEqualTo("SENTENCING")
      .jsonPath("$.content[0].latestCourtAppearance.outcome").isEqualTo("New Outcome")
      .jsonPath("$.content[0].latestCourtAppearance.charges[0].offenceCode").isEqualTo("NEW-OFFENCE")

    // Ensure the second prisoner that was only migrated once still exists and has not been deleted
    getPagedSearchResponse(extraPrisonerMigratedOnce)
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.content.length()").isEqualTo(1)
      .jsonPath("$.content[0].prisonerId").isEqualTo(extraPrisonerMigratedOnce)
      .jsonPath("$.content[0].legacyData.caseReferences[0].offenderCaseReference").isEqualTo("NOMIS123")
  }

  private fun migrateCase(migrationCourtCases: MigrationCreateCourtCases) {
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
  }

  private fun getPagedSearchResponse(prisonerId: String) = webTestClient.get()
    .uri {
      it.path("/court-case/paged/search")
        .queryParam("prisonerId", prisonerId)
        .build()
    }
    .headers {
      it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
    }
    .exchange()
}
