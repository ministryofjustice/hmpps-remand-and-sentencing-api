package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.courtcase

import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.DpsDataCreator
import java.util.UUID

class GetSentencedCourtCaseTests : IntegrationTestBase() {

  @Test
  fun `get all court cases with at least 1 active sentence`() {
    val remandedCharge = DpsDataCreator.dpsCreateCharge(sentence = null, outcomeUuid = UUID.fromString("315280e5-d53e-43b3-8ba6-44da25676ce2"))
    val remandedCourtAppearance = DpsDataCreator.dpsCreateCourtAppearance(charges = listOf(remandedCharge), outcomeUuid = UUID.fromString("2f585681-7b1a-44fb-a0cb-f9a4b1d9cda8"))
    val remandedCourtCase = DpsDataCreator.dpsCreateCourtCase(appearances = listOf(remandedCourtAppearance))
    val (remandedCourtCaseUuid) = createCourtCase(remandedCourtCase)
    val (courtCaseUuid, createdCase) = createCourtCase()
    webTestClient
      .get()
      .uri("/person/${createdCase.prisonerId}/sentenced-court-cases")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.courtCases[?(@.courtCaseUuid == '$courtCaseUuid')]")
      .exists()
      .jsonPath("$.courtCases[?(@.courtCaseUuid == '$remandedCourtCaseUuid')]")
      .doesNotExist()
  }

  @Test
  fun `no token results in unauthorized`() {
    val (_, createdCase) = createCourtCase()
    webTestClient.get()
      .uri("/person/${createdCase.prisonerId}/sentenced-court-cases")
      .headers {
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `token with incorrect role is forbidden`() {
    val (_, createdCase) = createCourtCase()
    webTestClient.get()
      .uri("/person/${createdCase.prisonerId}/sentenced-court-cases")
      .headers {
        it.authToken(roles = listOf("ROLE_OTHER_FUNCTION"))
      }
      .exchange()
      .expectStatus()
      .isForbidden
  }
}
