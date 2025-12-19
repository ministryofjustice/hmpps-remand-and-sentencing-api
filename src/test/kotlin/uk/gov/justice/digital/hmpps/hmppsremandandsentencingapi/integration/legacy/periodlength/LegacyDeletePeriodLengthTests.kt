package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.periodlength

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import java.util.UUID

class LegacyDeletePeriodLengthTests : IntegrationTestBase() {

  @Test
  fun `can delete period-length`() {
    val periodLengthUuid = createPeriodLength().first
    webTestClient
      .delete()
      .uri("/legacy/period-length/$periodLengthUuid")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_PERIOD_LENGTH_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isNoContent
    val message = getMessages(1)[0]
    assertThat(message.eventType).isEqualTo("sentence.period-length.deleted")
    assertThat(message.additionalInformation.get("source").asText()).isEqualTo("NOMIS")
  }

  @Test
  fun `can delete period length when charge unlinked to appearance`() {
    val (_, createdCourtCase) = createCourtCase()
    val createdAppearance = createdCourtCase.appearances.first()
    val createdCharge = createdAppearance.charges.first()
    val createdPeriodLength = createdCharge.sentence!!.periodLengths.first()
    webTestClient
      .delete()
      .uri("/legacy/court-appearance/${createdAppearance.appearanceUuid}/charge/${createdCharge.chargeUuid}")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_APPEARANCE_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isOk

    webTestClient
      .delete()
      .uri("/legacy/period-length/${createdPeriodLength.periodLengthUuid}")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_PERIOD_LENGTH_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isNoContent
  }

  @Test
  fun `no token results in unauthorized`() {
    webTestClient
      .delete()
      .uri("/legacy/period-length/${UUID.randomUUID()}")
      .headers {
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `token with incorrect role is forbidden`() {
    webTestClient
      .delete()
      .uri("/legacy/period-length/${UUID.randomUUID()}")
      .headers {
        it.authToken(roles = listOf("ROLE_OTHER_FUNCTION"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isForbidden
  }
}
