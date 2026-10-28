package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.person

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.util.DataCreator
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.DpsDataCreator
import java.time.LocalDate
import java.util.UUID

class GetPersonStatusTests : IntegrationTestBase() {

  @Test
  fun `isOnRemand is true when the latest appearance outcome is REMAND`() {
    val prisonerId = "PRN0001"
    createCourtCase(DpsDataCreator.dpsCreateNonSentencedCourtCase(prisonerId = prisonerId)) // outcome is 'Remand in custody'

    expectIsOnRemand(prisonerId = prisonerId, expected = true)
  }

  @Test
  fun `isOnRemand is false when the latest appearance outcome is sentencing`() {
    val prisonerId = "PRN0002"
    createCourtCase(
      DpsDataCreator.dpsCreateCourtCase(
        prisonerId = prisonerId,
        appearances = listOf(
          DpsDataCreator.dpsCreateNonSentencedCourtAppearance(appearanceDate = LocalDate.now().minusDays(1)),
          DpsDataCreator.dpsCreateCourtAppearance(),
        ),
      ),
    )

    expectIsOnRemand(prisonerId = prisonerId, expected = false)
  }

  @Test
  fun `isOnRemand is false when the latest appearance outcome is a final non custodial outcome`() {
    val prisonerId = "PRN0003"
    createCourtCase(
      DpsDataCreator.dpsCreateNonSentencedCourtCase(
        prisonerId = prisonerId,
        appearances = listOf(
          DpsDataCreator.dpsCreateNonSentencedCourtAppearance(
            outcomeUuid = UUID.fromString("1709089f-0cf8-4544-8229-a2251e671241"), // Fine outcome
          ),
        ),
      ),
    )

    expectIsOnRemand(prisonerId, false)
  }

  @Test
  fun `isOnRemand is true when the latest appearance has a legacy NOMIS remand outcome`() {
    val prisonerId = "PRN0004"
    createLegacyCourtAppearance(
      legacyCreateCourtCase = DataCreator.legacyCreateCourtCase(prisonerId = prisonerId),
      legacyCreateCourtAppearance = DataCreator.legacyCreateCourtAppearance(
        legacyData = DataCreator.courtAppearanceLegacyData(nomisOutcomeCode = "4505"),
      ),
    )

    expectIsOnRemand(prisonerId, true)
  }

  @Test
  fun `isOnRemand is false when the latest appearance has non-REMAND NOMIS outcome`() {
    val prisonerId = "PRN0005"
    createLegacyCourtAppearance(
      legacyCreateCourtCase = DataCreator.legacyCreateCourtCase(prisonerId = prisonerId),
      legacyCreateCourtAppearance = DataCreator.legacyCreateCourtAppearance(
        legacyData = DataCreator.courtAppearanceLegacyData(nomisOutcomeCode = "9999"),
      ),
    )

    expectIsOnRemand(prisonerId, false)
  }

  private fun expectIsOnRemand(prisonerId: String, expected: Boolean) {
    webTestClient.get()
      .uri("/person/{prisonerId}/status", prisonerId)
      .headers { it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__PRISONER__RO")) }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.isOnRemand")
      .isEqualTo(expected)
  }
}
