package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.courtcase

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase

class GetCourtCaseCountNumbersTests : IntegrationTestBase() {

  @Test
  fun `retrieve all count numbers for a court case`() {
    val (courtCaseUuid, courtCase) = createCourtCase()
    val countNumber = courtCase.appearances.first().charges.first().sentence!!.chargeNumber!!
    webTestClient
      .get()
      .uri("/court-case/$courtCaseUuid/count-numbers")
      .headers { it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI")) }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.countNumbers[0].countNumber")
      .isEqualTo(countNumber)
  }

  @Test
  fun `no token results in unauthorized`() {
    val (courtCaseUuid) = createCourtCase()
    webTestClient.get()
      .uri("/court-case/$courtCaseUuid/count-numbers")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `token with incorrect role is forbidden`() {
    val (courtCaseUuid) = createCourtCase()
    webTestClient.get()
      .uri("/court-case/$courtCaseUuid/count-numbers")
      .headers { it.authToken(roles = listOf("ROLE_OTHER_FUNCTION")) }
      .exchange()
      .expectStatus()
      .isForbidden
  }
}
