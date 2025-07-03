package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.courtcase

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.DpsDataCreator
import java.time.LocalDate

class GetCourtCaseLatestOffenceDateTests : IntegrationTestBase() {

  @Test
  fun `should return latest offence date for court case`() {
    val offenceStart = LocalDate.now().minusDays(10)
    val offenceEnd = LocalDate.now().minusDays(5)
    val charge = DpsDataCreator.dpsCreateCharge(offenceStartDate = offenceStart, offenceEndDate = offenceEnd)
    val appearance = DpsDataCreator.dpsCreateCourtAppearance(charges = listOf(charge))
    val courtCase = DpsDataCreator.dpsCreateCourtCase(appearances = listOf(appearance))
    val (courtCaseUuid) = createCourtCase(courtCase)

    webTestClient.get()
      .uri("/court-case/$courtCaseUuid/latest-offence-date")
      .headers { it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING")) }
      .exchange()
      .expectStatus().isOk
      .expectBody(LocalDate::class.java)
      .isEqualTo(offenceEnd)
  }

  @Test
  fun `should return 204 when no valid offence dates exist`() {
    val appearance = DpsDataCreator.dpsCreateCourtAppearance(charges = emptyList())
    val courtCase = DpsDataCreator.dpsCreateCourtCase(appearances = listOf(appearance))
    val (courtCaseUuid) = createCourtCase(courtCase)

    webTestClient.get()
      .uri("/court-case/$courtCaseUuid/latest-offence-date")
      .headers { it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING")) }
      .exchange()
      .expectStatus().isNoContent
  }

  @Test
  fun `no token results in unauthorized`() {
    val (courtCaseUuid) = createCourtCase()
    webTestClient.get()
      .uri("/court-case/$courtCaseUuid/latest-offence-date")
      .exchange()
      .expectStatus().isUnauthorized
  }

  @Test
  fun `token with incorrect role is forbidden`() {
    val (courtCaseUuid) = createCourtCase()
    webTestClient.get()
      .uri("/court-case/$courtCaseUuid/latest-offence-date")
      .headers { it.authToken(roles = listOf("ROLE_OTHER_FUNCTION")) }
      .exchange()
      .expectStatus().isForbidden
  }
}
