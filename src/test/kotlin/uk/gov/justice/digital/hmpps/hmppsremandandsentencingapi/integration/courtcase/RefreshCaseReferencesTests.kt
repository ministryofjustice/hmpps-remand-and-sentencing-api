package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.courtcase

import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.CaseReferenceLegacyData
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.CourtCaseLegacyData
import java.time.LocalDateTime
import java.util.UUID

class RefreshCaseReferencesTests : IntegrationTestBase() {

  @Test
  fun `can refresh case references`() {
    val courtCase = createCourtCase()
    val legacyData = CourtCaseLegacyData(
      (
        courtCase.second.appearances.filter { it.courtCaseReference != null }.map {
          CaseReferenceLegacyData(
            it.courtCaseReference!!,
            LocalDateTime.now(),
          )
        } + CaseReferenceLegacyData("NEW_NOMIS_CASE_REFERENCE", LocalDateTime.now())
        ).toMutableList(),
    )
    webTestClient
      .put()
      .uri("/court-case/${courtCase.first}/case-references/refresh")
      .bodyValue(legacyData)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_COURT_CASE_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isNoContent
  }

  @Test
  fun `remove case reference from DPS court appearance when not in legacy data`() {
    val courtCase = createCourtCase()
    val dpsCaseReference = courtCase.second.appearances.first().courtCaseReference!!
    val legacyData = CourtCaseLegacyData(mutableListOf(CaseReferenceLegacyData("NEW_NOMIS_CASE_REFERENCE", LocalDateTime.now())))
    webTestClient
      .put()
      .uri("/court-case/${courtCase.first}/case-references/refresh")
      .bodyValue(legacyData)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_COURT_CASE_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isNoContent
    webTestClient
      .get()
    webTestClient
      .get()
      .uri("/court-case/${courtCase.first}")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING"))
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.appearances[?(@.courtCaseReference == '$dpsCaseReference')]")
      .doesNotExist()
  }

  @Test
  fun `no token results in unauthorized`() {
    val legacyData = CourtCaseLegacyData(mutableListOf())
    webTestClient
      .put()
      .uri("/court-case/${UUID.randomUUID()}/case-references/refresh")
      .bodyValue(legacyData)
      .headers {
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `token with incorrect role is forbidden`() {
    val legacyData = CourtCaseLegacyData(mutableListOf())
    webTestClient
      .put()
      .uri("/court-case/${UUID.randomUUID()}/case-references/refresh")
      .bodyValue(legacyData)
      .headers {
        it.authToken(roles = listOf("ROLE_OTHER_FUNCTION"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isForbidden
  }
}
