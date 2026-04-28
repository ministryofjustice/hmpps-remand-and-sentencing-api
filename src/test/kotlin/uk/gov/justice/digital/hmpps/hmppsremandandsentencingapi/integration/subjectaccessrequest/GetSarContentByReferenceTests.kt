package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.subjectaccessrequest

import org.junit.jupiter.api.Test
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase

@ActiveProfiles("sar", "test")
class GetSarContentByReferenceTests : IntegrationTestBase() {

  @Test
  fun `get court cases by invalid prisoner id`() {
    createCourtCase()
    webTestClient
      .get()
      .uri { uriBuilder ->
        uriBuilder
          .path("/subject-access-request")
          .queryParam("prn", "foo-bar")
          .build()
      }
      .headers {
        it.authToken(roles = listOf("ROLE_SAR_DATA_ACCESS"))
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .consumeWith(::println)
      .json(
        """
        {
          "attachments": [],
          "content": {
            "prisonerNumber": "foo-bar",
            "prisonerName": "No Data Held",
            "immigrationDetentions": []
          }
        }
        """.trimIndent(),
      )
  }
}
