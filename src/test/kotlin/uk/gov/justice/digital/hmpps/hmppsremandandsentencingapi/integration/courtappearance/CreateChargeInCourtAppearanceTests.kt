package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.courtappearance

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

class CreateChargeInCourtAppearanceTests : IntegrationTestBase() {

  @Test
  fun `create charge in existing court appearance`() {
    val courtCase = createCourtCase()
    val sentence = CreateSentence(null, "1", listOf(CreatePeriodLength(1, null, null, null, "years", PeriodLengthType.SENTENCE_LENGTH)), "FORTHWITH", null, null, UUID.fromString("1104e683-5467-4340-b961-ff53672c4f39"), LocalDate.now().minusDays(7), null)
    val charge = CreateCharge(
      null,
      UUID.randomUUID(),
      "OFF123",
      LocalDate.now(),
      null,
      null,
      true,
      sentence,
      ChargeLegacyData("1", "1", "10-10-2015", "1116", "A NOMIS charge outcome description"),
    )
    webTestClient
      .post()
      .uri("/court-appearance/${courtCase.second.appearances.first().appearanceUuid!!}/charge")
      .bodyValue(charge)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_CHARGE_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isCreated
      .expectBody()
      .jsonPath("$.chargeUuid")
      .value(MatchesPattern.matchesPattern("([a-f0-9]{8}(-[a-f0-9]{4}){4}[a-f0-9]{8})"))
  }

  @Test
  fun `must not create charge when no court appearance exists`() {
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
      .post()
      .uri("/court-appearance/${UUID.randomUUID()}/charge")
      .bodyValue(charge)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_CHARGE_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isNotFound
  }

  @Test
  fun `no token results in unauthorized`() {
    val courtCase = createCourtCase()
    val charge = CreateCharge(null, UUID.randomUUID(), "OFF123", LocalDate.now(), null, UUID.fromString("f17328cf-ceaa-43c2-930a-26cf74480e18"), null, null, null)
    webTestClient
      .post()
      .uri("/court-appearance/${courtCase.second.appearances.first().appearanceUuid!!}/charge")
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
    val courtCase = createCourtCase()
    val charge = CreateCharge(null, UUID.randomUUID(), "OFF123", LocalDate.now(), null, UUID.fromString("f17328cf-ceaa-43c2-930a-26cf74480e18"), null, null, null)
    webTestClient
      .post()
      .uri("/court-appearance/${courtCase.second.appearances.first().appearanceUuid!!}/charge")
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
