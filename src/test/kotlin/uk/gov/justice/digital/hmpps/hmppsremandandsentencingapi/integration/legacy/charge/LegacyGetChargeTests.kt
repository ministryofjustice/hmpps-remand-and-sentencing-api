package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.charge

import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.DpsDataCreator
import java.util.UUID

class LegacyGetChargeTests : IntegrationTestBase() {

  @Test
  fun `get charge by lifetime uuid`() {
    val (lifetimeUuid, createdCharge) = createLegacyCharge()

    webTestClient
      .get()
      .uri("/legacy/charge/$lifetimeUuid")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_CHARGE_RO"))
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.lifetimeUuid")
      .isEqualTo(lifetimeUuid.toString())
      .jsonPath("$.offenceCode")
      .isEqualTo(createdCharge.offenceCode)
  }

  @Test
  fun `get latest charge when retrieving by uuid`() {
    val remandedCharge = DpsDataCreator.dpsCreateCharge(outcomeUuid = UUID.fromString("315280e5-d53e-43b3-8ba6-44da25676ce2"), sentence = null)
    val remandedAppearance = DpsDataCreator.dpsCreateCourtAppearance(outcomeUuid = UUID.fromString("2f585681-7b1a-44fb-a0cb-f9a4b1d9cda8"), charges = listOf(remandedCharge))
    val toCreateCourtCase = DpsDataCreator.dpsCreateCourtCase(appearances = listOf(remandedAppearance))
    val (uuid) = createCourtCase(toCreateCourtCase)
    val sentencedCharge = remandedCharge.copy(outcomeUuid = UUID.fromString("f17328cf-ceaa-43c2-930a-26cf74480e18"), sentence = DpsDataCreator.dpsCreateSentence())
    val sentencedAppearance = DpsDataCreator.dpsCreateCourtAppearance(courtCaseUuid = uuid, outcomeUuid = UUID.fromString("62412083-9892-48c9-bf01-7864af4a8b3c"), charges = listOf(sentencedCharge))
    webTestClient
      .post()
      .uri("/court-appearance")
      .bodyValue(sentencedAppearance)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isCreated
    webTestClient
      .get()
      .uri("/legacy/charge/${remandedCharge.chargeUuid}")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_CHARGE_RO"))
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.nomisOutcomeCode")
      .isEqualTo("1002")
  }

  @Test
  fun `no charge exist for uuid results in not found`() {
    webTestClient
      .get()
      .uri("/legacy/charge/${UUID.randomUUID()}")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_CHARGE_RO"))
      }
      .exchange()
      .expectStatus()
      .isNotFound
  }

  @Test
  fun `no token results in unauthorized`() {
    val (lifetimeUuid) = createLegacyCharge()
    webTestClient
      .get()
      .uri("/legacy/charge/$lifetimeUuid")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `token with incorrect role is forbidden`() {
    val (lifetimeUuid) = createLegacyCharge()
    webTestClient
      .get()
      .uri("/legacy/charge/$lifetimeUuid")
      .headers {
        it.authToken(roles = listOf("ROLE_OTHER_FUNCTION"))
      }
      .exchange()
      .expectStatus()
      .isForbidden
  }
}
