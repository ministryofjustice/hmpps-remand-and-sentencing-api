package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.draft.courtcase

import org.hamcrest.text.MatchesPattern
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.DraftDataCreator

class CreateDraftCourtAppearanceInCourtCaseTests : IntegrationTestBase() {

  @Test
  fun `create draft court appearance in existing court case`() {
    val draftCourtCase = createDraftCourtCase()
    val draftCourtAppearance = DraftDataCreator.draftCreateCourtAppearance(sessionBlob = objectMapper.createObjectNode().put("aNewFieldKey", "aNewFieldValue"))
    webTestClient
      .post()
      .uri("/draft/court-case/${draftCourtCase.courtCaseUuid}/appearance")
      .bodyValue(draftCourtAppearance)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_REMAND_AND_SENTENCING_UI"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isCreated
      .expectBody()
      .jsonPath("$.draftUuid")
      .value(MatchesPattern.matchesPattern("([a-f0-9]{8}(-[a-f0-9]{4}){4}[a-f0-9]{8})"))
  }

  @Test
  fun `no token results in unauthorized`() {
    val draftCreateCourtCase = DraftDataCreator.draftCreateCourtCase()
    webTestClient
      .post()
      .uri("/draft/court-case")
      .bodyValue(draftCreateCourtCase)
      .headers {
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `token with incorrect role is forbidden`() {
    val draftCreateCourtCase = DraftDataCreator.draftCreateCourtCase()
    webTestClient
      .post()
      .uri("/draft/court-case")
      .bodyValue(draftCreateCourtCase)
      .headers {
        it.authToken(roles = listOf("ROLE_OTHER_FUNCTION"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isForbidden
  }
}
