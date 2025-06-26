package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.courtcase

import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.util.DataCreator
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.MigrationCreateCourtCasesResponse
import java.time.LocalDate

class LegacyUnlinkCourtCaseTests : IntegrationTestBase() {

  @Test
  fun `unlink source from target case`() {
    val sourceCharge = DataCreator.migrationCreateCharge(sentence = null, merged = true)
    val sourceAppearance = DataCreator.migrationCreateCourtAppearance(charges = listOf(sourceCharge))
    val sourceCourtCase = DataCreator.migrationCreateCourtCase(appearances = listOf(sourceAppearance), merged = true)

    val targetCharge = DataCreator.migrationCreateCharge(
      chargeNOMISId = sourceCharge.chargeNOMISId,
      sentence = null,
      mergedFromCaseId = sourceCourtCase.caseId,
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
    val sourceCourtCaseUuid = response.courtCases.first { it.caseId == sourceCourtCase.caseId }.courtCaseUuid

    webTestClient
      .put()
      .uri("/legacy/court-case/$sourceCourtCaseUuid/unlink/$targetCourtCaseUuid")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_COURT_CASE_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isNoContent

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
      .jsonPath("$.content[?(@.courtCaseUuid == '$sourceCourtCaseUuid')].courtCaseStatus")
      .isEqualTo(EntityStatus.ACTIVE.name)
  }

  @Test
  fun `no token results in unauthorized`() {
    val sourceCharge = DataCreator.migrationCreateCharge(sentence = null, merged = true)
    val sourceAppearance = DataCreator.migrationCreateCourtAppearance(charges = listOf(sourceCharge))
    val sourceCourtCase = DataCreator.migrationCreateCourtCase(appearances = listOf(sourceAppearance), merged = true)

    val targetCharge = DataCreator.migrationCreateCharge(
      chargeNOMISId = sourceCharge.chargeNOMISId,
      sentence = null,
      mergedFromCaseId = sourceCourtCase.caseId,
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
    val sourceCourtCaseUuid = response.courtCases.first { it.caseId == sourceCourtCase.caseId }.courtCaseUuid

    webTestClient
      .put()
      .uri("/legacy/court-case/$sourceCourtCaseUuid/unlink/$targetCourtCaseUuid")
      .headers {
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `token with incorrect role is forbidden`() {
    val sourceCharge = DataCreator.migrationCreateCharge(sentence = null, merged = true)
    val sourceAppearance = DataCreator.migrationCreateCourtAppearance(charges = listOf(sourceCharge))
    val sourceCourtCase = DataCreator.migrationCreateCourtCase(appearances = listOf(sourceAppearance), merged = true)

    val targetCharge = DataCreator.migrationCreateCharge(
      chargeNOMISId = sourceCharge.chargeNOMISId,
      sentence = null,
      mergedFromCaseId = sourceCourtCase.caseId,
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
    val sourceCourtCaseUuid = response.courtCases.first { it.caseId == sourceCourtCase.caseId }.courtCaseUuid

    webTestClient
      .put()
      .uri("/legacy/court-case/$sourceCourtCaseUuid/unlink/$targetCourtCaseUuid")
      .headers {
        it.authToken(roles = listOf("ROLE_OTHER_FUNCTION"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isForbidden
  }
}
