package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.charge

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import java.time.format.DateTimeFormatter
import java.util.UUID

class GetChargeTests : IntegrationTestBase() {

  @Test
  fun `get charge by uuid`() {
    val createdCharge = createCourtCase().second.appearances.first().charges.first()
    webTestClient
      .get()
      .uri("/charge/${createdCharge.chargeUuid!!}")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING"))
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.chargeUuid")
      .isEqualTo(createdCharge.chargeUuid.toString())
      .jsonPath("$.offenceCode")
      .isEqualTo(createdCharge.offenceCode)
      .jsonPath("$.offenceStartDate")
      .isEqualTo(createdCharge.offenceStartDate.format(DateTimeFormatter.ISO_DATE))
      .jsonPath("$.outcome.outcomeUuid")
      .isEqualTo(createdCharge.outcomeUuid!!.toString())
      .jsonPath("$.sentence.sentenceType.sentenceTypeUuid")
      .isEqualTo(createdCharge.sentence!!.sentenceTypeId.toString())
  }

  @Test
  fun `no charge exist for uuid results in not found`() {
    webTestClient
      .get()
      .uri("/charge/${UUID.randomUUID()}")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING"))
      }
      .exchange()
      .expectStatus()
      .isNotFound
  }

  @Test
  fun `no token results in unauthorized`() {
    val createdCharge = createCourtCase().second.appearances.first().charges.first()
    webTestClient
      .get()
      .uri("/charge/${createdCharge.chargeUuid!!}")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `token with incorrect role is forbidden`() {
    val createdCharge = createCourtCase().second.appearances.first().charges.first()
    webTestClient
      .get()
      .uri("/charge/${createdCharge.chargeUuid!!}")
      .headers {
        it.authToken(roles = listOf("ROLE_OTHER_FUNCTION"))
      }
      .exchange()
      .expectStatus()
      .isForbidden
  }
}
