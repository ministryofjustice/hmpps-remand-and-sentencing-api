package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.hmctsresultcode

import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.DpsDataCreator
import java.util.UUID

class UpdateHmctsResultCodeTests : IntegrationTestBase() {
  @Test
  fun `update hmcts result code`() {
    val createHmctsResultCode = DpsDataCreator.createHmctsResultCode(
      resultUuid = UUID.randomUUID(),
    )
    webTestClient.post()
      .uri("/hmcts-result-code")
      .bodyValue(createHmctsResultCode)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus().isCreated

    val updateHmctsResultCode = createHmctsResultCode.copy(resultName = "A different Result Name")
    webTestClient.put()
      .uri("/hmcts-result-code/${createHmctsResultCode.resultUuid}")
      .bodyValue(updateHmctsResultCode)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.resultName")
      .isEqualTo(updateHmctsResultCode.resultName)
  }

  @Test
  fun `trying to update a hmcts result code with a result code that is already mapped results in error`() {
    val createHmctsResultCode = DpsDataCreator.createHmctsResultCode(
      resultUuid = UUID.randomUUID(),
    )
    webTestClient.post()
      .uri("/hmcts-result-code")
      .bodyValue(createHmctsResultCode)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus().isCreated

    val updateHmctsResultCode = createHmctsResultCode.copy(resultCode = "RI")
    webTestClient.put()
      .uri("/hmcts-result-code/${createHmctsResultCode.resultUuid}")
      .bodyValue(updateHmctsResultCode)
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
