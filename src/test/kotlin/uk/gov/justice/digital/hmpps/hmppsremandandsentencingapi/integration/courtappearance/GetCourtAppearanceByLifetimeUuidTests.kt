package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.courtappearance

import org.hamcrest.text.MatchesPattern
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateCharge
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateCourtAppearance
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreatePeriodLength
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateSentence
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.PeriodLengthType
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

class GetCourtAppearanceByLifetimeUuidTests : IntegrationTestBase() {

  @Test
  fun `get appearance by lifetime uuid`() {
    val courtCase = createCourtCase()
    val createdAppearance = courtCase.second.appearances.first()
    val sentence = CreateSentence(null, "1", listOf(CreatePeriodLength(1, null, null, null, "years", PeriodLengthType.SENTENCE_LENGTH)), "FORTHWITH", null, null, UUID.fromString("1104e683-5467-4340-b961-ff53672c4f39"), LocalDate.now().minusDays(7), null)
    val charge = CreateCharge(null, UUID.randomUUID(), "OFF123", LocalDate.now(), null, UUID.fromString("f17328cf-ceaa-43c2-930a-26cf74480e18"), null, sentence, null)
    val updatedAppearance = CreateCourtAppearance(courtCase.first, createdAppearance.appearanceUuid, createdAppearance.lifetimeUuid, UUID.fromString("62412083-9892-48c9-bf01-7864af4a8b3c"), "COURT1", "ADIFFERENTCOURTCASEREFERENCE", LocalDate.now(), null, "REMAND", 1, null, null, listOf(charge), LocalDate.now().minusDays(7), null)
    updateAppearance(updatedAppearance)
    webTestClient
      .get()
      .uri("/court-appearance/${createdAppearance.lifetimeUuid}/lifetime")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_APPEARANCE_RO"))
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.lifetimeUuid")
      .isEqualTo(createdAppearance.lifetimeUuid.toString())
      .jsonPath("$.courtCaseReference")
      .isEqualTo(updatedAppearance.courtCaseReference!!)
      .jsonPath("$.appearanceDate")
      .isEqualTo(updatedAppearance.appearanceDate.format(DateTimeFormatter.ISO_DATE))
      .jsonPath("$.outcome.outcomeUuid")
      .isEqualTo(updatedAppearance.outcomeUuid.toString())
      .jsonPath("$.warrantType")
      .isEqualTo(updatedAppearance.warrantType)
      .jsonPath("$.taggedBail")
      .isEqualTo(updatedAppearance.taggedBail!!)
      .jsonPath("$.charges[0].sentence.chargeNumber")
      .isEqualTo(updatedAppearance.charges[0].sentence!!.chargeNumber)
  }

  @Test
  fun `no appearance exist for uuid results in not found`() {
    webTestClient
      .get()
      .uri("/court-appearance/${UUID.randomUUID()}/lifetime")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_APPEARANCE_RO"))
      }
      .exchange()
      .expectStatus()
      .isNotFound
  }

  @Test
  fun `no token results in unauthorized`() {
    val createdAppearance = createCourtCase().second.appearances.first()
    webTestClient
      .get()
      .uri("/court-appearance/${createdAppearance.lifetimeUuid}/lifetime")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `token with incorrect role is forbidden`() {
    val createdAppearance = createCourtCase().second.appearances.first()
    webTestClient
      .get()
      .uri("/court-appearance/${createdAppearance.lifetimeUuid}/lifetime")
      .headers {
        it.authToken(roles = listOf("ROLE_OTHER_FUNCTION"))
      }
      .exchange()
      .expectStatus()
      .isForbidden
  }

  private fun updateAppearance(courtAppearance: CreateCourtAppearance) {
    webTestClient
      .put()
      .uri("/court-appearance/${courtAppearance.appearanceUuid}")
      .bodyValue(courtAppearance)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.appearanceUuid")
      .value(MatchesPattern.matchesPattern("([a-f0-9]{8}(-[a-f0-9]{4}){4}[a-f0-9]{8})"))
  }
}
