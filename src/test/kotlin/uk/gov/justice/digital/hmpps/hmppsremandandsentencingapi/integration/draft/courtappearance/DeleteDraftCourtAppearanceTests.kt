package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.draft.courtappearance

import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import java.util.UUID

class DeleteDraftCourtAppearanceTests : IntegrationTestBase() {

  @Test
  fun `delete draft court appearance`() {
    val draftCourtCase = createDraftCourtCase()
    webTestClient
      .delete()
      .uri("/draft/court-appearance/${draftCourtCase.draftAppearances.first().draftUuid}")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_REMAND_AND_SENTENCING_UI"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isOk
  }

  @Test
  fun `no draft court appearance exist for uuid results in not found`() {
    webTestClient
      .delete()
      .uri("/draft/court-appearance/${UUID.randomUUID()}")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_REMAND_AND_SENTENCING_UI"))
      }
      .exchange()
      .expectStatus()
      .isNotFound
  }

  @Test
  fun `no token results in unauthorized`() {
    val draftCourtCase = createDraftCourtCase()
    webTestClient
      .delete()
      .uri("/draft/court-appearance/${draftCourtCase.draftAppearances.first().draftUuid}")
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
    webTestClient
      .delete()
      .uri("/draft/court-appearance/${draftCourtCase.draftAppearances.first().draftUuid}")
      .headers {
        it.authToken(roles = listOf("ROLE_OTHER_FUNCTION"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isForbidden
  }
}
