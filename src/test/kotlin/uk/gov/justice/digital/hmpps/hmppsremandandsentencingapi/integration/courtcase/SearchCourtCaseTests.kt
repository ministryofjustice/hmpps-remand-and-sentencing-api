package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.courtcase

import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateCharge
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateCourtAppearance
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateCourtCase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateCourtCaseResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import java.time.LocalDate
import java.util.UUID

class SearchCourtCaseTests : IntegrationTestBase() {

  @Test
  fun `return all court cases associated with a prisoner id`() {
    val createdCourtCase = createCourtCase()
    webTestClient.get()
      .uri {
        it.path("/courtCase/search")
          .queryParam("prisonerId", createdCourtCase.first)
          .build()
      }
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING"))
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.[0].courtCaseUuid")
      .isEqualTo(createdCourtCase.second)
      .jsonPath("$.[*].prisonerId")
      .isEqualTo(createdCourtCase.first)
  }

  @Test
  fun `must only return court cases associated with prisoner id`() {
    val expectedCourtCase = createCourtCase()
    val otherCourtCase = createCourtCase("OTHERPRISONER")
    webTestClient.get()
      .uri {
        it.path("/courtCase/search")
          .queryParam("prisonerId", expectedCourtCase.first)
          .build()
      }
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING"))
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.[0].courtCaseUuid")
      .isEqualTo(expectedCourtCase.second)
      .jsonPath("$.[*].prisonerId")
      .isEqualTo(expectedCourtCase.first)
      .jsonPath("$.[?(@.courtCaseUuid == '${otherCourtCase.second}')]")
      .doesNotExist()
      .jsonPath("$.[?(@.prisonerId == '${otherCourtCase.first}')]")
      .doesNotExist()
  }

  @Test
  fun `no token results in unauthorized`() {
    webTestClient.get()
      .uri {
        it.path("/courtCase/search")
          .queryParam("prisonerId", "PRISONER_ID")
          .build()
      }
      .headers {
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `token with incorrect role is forbidden`() {
    webTestClient.get()
      .uri {
        it.path("/courtCase/search")
          .queryParam("prisonerId", "PRISONER_ID")
          .build()
      }
      .headers {
        it.authToken(roles = listOf("ROLE_OTHER_FUNCTION"))
      }
      .exchange()
      .expectStatus()
      .isForbidden
  }

  fun createCourtCase(prisonerId: String = "PRI123"): Pair<String, String> {
    val charge = CreateCharge(UUID.randomUUID(), "OFF123", LocalDate.now(), null, "OUT123")
    val appearance = CreateCourtAppearance(UUID.randomUUID(), "OUT123", "COURT1", "GH123456789", LocalDate.now(), null, null, listOf(charge))
    val courtCase = CreateCourtCase(prisonerId, listOf(appearance))
    val response = webTestClient
      .post()
      .uri("/courtCase")
      .bodyValue(courtCase)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isCreated.returnResult(CreateCourtCaseResponse::class.java)
      .responseBody.blockFirst()!!
    return courtCase.prisonerId to response.courtCaseUuid
  }
}
