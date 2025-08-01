package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.courtappearance

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.DpsDataCreator
import java.time.format.DateTimeFormatter
import java.util.UUID

class GetCourtAppearanceTests : IntegrationTestBase() {

  @Test
  fun `get appearance by uuid`() {
    val createdAppearance = createCourtCase().second.appearances.first()
    webTestClient
      .get()
      .uri("/court-appearance/${createdAppearance.appearanceUuid!!}")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.appearanceUuid")
      .isEqualTo(createdAppearance.appearanceUuid.toString())
      .jsonPath("$.courtCaseReference")
      .isEqualTo(createdAppearance.courtCaseReference!!)
      .jsonPath("$.appearanceDate")
      .isEqualTo(createdAppearance.appearanceDate.format(DateTimeFormatter.ISO_DATE))
      .jsonPath("$.outcome.outcomeUuid")
      .isEqualTo(createdAppearance.outcomeUuid.toString())
      .jsonPath("$.nextCourtAppearance.appearanceDate")
      .isEqualTo(createdAppearance.nextCourtAppearance!!.appearanceDate.format(DateTimeFormatter.ISO_DATE))
      .jsonPath("$.nextCourtAppearance.courtCode")
      .isEqualTo(createdAppearance.nextCourtAppearance.courtCode)
      .jsonPath("$.warrantType")
      .isEqualTo(createdAppearance.warrantType)
      .jsonPath("$.charges[0].sentence.sentenceUuid")
      .isEqualTo(createdAppearance.charges[0].sentence!!.sentenceUuid.toString())
      .jsonPath("$.charges[0].sentence.chargeNumber")
      .isEqualTo(createdAppearance.charges[0].sentence!!.chargeNumber)
  }

  @Test
  fun `return inactive and active charges`() {
    val inactiveCharge = DpsDataCreator.dpsCreateCharge(outcomeUuid = UUID.fromString("86776327-7e1f-4830-bd4e-69168b3b0197")) // not guilty
    val activeCharge = DpsDataCreator.dpsCreateCharge(outcomeUuid = UUID.fromString("315280e5-d53e-43b3-8ba6-44da25676ce2")) // remand in custody
    val appearance = DpsDataCreator.dpsCreateCourtAppearance(charges = listOf(inactiveCharge, activeCharge))
    createCourtCase(DpsDataCreator.dpsCreateCourtCase(appearances = listOf(appearance)))
    webTestClient
      .get()
      .uri("/court-appearance/${appearance.appearanceUuid!!}")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.charges[?(@.chargeUuid == '${inactiveCharge.chargeUuid}')]")
      .exists()
      .jsonPath("$.charges[?(@.chargeUuid == '${activeCharge.chargeUuid}')]")
      .exists()
  }

  @Test
  fun `no appearance exist for uuid results in not found`() {
    webTestClient
      .get()
      .uri("/court-appearance/${UUID.randomUUID()}")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
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
      .uri("/court-appearance/${createdAppearance.appearanceUuid!!}")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `token with incorrect role is forbidden`() {
    val createdAppearance = createCourtCase().second.appearances.first()
    webTestClient
      .get()
      .uri("/court-appearance/${createdAppearance.appearanceUuid!!}")
      .headers {
        it.authToken(roles = listOf("ROLE_OTHER_FUNCTION"))
      }
      .exchange()
      .expectStatus()
      .isForbidden
  }
}
