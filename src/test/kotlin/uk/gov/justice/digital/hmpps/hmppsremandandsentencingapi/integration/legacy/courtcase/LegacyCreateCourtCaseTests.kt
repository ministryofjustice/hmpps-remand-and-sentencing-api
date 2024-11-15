package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.courtcase

import org.assertj.core.api.Assertions
import org.hamcrest.text.MatchesPattern
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.util.DataCreator

class LegacyCreateCourtCaseTests : IntegrationTestBase() {

  @Test
  fun `Successfully create court case`() {
    val legacyCreateCourtCase = DataCreator.legacyCreateCourtCase()
    webTestClient
      .post()
      .uri("/legacy/court-case")
      .bodyValue(legacyCreateCourtCase)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_COURT_CASE_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isCreated
      .expectBody()
      .jsonPath("$.courtCaseUuid")
      .value(MatchesPattern.matchesPattern("([a-f0-9]{8}(-[a-f0-9]{4}){4}[a-f0-9]{8})"))
    val message = getMessages(1)[0]
    Assertions.assertThat(message.eventType).isEqualTo("court-case.inserted")
    Assertions.assertThat(message.additionalInformation.get("source").asText()).isEqualTo("NOMIS")
  }

  @Test
  fun `no token results in unauthorized`() {
    val legacyCreateCourtCase = DataCreator.legacyCreateCourtCase()
    webTestClient
      .post()
      .uri("/legacy/court-case")
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
      .post()
      .uri("/legacy/court-case")
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
