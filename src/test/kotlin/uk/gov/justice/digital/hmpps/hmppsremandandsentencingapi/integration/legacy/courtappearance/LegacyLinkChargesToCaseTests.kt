package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.courtappearance

import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.util.DataCreator
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.MigrationCreateCourtCasesResponse
import java.time.format.DateTimeFormatter
import java.util.UUID

class LegacyLinkChargesToCaseTests : IntegrationTestBase() {

  @Test
  fun `links charge to source case`() {
    val charge = DataCreator.migrationCreateCharge(chargeNOMISId = 889)
    val sourceCourtCase = DataCreator.migrationCreateCourtCase(appearances = listOf(DataCreator.migrationCreateCourtAppearance(eventId = 556, charges = listOf(charge))))
    val targetCourtCase = DataCreator.migrationCreateCourtCase(caseId = 2)
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
    val targetAppearanceUuid = response.appearances.first { it.eventId == targetCourtCase.appearances.first().eventId }.appearanceUuid
    val sourceCourtCaseUuid = response.courtCases.first { it.caseId == sourceCourtCase.caseId }.courtCaseUuid
    val sourceChargeUuid = response.charges.first { it.chargeNOMISId == charge.chargeNOMISId }.chargeUuid

    val toLinkAppearanceToCharge = DataCreator.legacyUpdateCharge(
      offenceStartDate = charge.offenceStartDate,
      offenceEndDate = charge.offenceEndDate,
      legacyData = charge.legacyData,
    )

    webTestClient
      .put()
      .uri("/legacy/court-appearance/$targetAppearanceUuid/charge/$sourceChargeUuid")
      .bodyValue(toLinkAppearanceToCharge)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_APPEARANCE_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isNoContent

    val linkChargeToCase = DataCreator.legacyLinkChargeToCase(sourceCourtCaseUuid)

    webTestClient
      .put()
      .uri("/legacy/court-appearance/$targetAppearanceUuid/charge/$sourceChargeUuid/link")
      .bodyValue(linkChargeToCase)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_APPEARANCE_RW"))
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
      .jsonPath("$.content[?(@.courtCaseUuid == '$targetCourtCaseUuid')].latestCourtAppearance.charges[?(@.chargeUuid == '$sourceChargeUuid')].mergedFromCase.caseReference")
      .isEqualTo(sourceCourtCase.courtCaseLegacyData.caseReferences.first().offenderCaseReference)
      .jsonPath("$.content[?(@.courtCaseUuid == '$targetCourtCaseUuid')].latestCourtAppearance.charges[?(@.chargeUuid == '$sourceChargeUuid')].mergedFromCase.courtCode")
      .isEqualTo(sourceCourtCase.appearances.first().courtCode)
      .jsonPath("$.content[?(@.courtCaseUuid == '$targetCourtCaseUuid')].latestCourtAppearance.charges[?(@.chargeUuid == '$sourceChargeUuid')].mergedFromCase.mergedFromDate")
      .isEqualTo(linkChargeToCase.linkedDate.format(DateTimeFormatter.ISO_DATE))
  }

  @Test
  fun `throw not found when charge and appearance are not linked`() {
    val charge = DataCreator.migrationCreateCharge(chargeNOMISId = 889)
    val sourceCourtCase = DataCreator.migrationCreateCourtCase(appearances = listOf(DataCreator.migrationCreateCourtAppearance(eventId = 556, charges = listOf(charge))))
    val targetCourtCase = DataCreator.migrationCreateCourtCase(caseId = 2)
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

    val targetAppearanceUuid = response.appearances.first { it.eventId == targetCourtCase.appearances.first().eventId }.appearanceUuid
    val sourceCourtCaseUuid = response.courtCases.first { it.caseId == sourceCourtCase.caseId }.courtCaseUuid
    val sourceChargeUuid = response.charges.first { it.chargeNOMISId == charge.chargeNOMISId }.chargeUuid

    val linkChargeToCase = DataCreator.legacyLinkChargeToCase(sourceCourtCaseUuid)

    webTestClient
      .put()
      .uri("/legacy/court-appearance/$targetAppearanceUuid/charge/$sourceChargeUuid/link")
      .bodyValue(linkChargeToCase)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_APPEARANCE_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isNotFound
  }

  @Test
  fun `no token results in unauthorized`() {
    val linkChargeToCase = DataCreator.legacyLinkChargeToCase(UUID.randomUUID().toString())

    webTestClient
      .put()
      .uri("/legacy/court-appearance/${UUID.randomUUID()}/charge/${UUID.randomUUID()}/link")
      .bodyValue(linkChargeToCase)
      .headers {
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `token with incorrect role is forbidden`() {
    val linkChargeToCase = DataCreator.legacyLinkChargeToCase(UUID.randomUUID().toString())

    webTestClient
      .put()
      .uri("/legacy/court-appearance/${UUID.randomUUID()}/charge/${UUID.randomUUID()}/link")
      .bodyValue(linkChargeToCase)
      .headers {
        it.contentType = MediaType.APPLICATION_JSON
        it.authToken(roles = listOf("ROLE_OTHER_FUNCTION"))
      }
      .exchange()
      .expectStatus()
      .isForbidden
  }
}
