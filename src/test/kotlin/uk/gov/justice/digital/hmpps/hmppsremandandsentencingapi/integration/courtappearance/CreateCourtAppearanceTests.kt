package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.courtappearance

import org.hamcrest.text.MatchesPattern
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateCharge
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateCourtAppearance
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateFineAmount
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreatePeriodLength
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateSentence
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.legacy.ChargeLegacyData
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.legacy.CourtAppearanceLegacyData
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.PeriodLengthType
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class CreateCourtAppearanceTests : IntegrationTestBase() {

  @Test
  fun `create appearance in existing court case`() {
    val courtCase = createCourtCase()
    val sentence = CreateSentence(
      null, "1", listOf(CreatePeriodLength(1, null, null, null, "years", PeriodLengthType.SENTENCE_LENGTH)), "FORTHWITH", null, null, UUID.fromString("1104e683-5467-4340-b961-ff53672c4f39"), LocalDate.now().minusDays(7),
      CreateFineAmount(BigDecimal.valueOf(15.50)),
    )
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
    val appearance = CreateCourtAppearance(
      courtCase.first, UUID.randomUUID(), null, "COURT1", "GH123456789", LocalDate.now(), null, "REMAND", 1, null, null, listOf(charge), LocalDate.now().minusDays(7),
      CourtAppearanceLegacyData(
        "1",
        "1",
        "10-10-2015",
        "1116",
        "A NOMIS outcome description",
        LocalDate.now().plusDays(4),
        LocalDateTime.now().plusDays(4).withHour(10).withMinute(0).withSecond(0).withNano(0),
      ),
    )
    webTestClient
      .post()
      .uri("/court-appearance")
      .bodyValue(appearance)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isCreated
      .expectBody()
      .jsonPath("$.appearanceUuid")
      .value(MatchesPattern.matchesPattern("([a-f0-9]{8}(-[a-f0-9]{4}){4}[a-f0-9]{8})"))
  }

  @Test
  fun `create appearance with consecutive to sentences`() {
    val courtCase = createCourtCase()
    val forthWithSentence = CreateSentence(null, "1", listOf(CreatePeriodLength(1, null, null, null, "years", PeriodLengthType.SENTENCE_LENGTH)), "FORTHWITH", null, null, UUID.fromString("1104e683-5467-4340-b961-ff53672c4f39"), LocalDate.now().minusDays(7), null)
    val forthWithCharge = CreateCharge(null, UUID.randomUUID(), "OFF123", LocalDate.now(), null, UUID.fromString("f17328cf-ceaa-43c2-930a-26cf74480e18"), true, forthWithSentence, null)
    val concurrentSentence = CreateSentence(null, "2", listOf(CreatePeriodLength(1, null, null, null, "years", PeriodLengthType.SENTENCE_LENGTH)), "CONCURRENT", null, null, UUID.fromString("1104e683-5467-4340-b961-ff53672c4f39"), LocalDate.now().minusDays(7), null)
    val concurrentCharge = CreateCharge(null, UUID.randomUUID(), "OFF123", LocalDate.now(), null, UUID.fromString("f17328cf-ceaa-43c2-930a-26cf74480e18"), true, concurrentSentence, null)
    val consecutiveToSentence = CreateSentence(null, "3", listOf(CreatePeriodLength(1, null, null, null, "years", PeriodLengthType.SENTENCE_LENGTH)), "CONSECUTIVE_TO", "1", null, UUID.fromString("1104e683-5467-4340-b961-ff53672c4f39"), LocalDate.now().minusDays(7), null)
    val consecutiveToCharge = CreateCharge(null, UUID.randomUUID(), "OFF123", LocalDate.now(), null, UUID.fromString("f17328cf-ceaa-43c2-930a-26cf74480e18"), true, consecutiveToSentence, null)
    val appearance = CreateCourtAppearance(
      courtCase.first, UUID.randomUUID(), UUID.fromString("62412083-9892-48c9-bf01-7864af4a8b3c"), "COURT1", "GH123456789", LocalDate.now(), null, "SENTENCE", 1,
      CreatePeriodLength(
        6,
        null,
        null,
        null,
        "years",
        PeriodLengthType.OVERALL_SENTENCE_LENGTH,
      ),
      null, listOf(consecutiveToCharge, concurrentCharge, forthWithCharge), LocalDate.now().minusDays(7), null,
    )
    webTestClient
      .post()
      .uri("/court-appearance")
      .bodyValue(appearance)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isCreated
      .expectBody()
      .jsonPath("$.appearanceUuid")
      .value(MatchesPattern.matchesPattern("([a-f0-9]{8}(-[a-f0-9]{4}){4}[a-f0-9]{8})"))
  }

  @Test
  fun `must not create appearance when no court case exists`() {
    val charge = CreateCharge(null, UUID.randomUUID(), "OFF123", LocalDate.now(), null, UUID.fromString("f17328cf-ceaa-43c2-930a-26cf74480e18"), null, null, null)
    val appearance = CreateCourtAppearance(UUID.randomUUID().toString(), UUID.randomUUID(), UUID.fromString("62412083-9892-48c9-bf01-7864af4a8b3c"), "COURT1", "GH123456789", LocalDate.now(), null, "REMAND", 1, null, null, listOf(charge), null, null)
    webTestClient
      .post()
      .uri("/court-appearance")
      .bodyValue(appearance)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isNotFound
  }

  @Test
  fun `no token results in unauthorized`() {
    val charge = CreateCharge(null, UUID.randomUUID(), "OFF123", LocalDate.now(), null, UUID.fromString("f17328cf-ceaa-43c2-930a-26cf74480e18"), null, null, null)
    val appearance = CreateCourtAppearance(UUID.randomUUID().toString(), UUID.randomUUID(), UUID.fromString("62412083-9892-48c9-bf01-7864af4a8b3c"), "COURT1", "GH123456789", LocalDate.now(), null, "REMAND", 1, null, null, listOf(charge), null, null)
    webTestClient
      .post()
      .uri("/court-appearance")
      .bodyValue(appearance)
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
    val appearance = CreateCourtAppearance(UUID.randomUUID().toString(), UUID.randomUUID(), UUID.fromString("62412083-9892-48c9-bf01-7864af4a8b3c"), "COURT1", "GH123456789", LocalDate.now(), null, "REMAND", 1, null, null, listOf(charge), null, null)
    webTestClient
      .post()
      .uri("/court-appearance")
      .bodyValue(appearance)
      .headers {
        it.authToken(roles = listOf("ROLE_OTHER_FUNCTION"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isForbidden
  }
}
