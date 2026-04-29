package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.subjectaccessrequest

import org.junit.jupiter.api.Test
import org.mockito.kotlin.whenever
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.subjectaccessrequest.Prisoner
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.subjectaccessrequest.PrisonerDetailsService

@ActiveProfiles("sar", "test")
class GetSarContentByReferenceTests : IntegrationTestBase() {

  @MockitoBean
  lateinit var prisonerDetailsService: PrisonerDetailsService<Prisoner>

  @Test
  fun `get empty immigrationDetentions by invalid prisoner id`() {
    createCourtCase()
    whenever(prisonerDetailsService
      .getPrisonerDetails("foo-bar", null, null)).thenReturn(Prisoner())
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

  @Test
  fun `get 209 by attempting use of crn`() {
    createCourtCase()
    webTestClient
      .get()
      .uri { uriBuilder ->
        uriBuilder
          .path("/subject-access-request")
          .queryParam("crn", "foo-bar")
          .build()
      }
      .headers {
        it.authToken(roles = listOf("ROLE_SAR_DATA_ACCESS"))
      }
      .exchange()
      .expectStatus()
      .isEqualTo(209)
  }

  @Test
  fun `get 204 when no data returned from service`() {
    createCourtCase()
    webTestClient
      .get()
      .uri { uriBuilder ->
        uriBuilder
          .path("/subject-access-request")
          .queryParam("prn", "null")
          .build()
      }
      .headers {
        it.authToken(roles = listOf("ROLE_SAR_DATA_ACCESS"))
      }
      .exchange()
      .expectStatus()
      .isEqualTo(204)
  }
}
