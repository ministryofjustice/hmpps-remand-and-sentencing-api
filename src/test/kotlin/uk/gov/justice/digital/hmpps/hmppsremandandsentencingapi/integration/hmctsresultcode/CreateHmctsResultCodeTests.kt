package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.hmctsresultcode

import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.DpsDataCreator

class CreateHmctsResultCodeTests : IntegrationTestBase() {

  @Test
  fun `create hmcts result code`() {
    val createHmctsResultCode = DpsDataCreator.createHmctsResultCode()
    webTestClient.post()
      .uri("/hmcts-result-code")
      .bodyValue(createHmctsResultCode)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus().isCreated
      .expectBody()
      .jsonPath("$.resultName")
      .isEqualTo(createHmctsResultCode.resultName)
      .jsonPath("$.resultCode")
      .isEqualTo(createHmctsResultCode.resultCode)
  }

  @Test
  fun `adding a hmcts result code with a blank name results in error`() {
    val createHmctsResultCode = DpsDataCreator.createHmctsResultCode(resultName = "")
    webTestClient.post()
      .uri("/hmcts-result-code")
      .bodyValue(createHmctsResultCode)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus().isBadRequest
      .expectBody()
      .jsonPath("$.fieldErrors[0].field")
      .isEqualTo("resultName")
      .jsonPath("$.fieldErrors[0].message")
      .isEqualTo("Result name must not be blank")
  }

  @Test
  fun `trying to create a hmcts result code with a NOMIS code that is already mapped results in error`() {
    val createHmctsResultCode = DpsDataCreator.createHmctsResultCode()
    webTestClient.post()
      .uri("/hmcts-result-code")
      .bodyValue(createHmctsResultCode)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus().isCreated
    webTestClient.post()
      .uri("/hmcts-result-code")
      .bodyValue(createHmctsResultCode)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus().isBadRequest
      .expectBody()
      .jsonPath("$.fieldErrors[0].field")
      .isEqualTo("resultCode")
      .jsonPath("$.fieldErrors[0].message")
      .isEqualTo("resultCode HMCTS result code is already mapped")
  }
}
