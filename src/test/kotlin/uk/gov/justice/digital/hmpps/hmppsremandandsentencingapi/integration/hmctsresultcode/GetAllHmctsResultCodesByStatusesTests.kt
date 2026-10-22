package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.hmctsresultcode

import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ReferenceEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.DpsDataCreator
import java.util.UUID

class GetAllHmctsResultCodesByStatusesTests : IntegrationTestBase() {

  @Test
  fun `return all active `() {
    webTestClient.get()
      .uri("/hmcts-result-code/status?statuses=ACTIVE")
      .headers { it.authToken() }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.[?(@.resultCode == 'RI')]")
      .exists()
      .jsonPath("$.[?(@.resultCode == 'DISM')]")
      .exists()
  }

  @Test
  fun `return all inactive results`() {
    val createHmctsResultCode = DpsDataCreator.createHmctsResultCode(
      resultUuid = UUID.randomUUID(),
      status = ReferenceEntityStatus.INACTIVE,
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

    webTestClient.get()
      .uri("/hmcts-result-code/status?statuses=INACTIVE")
      .headers { it.authToken() }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.[?(@.resultUuid == '${createHmctsResultCode.resultUuid}')]")
      .exists()
  }

  @Test
  fun `no token results in unauthorized`() {
    webTestClient.get()
      .uri("/hmcts-result-code/all")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }
}
