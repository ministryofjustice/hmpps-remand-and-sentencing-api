package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.subjectaccessrequest

import org.junit.jupiter.api.Test
import org.mockito.kotlin.whenever
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.subjectaccessrequest.Prisoner
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.subjectaccessrequest.util.ExpectResponseData.emptyNotInNomisResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.subjectaccessrequest.util.ExpectResponseData.validNotInNomisResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.subjectaccessrequest.util.PrisonerTestData
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.subjectaccessrequest.PrisonerDetailsService

@ActiveProfiles("sar", "test")
class GetSarContentByReferenceTests : IntegrationTestBase() {

  @MockitoBean
  lateinit var prisonerDetailsService: PrisonerDetailsService<Prisoner>

  @Test
  fun `get immigrationDetentions by valid prisoner id`() {
    whenever(
      prisonerDetailsService
        .getPrisonerDetails("A6764DZ", null, null),
    ).thenReturn(PrisonerTestData.prisoner())
    webTestClient
      .get()
      .uri { uriBuilder ->
        uriBuilder
          .path("/subject-access-request")
          .queryParam("prn", "A6764DZ")
          .build()
      }
      .headers {
        it.authToken(roles = listOf("ROLE_SAR_DATA_ACCESS"))
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .json(validNotInNomisResponse())
  }

  @Test
  fun `get empty immigrationDetentions by valid prisoner id with no data yet associated`() {
    whenever(
      prisonerDetailsService
        .getPrisonerDetails("foo-bar", null, null),
    ).thenReturn(Prisoner(prisonerNumber = "foo-bar"))
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
      .json(emptyNotInNomisResponse())
  }

  @Test
  fun `get 209 by attempting use of crn`() {
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

  @Test
  fun `get 400 when query not properly formed`() {
    webTestClient
      .get()
      .uri { uriBuilder ->
        uriBuilder
          .path("/subject-access-request")
          .queryParam("invalid-param", "null")
          .build()
      }
      .headers {
        it.authToken(roles = listOf("ROLE_SAR_DATA_ACCESS"))
      }
      .exchange()
      .expectStatus()
      .isEqualTo(400)
  }

  @Test
  fun `get 401 when auth token missing or not valid`() {
    webTestClient
      .get()
      .uri { uriBuilder ->
        uriBuilder
          .path("/subject-access-request")
          .queryParam("invalid-param", "null")
          .build()
      }
      .exchange()
      .expectStatus()
      .isEqualTo(401)
  }

  @Test
  fun `get 403 when auth token role not SAR_DATA_ACCESS`() {
    webTestClient
      .get()
      .uri { uriBuilder ->
        uriBuilder
          .path("/subject-access-request")
          .queryParam("invalid-param", "null")
          .build()
      }
      .headers {
        it.authToken(roles = listOf("ROLE_LIMITED_ACCESS"))
      }
      .exchange()
      .expectStatus()
      .isEqualTo(403)
  }

  @Test
  fun `get 500 when internal exception thrown`() {
    whenever(
      prisonerDetailsService
        .getPrisonerDetails("foo-bar", null, null),
    ).thenThrow(RuntimeException::class.java)
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
      .isEqualTo(500)
  }
}
