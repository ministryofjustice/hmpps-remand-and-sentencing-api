package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.prisoner

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacyCourtCaseUuids

class LegacyGetCourtCaseUuidsTests : IntegrationTestBase() {

  @Test
  fun `retrieve all court case uuids for prisoner`() {
    val (firstCourtCaseUuid, createCourtCase) = createLegacyCourtCase()
    val (secondCourtCaseUuid) = createLegacyCourtCase()
    deleteCourtCase(secondCourtCaseUuid)
    val result = webTestClient
      .get()
      .uri("/legacy/prisoner/${createCourtCase.prisonerId}/court-case-uuids")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_COURT_CASE_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isOk.returnResult(LegacyCourtCaseUuids::class.java)
      .responseBody.blockFirst()!!

    Assertions.assertThat(result.courtCaseUuids).contains(firstCourtCaseUuid)
    Assertions.assertThat(result.courtCaseUuids).doesNotContain(secondCourtCaseUuid)
  }

  @Test
  fun `no token results in unauthorized`() {
    val (_, createCourtCase) = createLegacyCourtCase()

    webTestClient
      .get()
      .uri("/legacy/prisoner/${createCourtCase.prisonerId}/court-case-uuids")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `token with incorrect role is forbidden`() {
    val (_, createCourtCase) = createLegacyCourtCase()
    webTestClient
      .get()
      .uri("/legacy/prisoner/${createCourtCase.prisonerId}/court-case-uuids")
      .headers {
        it.authToken(roles = listOf("ROLE_OTHER_FUNCTION"))
      }
      .exchange()
      .expectStatus()
      .isForbidden
  }

  private fun deleteCourtCase(courtCaseUuid: String) {
    webTestClient
      .delete()
      .uri("/legacy/court-case/$courtCaseUuid")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_COURT_CASE_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isOk
  }
}
