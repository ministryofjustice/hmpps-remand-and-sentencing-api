package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.courtappearance

import org.assertj.core.api.Assertions
import org.hamcrest.text.MatchesPattern
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.DpsDataCreator
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
    val messages = getMessages(4)
    Assertions.assertThat(messages).hasSize(4).extracting<String> { it.eventType }.contains("court-appearance.updated")
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

    val messages = getMessages(2)
    Assertions.assertThat(messages).hasSize(2).extracting<String> { it.eventType }.contains("court-appearance.updated")

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
