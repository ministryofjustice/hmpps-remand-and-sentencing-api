package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.chargeoutcome

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.core.ParameterizedTypeReference
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.ChargeOutcome
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase

class GetChargeOutcomesByUuidsTests : IntegrationTestBase() {

  private inline fun <reified T> typeReference() = object : ParameterizedTypeReference<T>() {}

  @Test
  fun `providing no parameters results in bad request`() {
    webTestClient.get()
      .uri("/charge-outcome/uuid/multiple")
      .headers { it.authToken() }
      .exchange()
      .expectStatus()
      .isBadRequest
  }

  @Test
  fun `retrieve multiple sentence types by uuids`() {
    val result = webTestClient.get()
      .uri("/charge-outcome/uuid/multiple?uuids=f17328cf-ceaa-43c2-930a-26cf74480e18,315280e5-d53e-43b3-8ba6-44da25676ce2")
      .headers { it.authToken() }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody(typeReference<List<ChargeOutcome>>())
      .returnResult().responseBody!!

    val descriptions = result.map { it.outcomeName }
    Assertions.assertThat(descriptions).containsExactlyInAnyOrderElementsOf(listOf("Imprisonment", "Remand in custody"))
  }
}
