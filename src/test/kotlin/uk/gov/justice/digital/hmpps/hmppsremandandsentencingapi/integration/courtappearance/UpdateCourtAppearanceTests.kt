package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.courtappearance

import org.assertj.core.api.Assertions
import org.hamcrest.text.MatchesPattern
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateCourtAppearanceResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.DpsDataCreator
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

class UpdateCourtAppearanceTests : IntegrationTestBase() {

  @Test
  fun `update appearance in existing court case`() {
    val courtCase = createCourtCase()
    val createdAppearance = courtCase.second.appearances.first()
    val updateCourtAppearance = DpsDataCreator.dpsCreateCourtAppearance(courtCaseUuid = courtCase.first, appearanceUUID = createdAppearance.appearanceUuid, lifetimeUuid = createdAppearance.lifetimeUuid, courtCaseReference = "ADIFFERENTCOURTCASEREFERENCE")
    webTestClient
      .put()
      .uri("/court-appearance/${createdAppearance.appearanceUuid}")
      .bodyValue(updateCourtAppearance)
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
    val messages = getMessages(6)
    Assertions.assertThat(messages).hasSize(6).extracting<String> { it.eventType }.contains("court-appearance.updated")
  }

  @Test
  fun `updating only a court appearance keeps the next court appearance`() {
    val courtCase = createCourtCase()
    val createdAppearance = courtCase.second.appearances.first()
    val updateCourtAppearance = DpsDataCreator.dpsCreateCourtAppearance(courtCaseUuid = courtCase.first, appearanceUUID = createdAppearance.appearanceUuid, lifetimeUuid = createdAppearance.lifetimeUuid, courtCaseReference = "ADIFFERENTCOURTCASEREFERENCE")
    val response = webTestClient
      .put()
      .uri("/court-appearance/${createdAppearance.appearanceUuid}")
      .bodyValue(updateCourtAppearance)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isOk
      .returnResult(CreateCourtAppearanceResponse::class.java)
      .responseBody.blockFirst()!!
    webTestClient
      .get()
      .uri("/court-appearance/${response.appearanceUuid}")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.nextCourtAppearance")
      .exists()
  }

  @Test
  fun `updating the appearance date results in sentence updated events`() {
    val appearanceDate = LocalDate.now()
    val sentencedAppearance = DpsDataCreator.dpsCreateCourtAppearance(
      outcomeUuid = UUID.fromString("62412083-9892-48c9-bf01-7864af4a8b3c"),
      warrantType = "SENTENCING",
      appearanceDate = appearanceDate,
    )
    val (courtCaseUuid, createdCourtCase) = createCourtCase(DpsDataCreator.dpsCreateCourtCase(appearances = listOf(sentencedAppearance)))
    val createdAppearance = createdCourtCase.appearances.first().copy(courtCaseUuid = courtCaseUuid, appearanceDate = appearanceDate.minusDays(10))
    webTestClient
      .put()
      .uri("/court-appearance/${createdAppearance.appearanceUuid}")
      .bodyValue(createdAppearance)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isOk

    val messages = getMessages(2)
    Assertions.assertThat(messages).hasSize(2).extracting<String> { it.eventType }.contains("sentence.updated")
  }

  @Test
  fun `updating the next appearance time stores the updated value`() {
    val courtCase = createCourtCase()
    val createdAppearance = courtCase.second.appearances.first()
    val createdNextAppearance = createdAppearance.nextCourtAppearance!!
    val updateNextAppearance = createdNextAppearance.copy(appearanceTime = createdNextAppearance.appearanceTime!!.plusHours(2).withSecond(0).withNano(0))
    val updateCourtAppearance = createdAppearance.copy(courtCaseUuid = courtCase.first, appearanceUuid = createdAppearance.appearanceUuid, lifetimeUuid = createdAppearance.lifetimeUuid, nextCourtAppearance = updateNextAppearance)
    val response = webTestClient
      .put()
      .uri("/court-appearance/${createdAppearance.appearanceUuid}")
      .bodyValue(updateCourtAppearance)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isOk
      .returnResult(CreateCourtAppearanceResponse::class.java)
      .responseBody.blockFirst()!!

    webTestClient
      .get()
      .uri("/court-appearance/${response.appearanceUuid}")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.nextCourtAppearance.appearanceTime")
      .isEqualTo(updateNextAppearance.appearanceTime!!.format(DateTimeFormatter.ISO_LOCAL_TIME))
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
      .jsonPath("$.charges.[?(@.lifetimeUuid == '${charge.lifetimeChargeUuid}')].outcome.outcomeUuid")
      .isEqualTo("68e56c1f-b179-43da-9d00-1272805a7ad3") // replaced with another outcome
      .jsonPath("$.charges.[?(@.lifetimeUuid != '${charge.lifetimeChargeUuid}')].offenceCode")
      .isEqualTo(charge.offenceCode)
  }

  @Test
  fun `update appearance to delete charge`() {
    val courtCase = createCourtCase()
    val charge = DpsDataCreator.dpsCreateCharge()
    val secondCharge = DpsDataCreator.dpsCreateCharge(offenceCode = "OFF567")
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
    purgeQueues()
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

    val messages = getMessages(3)
    Assertions.assertThat(messages).hasSize(3).extracting<String> { it.eventType }.contains("court-appearance.updated")

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
    val createdAppearance = courtCase.second.appearances.first()
    val updateCourtAppearance = DpsDataCreator.dpsCreateCourtAppearance(courtCaseUuid = courtCase.first, appearanceUUID = createdAppearance.appearanceUuid, lifetimeUuid = createdAppearance.lifetimeUuid, courtCaseReference = "ADIFFERENTCOURTCASEREFERENCE")
    webTestClient
      .put()
      .uri("/court-appearance/${createdAppearance.appearanceUuid}")
      .bodyValue(updateCourtAppearance)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isOk

    val editedAppearance = updateCourtAppearance.copy(courtCode = "DIFFERENTCOURT1")
    webTestClient
      .put()
      .uri("/court-appearance/${createdAppearance.appearanceUuid}")
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
    val updateCourtAppearance = DpsDataCreator.dpsCreateCourtAppearance(courtCaseUuid = UUID.randomUUID().toString())
    webTestClient
      .put()
      .uri("/court-appearance/${updateCourtAppearance.appearanceUuid}")
      .bodyValue(updateCourtAppearance)
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
    val updateCourtAppearance = DpsDataCreator.dpsCreateCourtAppearance()
    webTestClient
      .put()
      .uri("/court-appearance/${updateCourtAppearance.appearanceUuid}")
      .bodyValue(updateCourtAppearance)
      .headers {
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `token with incorrect role is forbidden`() {
    val updateCourtAppearance = DpsDataCreator.dpsCreateCourtAppearance()
    webTestClient
      .put()
      .uri("/court-appearance/${updateCourtAppearance.appearanceUuid}")
      .bodyValue(updateCourtAppearance)
      .headers {
        it.authToken(roles = listOf("ROLE_OTHER_FUNCTION"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isForbidden
  }
}
