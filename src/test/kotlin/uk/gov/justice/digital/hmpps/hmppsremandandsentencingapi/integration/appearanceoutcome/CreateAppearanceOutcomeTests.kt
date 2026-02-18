package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.appearanceoutcome

import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.DpsDataCreator
import java.util.UUID

class CreateAppearanceOutcomeTests : IntegrationTestBase() {

  @Test
  fun `create appearance outcome`() {
    val createAppearanceOutcome = DpsDataCreator.createAppearanceOutcome()
    webTestClient.post()
      .uri("/appearance-outcome")
      .bodyValue(createAppearanceOutcome)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus().isCreated
      .expectBody()
      .jsonPath("$.outcomeName")
      .isEqualTo(createAppearanceOutcome.outcomeName)
      .jsonPath("$.nomisCode")
      .isEqualTo(createAppearanceOutcome.nomisCode)
  }

  @Test
  fun `adding a appearance outcome with a blank name results in error`() {
    val createAppearanceOutcome = DpsDataCreator.createAppearanceOutcome(outcomeName = "")
    webTestClient.post()
      .uri("/appearance-outcome")
      .bodyValue(createAppearanceOutcome)
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
  fun `trying to create a appearance outcome with a type not currently in the database results in error`() {
    val createAppearanceOutcome = DpsDataCreator.createAppearanceOutcome(outcomeType = "SOME_RANDOM_TYPE")
    webTestClient.post()
      .uri("/appearance-outcome")
      .bodyValue(createAppearanceOutcome)
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
  fun `trying to create a appearance outcome with a disposition code not currently in the database results in error`() {
    val createAppearanceOutcome = DpsDataCreator.createAppearanceOutcome(dispositionCode = "SOME_RANDOM_CODE")
    webTestClient.post()
      .uri("/appearance-outcome")
      .bodyValue(createAppearanceOutcome)
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
  fun `trying to create a appearance outcome with a warrant type not currently in the database results in error`() {
    val createAppearanceOutcome = DpsDataCreator.createAppearanceOutcome(warrantType = "SOME_RANDOM_TYPE")
    webTestClient.post()
      .uri("/appearance-outcome")
      .bodyValue(createAppearanceOutcome)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus().isBadRequest
      .expectBody()
      .jsonPath("$.fieldErrors[0].field")
      .isEqualTo("warrantType")
      .jsonPath("$.fieldErrors[0].message")
      .isEqualTo("Must use one of existing the warrant types IMMIGRATION, NON_SENTENCING, SENTENCING")
  }

  @Test
  fun `trying to create a appearance outcome with a NOMIS code that is already mapped results in error`() {
    val createAppearanceOutcome = DpsDataCreator.createAppearanceOutcome()
    webTestClient.post()
      .uri("/appearance-outcome")
      .bodyValue(createAppearanceOutcome)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus().isCreated
    webTestClient.post()
      .uri("/appearance-outcome")
      .bodyValue(createAppearanceOutcome)
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
    val createAppearanceOutcome = DpsDataCreator.createAppearanceOutcome(
      outcomeUuid = appearanceOutcomeUuid,
      nomisCode = createdAppearance.legacyData.nomisOutcomeCode!!,
      outcomeName = "A new supported appearance outcome",
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
