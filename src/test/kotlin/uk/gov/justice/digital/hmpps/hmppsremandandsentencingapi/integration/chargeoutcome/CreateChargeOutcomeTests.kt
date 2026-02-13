package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.chargeoutcome

import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.DpsDataCreator

class CreateChargeOutcomeTests : IntegrationTestBase() {

  @Test
  fun `create charge outcome`() {
    val createChargeOutcome = DpsDataCreator.createChargeOutcome()
    webTestClient.post()
      .uri("/charge-outcome")
      .bodyValue(createChargeOutcome)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus().isCreated
      .expectBody()
      .jsonPath("$.outcomeName")
      .isEqualTo(createChargeOutcome.outcomeName)
      .jsonPath("$.nomisCode")
      .isEqualTo(createChargeOutcome.nomisCode)
  }

  @Test
  fun `trying to create a charge outcome with a type not currently in the database results in error`() {
    val createChargeOutcome = DpsDataCreator.createChargeOutcome(outcomeType = "SOME_RANDOM_TYPE")
    webTestClient.post()
      .uri("/charge-outcome")
      .bodyValue(createChargeOutcome)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus().isBadRequest
      .expectBody()
      .jsonPath("$.fieldErrors[0].field")
      .isEqualTo("outcomeType")
      .jsonPath("$.fieldErrors[0].message")
      .isEqualTo("Must use one of existing the outcome types SENTENCING, NON_CUSTODIAL, IMMIGRATION, REMAND")
  }

  @Test
  fun `trying to create a charge outcome with a disposition code not currently in the database results in error`() {
    val createChargeOutcome = DpsDataCreator.createChargeOutcome(dispositionCode = "SOME_RANDOM_CODE")
    webTestClient.post()
      .uri("/charge-outcome")
      .bodyValue(createChargeOutcome)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus().isBadRequest
      .expectBody()
      .jsonPath("$.fieldErrors[0].field")
      .isEqualTo("dispositionCode")
      .jsonPath("$.fieldErrors[0].message")
      .isEqualTo("Must use one of existing the disposition codes FINAL, INTERIM")
  }
}
