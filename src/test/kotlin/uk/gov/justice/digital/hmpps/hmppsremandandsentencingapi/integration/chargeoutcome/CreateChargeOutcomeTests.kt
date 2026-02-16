package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.chargeoutcome

import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.DpsDataCreator
import java.util.UUID

class CreateChargeOutcomeTests : IntegrationTestBase() {

  @Test
  fun `create charge outcome`() {
    val createChargeOutcome = DpsDataCreator.createChargeOutcome()
    webTestClient.post()
      .uri("/charge-outcome")
      .bodyValue(createChargeOutcome)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus().isCreated
      .expectBody()
      .jsonPath("$.outcomeName")
      .isEqualTo(createChargeOutcome.outcomeName)
      .jsonPath("$.nomisCode")
      .isEqualTo(createChargeOutcome.nomisCode)
  }

  @Test
  fun `trying to create a charge outcome with a type not currently in the database results in error`() {
    val createChargeOutcome = DpsDataCreator.createChargeOutcome(outcomeType = "SOME_RANDOM_TYPE")
    webTestClient.post()
      .uri("/charge-outcome")
      .bodyValue(createChargeOutcome)
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
  fun `trying to create a charge outcome with a disposition code not currently in the database results in error`() {
    val createChargeOutcome = DpsDataCreator.createChargeOutcome(dispositionCode = "SOME_RANDOM_CODE")
    webTestClient.post()
      .uri("/charge-outcome")
      .bodyValue(createChargeOutcome)
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
  fun `migrate legacy charge records to the new supported charge outcome`() {
    val (chargeUuid, createdCharge) = createLegacyCharge()
    val chargeOutcomeUuid = UUID.randomUUID()
    val createChargeOutcome = DpsDataCreator.createChargeOutcome(
      outcomeUuid = chargeOutcomeUuid,
      nomisCode = createdCharge.legacyData.nomisOutcomeCode!!,
      outcomeName = "A new supported charge outcome",
    )
    webTestClient.post()
      .uri("/charge-outcome")
      .bodyValue(createChargeOutcome)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus().isCreated

    await untilCallTo {
      chargeRepository.findFirstByChargeUuidAndStatusIdNotOrderByUpdatedAtDesc(chargeUuid)?.chargeOutcome
    } matches { it != null }

    webTestClient
      .get()
      .uri("/court-appearance/${createdCharge.appearanceLifetimeUuid}")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.charges[0].outcome.outcomeUuid")
      .isEqualTo(chargeOutcomeUuid.toString())
  }
}
