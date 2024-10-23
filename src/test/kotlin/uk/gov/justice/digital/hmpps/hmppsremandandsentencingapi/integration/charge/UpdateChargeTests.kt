package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.charge

import org.assertj.core.api.Assertions
import org.hamcrest.text.MatchesPattern
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateCharge
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreatePeriodLength
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateSentence
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.legacy.ChargeLegacyData
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.PeriodLengthType
import java.time.LocalDate
import java.util.UUID

class UpdateChargeTests : IntegrationTestBase() {

  @Test
  fun `can update charge supplying appearance uuid`() {
    val courtCase = createCourtCase()
    val createdAppearance = courtCase.second.appearances.first()
    val sentence = CreateSentence(null, "1", listOf(CreatePeriodLength(1, null, null, null, "years", PeriodLengthType.SENTENCE_LENGTH)), "FORTHWITH", null, null, UUID.fromString("1104e683-5467-4340-b961-ff53672c4f39"), LocalDate.now().minusDays(7))
    val charge = CreateCharge(
      createdAppearance.appearanceUuid,
      createdAppearance.charges.first().chargeUuid,
      "OFF123",
      LocalDate.now(),
      null,
      null,
      true,
      sentence,
      ChargeLegacyData("1", "1", "10-10-2015", "1116", "A NOMIS charge outcome description"),
    )
    webTestClient
      .put()
      .uri("/charge/${charge.chargeUuid}")
      .bodyValue(charge)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_CHARGE_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.chargeUuid")
      .value(MatchesPattern.matchesPattern("([a-f0-9]{8}(-[a-f0-9]{4}){4}[a-f0-9]{8})"))
    val messages = getMessages(2)
    Assertions.assertThat(messages).hasSize(2).extracting<String> { it.eventType }.contains("charge.updated")
  }

  @Test
  fun `can update charge without supplying appearance uuid and charge already being in a court case`() {
    val courtCase = createCourtCase()
    val createdAppearance = courtCase.second.appearances.first()
    val sentence = CreateSentence(null, "1", listOf(CreatePeriodLength(1, null, null, null, "years", PeriodLengthType.SENTENCE_LENGTH)), "FORTHWITH", null, null, UUID.fromString("1104e683-5467-4340-b961-ff53672c4f39"), LocalDate.now().minusDays(7))
    val charge = CreateCharge(
      null,
      createdAppearance.charges.first().chargeUuid,
      "OFF123",
      LocalDate.now(),
      null,
      null,
      true,
      sentence,
      ChargeLegacyData("1", "1", "10-10-2015", "1116", "A NOMIS charge outcome description"),
    )
    webTestClient
      .put()
      .uri("/charge/${charge.chargeUuid}")
      .bodyValue(charge)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_CHARGE_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.chargeUuid")
      .value(MatchesPattern.matchesPattern("([a-f0-9]{8}(-[a-f0-9]{4}){4}[a-f0-9]{8})"))
  }

  @Test
  fun `cannot create a charge without court appearance and court case`() {
    val charge = CreateCharge(
      null,
      UUID.randomUUID(),
      "OFF123",
      LocalDate.now(),
      null,
      null,
      true,
      null,
      ChargeLegacyData("1", "1", "10-10-2015", "1116", "A NOMIS charge outcome description"),
    )
    webTestClient
      .put()
      .uri("/charge/${charge.chargeUuid}")
      .bodyValue(charge)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_CHARGE_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isBadRequest
  }

  @Test
  fun `no token results in unauthorized`() {
    val charge = CreateCharge(null, UUID.randomUUID(), "OFF123", LocalDate.now(), null, UUID.fromString("f17328cf-ceaa-43c2-930a-26cf74480e18"), null, null, null)
    webTestClient
      .put()
      .uri("/charge/${charge.chargeUuid}")
      .bodyValue(charge)
      .headers {
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `token with incorrect role is forbidden`() {
    val charge = CreateCharge(null, UUID.randomUUID(), "OFF123", LocalDate.now(), null, UUID.fromString("f17328cf-ceaa-43c2-930a-26cf74480e18"), null, null, null)
    webTestClient
      .put()
      .uri("/charge/${charge.chargeUuid}")
      .bodyValue(charge)
      .headers {
        it.authToken(roles = listOf("ROLE_OTHER_FUNCTION"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isForbidden
  }
}
