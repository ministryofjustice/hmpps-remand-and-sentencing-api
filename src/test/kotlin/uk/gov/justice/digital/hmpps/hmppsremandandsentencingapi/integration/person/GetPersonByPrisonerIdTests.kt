package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.person

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase

class GetPersonByPrisonerIdTests : IntegrationTestBase() {

  @Test
  fun `get person by prisoner id`() {
    webTestClient.get()
      .uri("/person/{prisonerId}", "A1234AB")
      .headers { it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING", "ROLE_RELEASE_DATES_CALCULATOR")) }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.personId")
      .isEqualTo("A1234AB")
      .jsonPath("$.firstName")
      .isEqualTo("Marvin")
      .jsonPath("$.lastName")
      .isEqualTo("Haggler")
      .jsonPath("$.establishment")
      .isEqualTo("HMP Bedford")
      .jsonPath("$.cellNumber")
      .isEqualTo("CELL-1")
      .jsonPath("$.dateOfBirth")
      .isEqualTo("1965-02-03")
      .jsonPath("$.pncNumber")
      .isEqualTo("1231/XX/121")
      .jsonPath("$.status")
      .isEqualTo("REMAND")
  }
}
