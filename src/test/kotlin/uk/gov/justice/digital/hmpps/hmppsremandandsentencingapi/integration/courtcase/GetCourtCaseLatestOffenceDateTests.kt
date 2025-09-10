package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.courtcase

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.LatestOffenceDate
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
      .headers { it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI")) }
      .exchange()
      .expectStatus().isOk
      .expectBody(LatestOffenceDate::class.java)
      .isEqualTo(LatestOffenceDate(offenceEnd))
  }

  @Test
  fun `should exclude appearance if parameter set - returning max date from other appearances`() {
    val offenceStartToExclude = LocalDate.now().minusDays(10)
    val offenceEndToExclude = LocalDate.now().minusDays(5)
    val offenceStartToInclude = LocalDate.now().minusDays(15)
    val chargeToExclude = DpsDataCreator.dpsCreateCharge(offenceStartDate = offenceStartToExclude, offenceEndDate = offenceEndToExclude)
    val chargeToInclude = DpsDataCreator.dpsCreateCharge(offenceStartDate = offenceStartToInclude)
    val appearanceToExclude = DpsDataCreator.dpsCreateCourtAppearance(charges = listOf(chargeToExclude))
    val appearanceToInclude = DpsDataCreator.dpsCreateCourtAppearance(charges = listOf(chargeToInclude))
    val courtCase = DpsDataCreator.dpsCreateCourtCase(appearances = listOf(appearanceToExclude, appearanceToInclude))
    val (courtCaseUuid) = createCourtCase(courtCase)

    webTestClient.get()
      .uri("/court-case/$courtCaseUuid/latest-offence-date?appearanceUuidToExclude=${appearanceToExclude.appearanceUuid}")
      .headers { it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI")) }
      .exchange()
      .expectStatus().isOk
      .expectBody(LatestOffenceDate::class.java)
      .isEqualTo(LatestOffenceDate(offenceStartToInclude))
  }

  @Test
  fun `should exclude appearance if parameter set`() {
    val offenceStart = LocalDate.now().minusDays(10)
    val offenceEnd = LocalDate.now().minusDays(5)
    val charge = DpsDataCreator.dpsCreateCharge(offenceStartDate = offenceStart, offenceEndDate = offenceEnd)
    val appearance = DpsDataCreator.dpsCreateCourtAppearance(charges = listOf(charge))
    val courtCase = DpsDataCreator.dpsCreateCourtCase(appearances = listOf(appearance))
    val (courtCaseUuid) = createCourtCase(courtCase)

    webTestClient.get()
      .uri("/court-case/$courtCaseUuid/latest-offence-date?appearanceUuidToExclude=${appearance.appearanceUuid}")
      .headers { it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI")) }
      .exchange()
      .expectStatus().isOk
      .expectBody(LatestOffenceDate::class.java)
      .isEqualTo(LatestOffenceDate(null))
  }

  @Test
  fun `should return null when no valid offence dates exist`() {
    val appearance = DpsDataCreator.dpsCreateCourtAppearance(charges = emptyList())
    val courtCase = DpsDataCreator.dpsCreateCourtCase(appearances = listOf(appearance))
    val (courtCaseUuid) = createCourtCase(courtCase)

    webTestClient.get()
      .uri("/court-case/$courtCaseUuid/latest-offence-date")
      .headers { it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI")) }
      .exchange()
      .expectStatus().isOk
      .expectBody(LatestOffenceDate::class.java)
      .isEqualTo(LatestOffenceDate(null))
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
