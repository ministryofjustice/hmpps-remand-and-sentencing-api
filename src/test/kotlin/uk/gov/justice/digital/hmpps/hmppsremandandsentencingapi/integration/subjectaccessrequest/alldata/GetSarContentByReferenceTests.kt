package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.subjectaccessrequest.alldata

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.whenever
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.subjectaccessrequest.alldata.AllDataPrisoner
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.subjectaccessrequest.alldata.util.ExpectResponseData.emptyFullDataResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.subjectaccessrequest.alldata.util.ExpectResponseData.validFullDataResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.subjectaccessrequest.alldata.util.PrisonerTestData
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.subjectaccessrequest.PrisonerDetailsService

@ActiveProfiles("sar", "test", "test-all-sar-data")
@DisplayName("GetSarContentByReferenceTests (All Data)")
class GetSarContentByReferenceTests : IntegrationTestBase() {

  @MockitoBean
  lateinit var allDataPrisonerDetailsService: PrisonerDetailsService<AllDataPrisoner>

  @Test
  fun `get immigrationDetentions by valid prisoner id`() {
    whenever(
      allDataPrisonerDetailsService
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
      .json(validFullDataResponse())
  }

  @Test
  fun `get empty court cases, recalls & immigrationDetentions by valid prisoner id with no data yet associated`() {
    whenever(
      allDataPrisonerDetailsService
        .getPrisonerDetails("foo-bar", null, null),
    ).thenReturn(AllDataPrisoner(prisonerNumber = "foo-bar"))
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
      .json(emptyFullDataResponse())
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
      allDataPrisonerDetailsService
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
