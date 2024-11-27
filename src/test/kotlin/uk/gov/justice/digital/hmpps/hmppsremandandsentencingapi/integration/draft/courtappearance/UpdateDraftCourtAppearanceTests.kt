package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.draft.courtappearance

import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.DraftDataCreator

class UpdateDraftCourtAppearanceTests : IntegrationTestBase() {

  @Test
  fun `update draft court appearance`() {
    val draftCourtCase = createDraftCourtCase()
    val draftCourtAppearance = DraftDataCreator.draftCreateCourtAppearance(sessionBlob = objectMapper.createObjectNode().put("aNewFieldKey", "aNewFieldValue"))
    webTestClient
      .put()
      .uri("/draft/court-appearance/${draftCourtCase.draftAppearances.first().draftUuid}")
      .bodyValue(draftCourtAppearance)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_REMAND_AND_SENTENCING_UI"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isOk
  }

  @Test
  fun `no token results in unauthorized`() {
    val draftCourtCase = createDraftCourtCase()
    val draftCourtAppearance = DraftDataCreator.draftCreateCourtAppearance(sessionBlob = objectMapper.createObjectNode().put("aNewFieldKey", "aNewFieldValue"))
    webTestClient
      .put()
      .uri("/draft/court-appearance/${draftCourtCase.draftAppearances.first().draftUuid}")
      .bodyValue(draftCourtAppearance)
      .headers {
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `token with incorrect role is forbidden`() {
    val draftCourtCase = createDraftCourtCase()
    val draftCourtAppearance = DraftDataCreator.draftCreateCourtAppearance(sessionBlob = objectMapper.createObjectNode().put("aNewFieldKey", "aNewFieldValue"))
    webTestClient
      .put()
      .uri("/draft/court-appearance/${draftCourtCase.draftAppearances.first().draftUuid}")
      .bodyValue(draftCourtAppearance)
      .headers {
        it.authToken(roles = listOf("ROLE_OTHER_FUNCTION"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isForbidden
  }
}
