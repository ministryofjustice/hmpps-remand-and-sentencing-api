package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.courtcase

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.courtappearance.ChargeAggravatingFactorHelper
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.util.DataCreator
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.CourtCaseEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.MigrationCreateCourtCasesResponse
import java.time.format.DateTimeFormatter

class LegacyLinkCourtCaseTests : IntegrationTestBase() {

  @Autowired
  private lateinit var jdbcTemplate: NamedParameterJdbcTemplate
  private val aggravatingFactors by lazy { ChargeAggravatingFactorHelper(jdbcTemplate) }

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
      .isEqualTo(CourtCaseEntityStatus.MERGED.name)
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

  @Test
  fun `should preserve aggravating factors when charge has no records on source case`() {
    val sharedChargeId = 99L
    val sourceCase = DataCreator.migrationCreateCourtCase(
      caseId = 1,
      appearances = listOf(DataCreator.migrationCreateCourtAppearance(eventId = 1, charges = listOf(DataCreator.migrationCreateCharge(chargeNOMISId = sharedChargeId)))),
    )
    val targetCase = DataCreator.migrationCreateCourtCase(
      caseId = 2,
      appearances = listOf(DataCreator.migrationCreateCourtAppearance(eventId = 2, charges = listOf(DataCreator.migrationCreateCharge(chargeNOMISId = sharedChargeId)))),
    )
    val migrateResponse = migrateCases(DataCreator.migrationCreateCourtCases(courtCases = listOf(sourceCase, targetCase)))
    val sourceCaseUuid = migrateResponse.courtCases.first { it.caseId == sourceCase.caseId }.courtCaseUuid
    val chargeUuid = migrateResponse.charges.first { it.chargeNOMISId == sharedChargeId }.chargeUuid
    val targetAppearanceUuid = migrateResponse.appearances.first { it.eventId == 2L }.appearanceUuid

    webTestClient.put()
      .uri("/legacy/court-appearance/$targetAppearanceUuid/charge/$chargeUuid/link")
      .bodyValue(DataCreator.legacyLinkChargeToCase(sourceCourtCaseUuid = sourceCaseUuid))
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_APPEARANCE_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus().isNoContent

    assertThat(aggravatingFactors.countAggravatingFactor(chargeUuid, "OATC")).isEqualTo(0)
    assertThat(aggravatingFactors.countAggravatingFactor(chargeUuid, "OAFPC")).isEqualTo(0)
  }
}
