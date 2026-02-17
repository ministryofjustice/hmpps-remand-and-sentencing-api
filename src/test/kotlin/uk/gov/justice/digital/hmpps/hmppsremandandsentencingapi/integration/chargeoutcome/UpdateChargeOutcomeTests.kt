package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.chargeoutcome

import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.DpsDataCreator
import java.util.UUID

class UpdateChargeOutcomeTests : IntegrationTestBase() {

  @Test
  fun `create charge outcome at uuid`() {
    val updateChargeOutcome = DpsDataCreator.createChargeOutcome()
    webTestClient.put()
      .uri("/charge-outcome/${UUID.randomUUID()}")
      .bodyValue(updateChargeOutcome)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.outcomeName")
      .isEqualTo(updateChargeOutcome.outcomeName)
      .jsonPath("$.nomisCode")
      .isEqualTo(updateChargeOutcome.nomisCode)
  }

  @Test
  fun `update charge outcome`() {
    val createChargeOutcome = DpsDataCreator.createChargeOutcome(
      outcomeUuid = UUID.randomUUID(),
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

    val updateChargeOutcome = createChargeOutcome.copy(outcomeName = "A different Outcome Name", nomisCode = "99")
    webTestClient.put()
      .uri("/charge-outcome/${createChargeOutcome.outcomeUuid}")
      .bodyValue(updateChargeOutcome)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.outcomeName")
      .isEqualTo(updateChargeOutcome.outcomeName)
  }

  @Test
  fun `updating a charge outcome with a blank name results in error`() {
    val updateChargeOutcome = DpsDataCreator.createChargeOutcome(outcomeName = "")
    webTestClient.put()
      .uri("/charge-outcome/${UUID.randomUUID()}")
      .bodyValue(updateChargeOutcome)
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
  fun `trying to update a charge outcome with a type not currently in the database results in error`() {
    val updateChargeOutcome = DpsDataCreator.createChargeOutcome(outcomeType = "SOME_RANDOM_TYPE")
    webTestClient.put()
      .uri("/charge-outcome/${UUID.randomUUID()}")
      .bodyValue(updateChargeOutcome)
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
  fun `trying to update a charge outcome with a disposition code not currently in the database results in error`() {
    val updateChargeOutcome = DpsDataCreator.createChargeOutcome(dispositionCode = "SOME_RANDOM_CODE")
    webTestClient.put()
      .uri("/charge-outcome/${UUID.randomUUID()}")
      .bodyValue(updateChargeOutcome)
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
  fun `trying to update a charge outcome with a NOMIS code that is already mapped results in error`() {
    val updateChargeOutcome = DpsDataCreator.createChargeOutcome()
    webTestClient.put()
      .uri("/charge-outcome/${UUID.randomUUID()}")
      .bodyValue(updateChargeOutcome)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus().isOk
    webTestClient.put()
      .uri("/charge-outcome/${UUID.randomUUID()}")
      .bodyValue(updateChargeOutcome)
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
  fun `migrate legacy charge records to the new supported charge outcome`() {
    val (chargeUuid, createdCharge) = createLegacyCharge()
    val chargeOutcomeUuid = UUID.randomUUID()
    val updateChargeOutcome = DpsDataCreator.createChargeOutcome(
      outcomeUuid = chargeOutcomeUuid,
      nomisCode = createdCharge.legacyData.nomisOutcomeCode!!,
      outcomeName = "A new supported charge outcome",
    )
    webTestClient.put()
      .uri("/charge-outcome/${UUID.randomUUID()}")
      .bodyValue(updateChargeOutcome)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus().isOk

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
