package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.courtcase

import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import java.util.stream.LongStream

class SearchCourtCaseTests : IntegrationTestBase() {

  @Test
  fun `return all court cases associated with a prisoner id`() {
    val createdCourtCase = createCourtCase()
    webTestClient.get()
      .uri {
        it.path("/court-case/search")
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
      .jsonPath("$.content.[0].courtCaseUuid")
      .isEqualTo(createdCourtCase.second)
      .jsonPath("$.content.[*].prisonerId")
      .isEqualTo(createdCourtCase.first)
  }

  @Test
  fun `must only return court cases associated with prisoner id`() {
    val expectedCourtCase = createCourtCase()
    val otherCourtCase = createCourtCase("OTHERPRISONER")
    webTestClient.get()
      .uri {
        it.path("/court-case/search")
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
      .jsonPath("$.content.[0].courtCaseUuid")
      .isEqualTo(expectedCourtCase.second)
      .jsonPath("$.content.[*].prisonerId")
      .isEqualTo(expectedCourtCase.first)
      .jsonPath("$.content.[?(@.courtCaseUuid == '${otherCourtCase.second}')]")
      .doesNotExist()
      .jsonPath("$.content.[?(@.prisonerId == '${otherCourtCase.first}')]")
      .doesNotExist()
  }

  // The default size is 20
  @Test
  fun `return paged results and not all results`() {
    val courtCases = LongStream.range(0, 100).mapToObj { createCourtCase() }.toList()
    webTestClient.get()
      .uri {
        it.path("/court-case/search")
          .queryParam("prisonerId", courtCases.first().first)
          .build()
      }
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING"))
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.content.length()")
      .isEqualTo(20)
  }

  @Test
  fun `sort by latest appearance date`() {
    val courtCases = LongStream.range(0, 100).mapToObj { createCourtCase(minusDaysFromAppearanceDate = it) }.toList()
    webTestClient.get()
      .uri {
        it.path("/court-case/search")
          .queryParam("prisonerId", courtCases.first().first)
          .queryParam("sort", "latestCourtAppearance_appearanceDate,desc")
          .build()
      }
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING"))
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.content.[0].courtCaseUuid")
      .isEqualTo(courtCases[0].second)
      .jsonPath("$.content.[1].courtCaseUuid")
      .isEqualTo(courtCases[1].second)
  }

  @Test
  fun `no token results in unauthorized`() {
    webTestClient.get()
      .uri {
        it.path("/court-case/search")
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
        it.path("/court-case/search")
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
}
