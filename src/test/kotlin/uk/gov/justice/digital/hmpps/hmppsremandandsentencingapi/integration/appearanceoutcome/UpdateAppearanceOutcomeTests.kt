package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.appearanceoutcome

import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.DpsDataCreator
import java.util.UUID

class UpdateAppearanceOutcomeTests : IntegrationTestBase() {

  @Test
  fun `create appearance outcome at uuid`() {
    val updateAppearanceOutcome = DpsDataCreator.createAppearanceOutcome()
    webTestClient.put()
      .uri("/appearance-outcome/${UUID.randomUUID()}")
      .bodyValue(updateAppearanceOutcome)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.outcomeName")
      .isEqualTo(updateAppearanceOutcome.outcomeName)
      .jsonPath("$.nomisCode")
      .isEqualTo(updateAppearanceOutcome.nomisCode)
  }

  @Test
  fun `update appearance outcome`() {
    val createAppearanceOutcome = DpsDataCreator.createAppearanceOutcome(
      outcomeUuid = UUID.randomUUID(),
    )
    webTestClient.post()
      .uri("/appearance-outcome")
      .bodyValue(createAppearanceOutcome)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus().isCreated

    val updateAppearanceOutcome = createAppearanceOutcome.copy(outcomeName = "A different Outcome Name", nomisCode = "99")
    webTestClient.put()
      .uri("/appearance-outcome/${createAppearanceOutcome.outcomeUuid}")
      .bodyValue(updateAppearanceOutcome)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.outcomeName")
      .isEqualTo(updateAppearanceOutcome.outcomeName)
  }

  @Test
  fun `updating a appearance outcome with a blank name results in error`() {
    val updateAppearanceOutcome = DpsDataCreator.createAppearanceOutcome(outcomeName = "")
    webTestClient.put()
      .uri("/appearance-outcome/${UUID.randomUUID()}")
      .bodyValue(updateAppearanceOutcome)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus().isBadRequest
      .expectBody()
      .jsonPath("$.fieldErrors[0].field")
      .isEqualTo("outcomeName")
      .jsonPath("$.fieldErrors[0].message")
      .isEqualTo("Outcome name must not be blank")
  }

  @Test
  fun `trying to update a appearance outcome with a type not currently in the database results in error`() {
    val updateAppearanceOutcome = DpsDataCreator.createAppearanceOutcome(outcomeType = "SOME_RANDOM_TYPE")
    webTestClient.put()
      .uri("/appearance-outcome/${UUID.randomUUID()}")
      .bodyValue(updateAppearanceOutcome)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus().isBadRequest
      .expectBody()
      .jsonPath("$.fieldErrors[0].field")
      .isEqualTo("outcomeType")
      .jsonPath("$.fieldErrors[0].message")
      .isEqualTo("Must use one of existing the outcome types IMMIGRATION, NON_CUSTODIAL, REMAND, SENTENCING")
  }

  @Test
  fun `trying to update a appearance outcome with a disposition code not currently in the database results in error`() {
    val updateAppearanceOutcome = DpsDataCreator.createAppearanceOutcome(dispositionCode = "SOME_RANDOM_CODE")
    webTestClient.put()
      .uri("/appearance-outcome/${UUID.randomUUID()}")
      .bodyValue(updateAppearanceOutcome)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus().isBadRequest
      .expectBody()
      .jsonPath("$.fieldErrors[0].field")
      .isEqualTo("dispositionCode")
      .jsonPath("$.fieldErrors[0].message")
      .isEqualTo("Must use one of existing the disposition codes FINAL, INTERIM")
  }

  @Test
  fun `trying to update a appearance outcome with a NOMIS code that is already mapped results in error`() {
    val updateAppearanceOutcome = DpsDataCreator.createAppearanceOutcome()
    webTestClient.put()
      .uri("/appearance-outcome/${UUID.randomUUID()}")
      .bodyValue(updateAppearanceOutcome)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus().isOk
    webTestClient.put()
      .uri("/appearance-outcome/${UUID.randomUUID()}")
      .bodyValue(updateAppearanceOutcome)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus().isBadRequest
      .expectBody()
      .jsonPath("$.fieldErrors[0].field")
      .isEqualTo("nomisCode")
      .jsonPath("$.fieldErrors[0].message")
      .isEqualTo("nomisCode outcome code is already mapped")
  }

  @Test
  fun `migrate legacy appearance records to the new supported appearance outcome`() {
    val (appearanceUuid, createdAppearance) = createLegacyCourtAppearance()
    val appearanceOutcomeUuid = UUID.randomUUID()
    val updateAppearanceOutcome = DpsDataCreator.createAppearanceOutcome(
      outcomeUuid = appearanceOutcomeUuid,
      nomisCode = createdAppearance.legacyData.nomisOutcomeCode!!,
      outcomeName = "A new supported appearance outcome",
    )
    webTestClient.put()
      .uri("/appearance-outcome/${UUID.randomUUID()}")
      .bodyValue(updateAppearanceOutcome)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus().isOk

    await untilCallTo {
      courtAppearanceRepository.findByAppearanceUuid(appearanceUuid)?.appearanceOutcome
    } matches { it != null }

    webTestClient
      .get()
      .uri("/court-appearance/$appearanceUuid")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.outcome.outcomeUuid")
      .isEqualTo(appearanceOutcomeUuid.toString())
  }
}
