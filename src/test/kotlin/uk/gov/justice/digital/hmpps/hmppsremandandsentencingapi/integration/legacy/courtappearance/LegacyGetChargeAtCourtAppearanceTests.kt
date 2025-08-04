package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.courtappearance

import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateCourtAppearanceResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.DpsDataCreator
import java.util.UUID

class LegacyGetChargeAtCourtAppearanceTests : IntegrationTestBase() {

  @Test
  fun `can get charge at specific appearance`() {
    val (courtCaseUuid, courtCase) = createCourtCase()
    val appearance = courtCase.appearances.first()
    val charge = appearance.charges.first()
    val oldOutcomeNomisCode = "1002" // NOMIS code for f17328cf-ceaa-43c2-930a-26cf74480e18
    val newOutcome = UUID.fromString("315280e5-d53e-43b3-8ba6-44da25676ce2")
    val newOutcomeNomisCode = "4531"
    val chargeWithNewOutcome = charge.copy(outcomeUuid = newOutcome)
    val newAppearance = DpsDataCreator.dpsCreateCourtAppearance(courtCaseUuid = courtCaseUuid, charges = listOf(chargeWithNewOutcome))
    val newAppearanceResponse = webTestClient
      .post()
      .uri("/court-appearance")
      .bodyValue(newAppearance)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isCreated
      .returnResult(CreateCourtAppearanceResponse::class.java)
      .responseBody.blockFirst()!!

    webTestClient
      .get()
      .uri("/legacy/court-appearance/${appearance.appearanceUuid}/charge/${charge.chargeUuid}")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_APPEARANCE_RO"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.nomisOutcomeCode")
      .isEqualTo(oldOutcomeNomisCode)

    webTestClient
      .get()
      .uri("/legacy/court-appearance/${newAppearance.appearanceUuid}/charge/${charge.chargeUuid}")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_APPEARANCE_RO"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.nomisOutcomeCode")
      .isEqualTo(newOutcomeNomisCode)
  }

  @Test
  fun `no charge exist for uuid results in not found`() {
    webTestClient
      .get()
      .uri("/legacy/court-appearance/${UUID.randomUUID()}/charge/${UUID.randomUUID()}")
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
    val charge = createdAppearance.charges.first()
    webTestClient
      .get()
      .uri("/legacy/court-appearance/${createdAppearance.appearanceUuid}/charge/${charge.chargeUuid}")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `token with incorrect role is forbidden`() {
    val createdAppearance = createCourtCase().second.appearances.first()
    val charge = createdAppearance.charges.first()
    webTestClient
      .get()
      .uri("/legacy/court-appearance/${createdAppearance.appearanceUuid}/charge/${charge.chargeUuid}")
      .headers {
        it.authToken(roles = listOf("ROLE_OTHER_FUNCTION"))
      }
      .exchange()
      .expectStatus()
      .isForbidden
  }
}
