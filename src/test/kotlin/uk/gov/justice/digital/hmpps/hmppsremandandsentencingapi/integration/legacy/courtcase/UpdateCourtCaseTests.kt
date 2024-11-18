package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.courtcase

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.util.DataCreator
import java.util.UUID

class UpdateCourtCaseTests : IntegrationTestBase() {

  @Test
  fun `update court case`() {
    val createdCourtCase = createLegacyCourtCase()
    val toUpdate = DataCreator.legacyCreateCourtCase(active = false)
    webTestClient
      .put()
      .uri("/legacy/court-case/${createdCourtCase.first}")
      .bodyValue(toUpdate)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_COURT_CASE_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isOk

    val message = getMessages(1)[0]
    Assertions.assertThat(message.eventType).isEqualTo("court-case.updated")
    Assertions.assertThat(message.additionalInformation.get("source").asText()).isEqualTo("NOMIS")
  }

  @Test
  fun `no token results in unauthorized`() {
    val legacyCreateCourtCase = DataCreator.legacyCreateCourtCase()
    webTestClient
      .put()
      .uri("/legacy/court-case/${UUID.randomUUID()}")
      .bodyValue(legacyCreateCourtCase)
      .headers {
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `token with incorrect role is forbidden`() {
    val legacyCreateCourtCase = DataCreator.legacyCreateCourtCase()
    webTestClient
      .put()
      .uri("/legacy/court-case/${UUID.randomUUID()}")
      .bodyValue(legacyCreateCourtCase)
      .headers {
        it.authToken(roles = listOf("ROLE_OTHER_FUNCTION"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isForbidden
  }
}
