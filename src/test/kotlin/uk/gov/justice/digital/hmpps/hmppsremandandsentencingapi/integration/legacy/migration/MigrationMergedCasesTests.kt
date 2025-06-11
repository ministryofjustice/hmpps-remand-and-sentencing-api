package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.migration

import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.util.DataCreator
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.MigrationCreateCourtCasesResponse
import java.time.LocalDate

class MigrationMergedCasesTests : IntegrationTestBase() {

  @Test
  fun `create source court case for a linked case`() {
    val sourceResponse = createSourceMergedCourtCase()
    val sourceCourtCaseUuid = sourceResponse.courtCases.first().courtCaseUuid
    val sourceChargeUuid = sourceResponse.charges.first().chargeUuid.toString()
    webTestClient
      .get()
      .uri("/court-case/$sourceCourtCaseUuid")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING"))
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.status")
      .isEqualTo("MERGED")
      .jsonPath("$.appearances[*].charges[?(@.chargeUuid == '$sourceChargeUuid')]")
      .exists()
  }

  @Test
  fun `create target court case for a linked case`() {
    val sourceCharge = DataCreator.migrationCreateCharge(sentence = null, merged = true)
    val sourceAppearance = DataCreator.migrationCreateCourtAppearance(charges = listOf(sourceCharge))
    val sourceCourtCase = DataCreator.migrationCreateCourtCase(appearances = listOf(sourceAppearance), merged = true)

    val targetCharge = DataCreator.migrationCreateCharge(
      chargeNOMISId = sourceCharge.chargeNOMISId,
      sentence = null,
      mergedFromCaseId = sourceCourtCase.caseId,
      mergedFromEventId = sourceAppearance.eventId,
      mergedFromDate = LocalDate.now().minusYears(6),

    )

    val targetAppearance = DataCreator.migrationCreateCourtAppearance(eventId = sourceAppearance.eventId + 1, charges = listOf(targetCharge))
    val targetCourtCase = DataCreator.migrationCreateCourtCase(caseId = sourceCourtCase.caseId + 1, appearances = listOf(targetAppearance))
    val courtCases = DataCreator.migrationCreateCourtCases(courtCases = listOf(sourceCourtCase, targetCourtCase))
    val response = webTestClient
      .post()
      .uri("/legacy/court-case/migration")
      .bodyValue(courtCases)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_COURT_CASE_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isCreated
      .returnResult(MigrationCreateCourtCasesResponse::class.java)
      .responseBody.blockFirst()!!

    val targetCourtCaseUuid = response.courtCases.first { it.caseId == targetCourtCase.caseId }.courtCaseUuid
    val sourceChargeUuid = response.charges.first { it.chargeNOMISId == sourceCharge.chargeNOMISId }.chargeUuid

    webTestClient
      .get()
      .uri {
        it.path("/court-case/paged/search")
          .queryParam("prisonerId", courtCases.prisonerId)
          .build()
      }
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.content[?(@.courtCaseUuid == '$targetCourtCaseUuid')].latestCourtAppearance.charges[?(@.chargeUuid == '$sourceChargeUuid')].mergedFromCase.caseReference")
      .isEqualTo(sourceCourtCase.courtCaseLegacyData.caseReferences.first().offenderCaseReference)
      .jsonPath("$.content[?(@.courtCaseUuid == '$targetCourtCaseUuid')].latestCourtAppearance.charges[?(@.chargeUuid == '$sourceChargeUuid')].mergedFromCase.courtCode")
      .isEqualTo(sourceCourtCase.appearances.first().courtCode)
      .jsonPath("$.content[?(@.courtCaseUuid == '$targetCourtCaseUuid')].mergedFromCases[0].courtCode")
      .isEqualTo(sourceCourtCase.appearances.first().courtCode)
      .jsonPath("$.content[?(@.courtCaseUuid == '$targetCourtCaseUuid')].mergedFromCases[0].caseReference")
      .isEqualTo(sourceCourtCase.courtCaseLegacyData.caseReferences.first().offenderCaseReference)
  }

  @Test
  fun `create target court case for a linked case - process target court case first`() {
    val sourceCharge = DataCreator.migrationCreateCharge(sentence = null, merged = true)
    val sourceAppearance = DataCreator.migrationCreateCourtAppearance(charges = listOf(sourceCharge))
    val sourceCourtCase = DataCreator.migrationCreateCourtCase(appearances = listOf(sourceAppearance), merged = true)
    val expectedMergedFromDate = LocalDate.now().minusYears(6)

    val targetCharge = DataCreator.migrationCreateCharge(
      chargeNOMISId = sourceCharge.chargeNOMISId,
      sentence = null,
      mergedFromCaseId = sourceCourtCase.caseId,
      mergedFromEventId = sourceAppearance.eventId,
      mergedFromDate = expectedMergedFromDate,

    )

    val targetAppearance = DataCreator.migrationCreateCourtAppearance(eventId = sourceAppearance.eventId + 1, charges = listOf(targetCharge))
    val targetCourtCase = DataCreator.migrationCreateCourtCase(caseId = sourceCourtCase.caseId + 1, appearances = listOf(targetAppearance))
    val courtCases = DataCreator.migrationCreateCourtCases(courtCases = listOf(targetCourtCase, sourceCourtCase))
    val response = webTestClient
      .post()
      .uri("/legacy/court-case/migration")
      .bodyValue(courtCases)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_COURT_CASE_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isCreated
      .returnResult(MigrationCreateCourtCasesResponse::class.java)
      .responseBody.blockFirst()!!

    val targetCourtCaseUuid = response.courtCases.first { it.caseId == targetCourtCase.caseId }.courtCaseUuid
    val sourceChargeUuid = response.charges.first { it.chargeNOMISId == sourceCharge.chargeNOMISId }.chargeUuid

    webTestClient
      .get()
      .uri {
        it.path("/court-case/paged/search")
          .queryParam("prisonerId", courtCases.prisonerId)
          .build()
      }
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.content[?(@.courtCaseUuid == '$targetCourtCaseUuid')].latestCourtAppearance.charges[?(@.chargeUuid == '$sourceChargeUuid')].mergedFromCase.caseReference")
      .isEqualTo(sourceCourtCase.courtCaseLegacyData.caseReferences.first().offenderCaseReference)
      .jsonPath("$.content[?(@.courtCaseUuid == '$targetCourtCaseUuid')].latestCourtAppearance.charges[?(@.chargeUuid == '$sourceChargeUuid')].mergedFromCase.courtCode")
      .isEqualTo(sourceCourtCase.appearances.first().courtCode)
      .jsonPath("$.content[?(@.courtCaseUuid == '$targetCourtCaseUuid')].latestCourtAppearance.charges[?(@.chargeUuid == '$sourceChargeUuid')].mergedFromCase.mergedFromDate")
      .isEqualTo(expectedMergedFromDate.toString())
      .jsonPath("$.content[?(@.courtCaseUuid == '$targetCourtCaseUuid')].mergedFromCases[0].courtCode")
      .isEqualTo(sourceCourtCase.appearances.first().courtCode)
      .jsonPath("$.content[?(@.courtCaseUuid == '$targetCourtCaseUuid')].mergedFromCases[0].caseReference")
      .isEqualTo(sourceCourtCase.courtCaseLegacyData.caseReferences.first().offenderCaseReference)
  }

  private fun createSourceMergedCourtCase(): MigrationCreateCourtCasesResponse {
    val charge = DataCreator.migrationCreateCharge(sentence = null, merged = true)
    val appearance = DataCreator.migrationCreateCourtAppearance(charges = listOf(charge))
    val sourceCourtCase = DataCreator.migrationCreateCourtCase(appearances = listOf(appearance), merged = true)
    val courtCases = DataCreator.migrationCreateCourtCases(courtCases = listOf(sourceCourtCase))
    return webTestClient
      .post()
      .uri("/legacy/court-case/migration")
      .bodyValue(courtCases)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_COURT_CASE_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isCreated
      .returnResult(MigrationCreateCourtCasesResponse::class.java)
      .responseBody.blockFirst()!!
  }
}
