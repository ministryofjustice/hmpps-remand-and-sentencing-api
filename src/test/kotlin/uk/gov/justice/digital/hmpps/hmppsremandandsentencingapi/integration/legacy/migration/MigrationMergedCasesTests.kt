package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.migration

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.util.DataCreator
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.MigrationCreateCourtCaseResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.MigrationCreateCourtCasesResponse

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
  @Disabled("Will do this as a follow on PR")
  fun `create target court case for a linked case`() {
    val sourceResponse = createSourceMergedCourtCase()
    val sourceCourtCaseId = sourceResponse.courtCases.first().caseId
    val sourceChargeId = sourceResponse.charges.first().chargeNOMISId
    val sourceChargeUuid = sourceResponse.charges.first().chargeUuid
    val targetCharge = DataCreator.migrationCreateCharge(sentence = null, mergedFromCaseId = sourceCourtCaseId, mergedChargeNOMISId = sourceChargeId)
    val targetAppearance = DataCreator.migrationCreateCourtAppearance(charges = listOf(targetCharge))
    val targetCourtCase = DataCreator.migrationCreateCourtCase(appearances = listOf(targetAppearance))
    val targetResponse = webTestClient
      .post()
      .uri("/legacy/court-case/migration")
      .bodyValue(targetCourtCase)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_COURT_CASE_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isCreated
      .returnResult(MigrationCreateCourtCaseResponse::class.java)
      .responseBody.blockFirst()!!

    webTestClient
      .get()
      .uri("/court-case/${targetResponse.courtCaseUuid}")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING"))
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.appearances[*].charges[?(@.chargeUuid == '$sourceChargeUuid')]")
      .exists()
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
