package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.courtcase

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CourtCase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.CaseReferenceLegacyData
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.RefreshCaseReferences
import java.time.LocalDateTime
import java.util.UUID

class LegacyRefreshCaseReferencesTests : IntegrationTestBase() {

  @Test
  fun `can refresh case references`() {
    val courtCase = createCourtCase()
    val refreshCaseReferences = RefreshCaseReferences(
      (
        courtCase.second.appearances.filter { it.courtCaseReference != null }.map {
          CaseReferenceLegacyData(
            it.courtCaseReference!!,
            LocalDateTime.now(),
          )
        } + CaseReferenceLegacyData("NEW_NOMIS_CASE_REFERENCE", LocalDateTime.now())
        ).toMutableList(),
      "SOME_USER",
    )
    webTestClient
      .put()
      .uri("/legacy/court-case/${courtCase.first}/case-references/refresh")
      .bodyValue(refreshCaseReferences)
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
    val refreshCaseReferences = RefreshCaseReferences(mutableListOf(CaseReferenceLegacyData("NEW_NOMIS_CASE_REFERENCE", LocalDateTime.now())), "SOME_USER")
    webTestClient
      .put()
      .uri("/legacy/court-case/${courtCase.first}/case-references/refresh")
      .bodyValue(refreshCaseReferences)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_COURT_CASE_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isNoContent
    webTestClient
      .get()
      .uri("/court-case/${courtCase.first}")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.appearances[?(@.courtCaseReference == '$dpsCaseReference')]")
      .doesNotExist()
  }

  @Test
  fun `only update active court appearances`() {
    val (courtCaseUuid, courtCase) = createCourtCase()
    val toUpdateAppearance = courtCase.appearances.first().copy(courtCaseReference = "SOMETHINGDIFFERENT", courtCaseUuid = courtCaseUuid)
    webTestClient
      .put()
      .uri("/court-appearance/${toUpdateAppearance.appearanceUuid}")
      .bodyValue(toUpdateAppearance)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isOk
    val refreshCaseReferences = RefreshCaseReferences(mutableListOf(CaseReferenceLegacyData("NEW_NOMIS_CASE_REFERENCE", LocalDateTime.now())), "SOME_USER")
    webTestClient
      .put()
      .uri("/legacy/court-case/$courtCaseUuid/case-references/refresh")
      .bodyValue(refreshCaseReferences)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_COURT_CASE_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isNoContent

    val result = webTestClient
      .get()
      .uri("/court-case/$courtCaseUuid")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
      }
      .exchange()
      .expectStatus()
      .isOk
      .returnResult(CourtCase::class.java)
      .responseBody.blockFirst()!!

    Assertions.assertThat(result.appearances.filter { it.appearanceUuid == toUpdateAppearance.appearanceUuid }).hasSize(1)
  }

  @Test
  fun `no token results in unauthorized`() {
    val refreshCaseReferences = RefreshCaseReferences(mutableListOf(), "SOME_USER")
    webTestClient
      .put()
      .uri("/legacy/court-case/${UUID.randomUUID()}/case-references/refresh")
      .bodyValue(refreshCaseReferences)
      .headers {
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `token with incorrect role is forbidden`() {
    val refreshCaseReferences = RefreshCaseReferences(mutableListOf(), "SOME_USER")
    webTestClient
      .put()
      .uri("/legacy/court-case/${UUID.randomUUID()}/case-references/refresh")
      .bodyValue(refreshCaseReferences)
      .headers {
        it.authToken(roles = listOf("ROLE_OTHER_FUNCTION"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isForbidden
  }
}
