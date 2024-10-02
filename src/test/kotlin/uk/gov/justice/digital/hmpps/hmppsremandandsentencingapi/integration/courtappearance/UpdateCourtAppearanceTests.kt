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
import java.util.UUID

class UpdateCourtAppearanceTests : IntegrationTestBase() {

  @Test
  fun `update appearance in existing court case`() {
    val courtCase = createCourtCase()
    val sentence = CreateSentence(null, "1", listOf(CreatePeriodLength(1, null, null, null, "years", PeriodLengthType.SENTENCE_LENGTH)), "FORTHWITH", null, UUID.fromString("1104e683-5467-4340-b961-ff53672c4f39"), LocalDate.now().minusDays(7))
    val charge = CreateCharge(UUID.randomUUID(), "OFF123", LocalDate.now(), null, UUID.fromString("f17328cf-ceaa-43c2-930a-26cf74480e18"), null, sentence)
    val appearance = CreateCourtAppearance(courtCase.first, courtCase.second.appearances.first().appearanceUuid, UUID.fromString("62412083-9892-48c9-bf01-7864af4a8b3c"), "COURT1", "ADIFFERENTCOURTCASEREFERENCE", LocalDate.now(), null, "REMAND", 1, null, null, listOf(charge), LocalDate.now().minusDays(7), null)
    webTestClient
      .put()
      .uri("/court-appearance/${appearance.appearanceUuid}")
      .bodyValue(appearance)
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

  @Test
  fun `update appearance to edit charge`() {
    val courtCase = createCourtCase()
    val charge = courtCase.second.appearances.first().charges.first().copy(offenceCode = "OFF634624")
    val appearance = courtCase.second.appearances.first().copy(charges = listOf(charge), courtCaseUuid = courtCase.first)
    webTestClient
      .put()
      .uri("/court-appearance/${appearance.appearanceUuid}")
      .bodyValue(appearance)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isOk

    webTestClient
      .get()
      .uri("/court-appearance/${appearance.appearanceUuid}")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING"))
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.charges.[0].offenceCode")
      .isEqualTo(charge.offenceCode)
  }

  @Test
  fun `update appearance to delete charge`() {
    val courtCase = createCourtCase()
    val sentence = CreateSentence(null, "1", listOf(CreatePeriodLength(1, null, null, null, "years", PeriodLengthType.SENTENCE_LENGTH)), "FORTHWITH", null, UUID.fromString("1104e683-5467-4340-b961-ff53672c4f39"), LocalDate.now().minusDays(7))
    val charge = CreateCharge(UUID.randomUUID(), "OFF123", LocalDate.now(), null, UUID.fromString("f17328cf-ceaa-43c2-930a-26cf74480e18"), null, null)
    val secondCharge = CreateCharge(UUID.randomUUID(), "OFF567", LocalDate.now(), null, UUID.fromString("f17328cf-ceaa-43c2-930a-26cf74480e18"), null, sentence)
    val appearance = courtCase.second.appearances.first().copy(charges = listOf(charge, secondCharge), courtCaseUuid = courtCase.first)
    webTestClient
      .put()
      .uri("/court-appearance/${appearance.appearanceUuid}")
      .bodyValue(appearance)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isOk
    val appearanceWithoutSecondCharge = appearance.copy(charges = listOf(charge))
    webTestClient
      .put()
      .uri("/court-appearance/${appearance.appearanceUuid}")
      .bodyValue(appearanceWithoutSecondCharge)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isOk

    webTestClient
      .get()
      .uri("/court-appearance/${appearance.appearanceUuid}")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING"))
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.charges.[?(@.chargeUuid == '${secondCharge.chargeUuid}')]")
      .doesNotExist()
      .jsonPath("$.charges.[?(@.chargeUuid == '${charge.chargeUuid}')]")
      .exists()
  }

  @Test
  fun `cannot edit an already edited court appearance`() {
    val courtCase = createCourtCase()
    val sentence = CreateSentence(null, "1", listOf(CreatePeriodLength(1, null, null, null, "years", PeriodLengthType.SENTENCE_LENGTH)), "FORTHWITH", null, UUID.fromString("1104e683-5467-4340-b961-ff53672c4f39"), LocalDate.now().minusDays(7))
    val charge = CreateCharge(UUID.randomUUID(), "OFF123", LocalDate.now(), null, UUID.fromString("f17328cf-ceaa-43c2-930a-26cf74480e18"), null, sentence)
    val appearance = CreateCourtAppearance(courtCase.first, courtCase.second.appearances.first().appearanceUuid, UUID.fromString("62412083-9892-48c9-bf01-7864af4a8b3c"), "COURT1", "ADIFFERENTCOURTCASEREFERENCE", LocalDate.now(), null, "REMAND", 1, null, null, listOf(charge), LocalDate.now().minusDays(7), null)
    webTestClient
      .put()
      .uri("/court-appearance/${appearance.appearanceUuid}")
      .bodyValue(appearance)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isOk

    val editedAppearance = appearance.copy(courtCode = "DIFFERENTCOURT1")
    webTestClient
      .put()
      .uri("/court-appearance/${appearance.appearanceUuid}")
      .bodyValue(editedAppearance)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isBadRequest
  }

  @Test
  fun `must not update appearance when no court case exists`() {
    val charge = CreateCharge(UUID.randomUUID(), "OFF123", LocalDate.now(), null, UUID.fromString("f17328cf-ceaa-43c2-930a-26cf74480e18"), null, null)
    val appearance = CreateCourtAppearance(UUID.randomUUID().toString(), UUID.randomUUID(), UUID.fromString("62412083-9892-48c9-bf01-7864af4a8b3c"), "COURT1", "GH123456789", LocalDate.now(), null, "REMAND", 1, null, null, listOf(charge), null, null)
    webTestClient
      .put()
      .uri("/court-appearance/${appearance.appearanceUuid}")
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
    val charge = CreateCharge(UUID.randomUUID(), "OFF123", LocalDate.now(), null, UUID.fromString("f17328cf-ceaa-43c2-930a-26cf74480e18"), null, null)
    val appearance = CreateCourtAppearance(UUID.randomUUID().toString(), UUID.randomUUID(), UUID.fromString("62412083-9892-48c9-bf01-7864af4a8b3c"), "COURT1", "GH123456789", LocalDate.now(), null, "REMAND", 1, null, null, listOf(charge), null, null)
    webTestClient
      .put()
      .uri("/court-appearance/${appearance.appearanceUuid}")
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
    val charge = CreateCharge(UUID.randomUUID(), "OFF123", LocalDate.now(), null, UUID.fromString("f17328cf-ceaa-43c2-930a-26cf74480e18"), null, null)
    val appearance = CreateCourtAppearance(UUID.randomUUID().toString(), UUID.randomUUID(), UUID.fromString("62412083-9892-48c9-bf01-7864af4a8b3c"), "COURT1", "GH123456789", LocalDate.now(), null, "REMAND", 1, null, null, listOf(charge), null, null)
    webTestClient
      .put()
      .uri("/court-appearance/${appearance.appearanceUuid}")
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
