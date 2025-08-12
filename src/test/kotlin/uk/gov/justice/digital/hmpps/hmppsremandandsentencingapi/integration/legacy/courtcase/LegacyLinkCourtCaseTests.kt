package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.courtcase

import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.util.DataCreator
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.MigrationCreateCourtCasesResponse
import java.time.format.DateTimeFormatter

class LegacyLinkCourtCaseTests : IntegrationTestBase() {

  @Test
  fun `link source to target case`() {
    val sourceCourtCase = DataCreator.migrationCreateCourtCase()
    val targetCourtCase = DataCreator.migrationCreateCourtCase(caseId = 2)
    val courtCases = DataCreator.migrationCreateCourtCases(courtCases = listOf(sourceCourtCase, targetCourtCase))
    val response = migrateCases(courtCases)

    val targetCourtCaseUuid = response.courtCases.first { it.caseId == targetCourtCase.caseId }.courtCaseUuid
    val sourceCourtCaseUuid = response.courtCases.first { it.caseId == sourceCourtCase.caseId }.courtCaseUuid

    linkCases(sourceCourtCaseUuid, targetCourtCaseUuid)

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
      .isEqualTo(EntityStatus.MERGED.name)
  }

  @Test
  fun `check that fetching a linked case populates the merged case details correctly`() {
    val sourceCourtCase = DataCreator.migrationCreateCourtCase()
    val targetCourtCase = DataCreator.migrationCreateCourtCase(caseId = 2)
    val courtCases = DataCreator.migrationCreateCourtCases(courtCases = listOf(sourceCourtCase, targetCourtCase))
    val response = migrateCases(courtCases)

    val targetCourtCaseUuid = response.courtCases.first { it.caseId == targetCourtCase.caseId }.courtCaseUuid
    val sourceCourtCaseUuid = response.courtCases.first { it.caseId == sourceCourtCase.caseId }.courtCaseUuid

    linkCases(sourceCourtCaseUuid, targetCourtCaseUuid)

    webTestClient
      .get()
      .uri { it.path("/court-case/$sourceCourtCaseUuid").build() }
      .headers { it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI")) }
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.mergedToCaseDetails.warrantDate")
      .isEqualTo(targetCourtCase.appearances.first().appearanceDate.format(DateTimeFormatter.ISO_DATE))
      .jsonPath("$.mergedToCaseDetails.caseReference")
      .isEqualTo("NOMIS123")
      .jsonPath("$.mergedToCaseDetails.courtCode")
      .isEqualTo(targetCourtCase.appearances.first().courtCode)
      .jsonPath("$.mergedToCaseDetails.warrantDate")
      .isEqualTo(targetCourtCase.appearances.first().appearanceDate.format(DateTimeFormatter.ISO_DATE))
  }

  @Test
  fun `no token results in unauthorized`() {
    val sourceCourtCase = DataCreator.migrationCreateCourtCase()
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
    val sourceCourtCaseUuid = response.courtCases.first { it.caseId == sourceCourtCase.caseId }.courtCaseUuid

    webTestClient
      .put()
      .uri("/legacy/court-case/$sourceCourtCaseUuid/link/$targetCourtCaseUuid")
      .headers {
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `token with incorrect role is forbidden`() {
    val sourceCourtCase = DataCreator.migrationCreateCourtCase()
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
    val sourceCourtCaseUuid = response.courtCases.first { it.caseId == sourceCourtCase.caseId }.courtCaseUuid

    webTestClient
      .put()
      .uri("/legacy/court-case/$sourceCourtCaseUuid/link/$targetCourtCaseUuid")
      .headers {
        it.authToken(roles = listOf("ROLE_OTHER_FUNCTION"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isForbidden
  }
}
