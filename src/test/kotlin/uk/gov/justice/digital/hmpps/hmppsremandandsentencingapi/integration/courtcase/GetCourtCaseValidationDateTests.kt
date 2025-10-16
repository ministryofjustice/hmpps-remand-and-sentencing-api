package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.courtcase

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.validate.CourtCaseValidationDate
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.DpsDataCreator
import java.time.LocalDate
import java.util.UUID

class GetCourtCaseValidationDateTests : IntegrationTestBase() {

  @Test
  fun `should return dates from other appearances`() {
    val offenceStartToExclude = LocalDate.now().minusDays(10)
    val offenceEndToExclude = LocalDate.now().minusDays(5)
    val offenceStartToInclude = LocalDate.now().minusDays(15)
    val chargeToExclude = DpsDataCreator.dpsCreateCharge(offenceStartDate = offenceStartToExclude, offenceEndDate = offenceEndToExclude)
    val chargeToInclude = DpsDataCreator.dpsCreateCharge(offenceStartDate = offenceStartToInclude)
    val appearanceToExclude = DpsDataCreator.dpsCreateCourtAppearance(charges = listOf(chargeToExclude), warrantType = "REMAND", appearanceDate = LocalDate.now().minusDays(5L))
    val appearanceToInclude = DpsDataCreator.dpsCreateCourtAppearance(charges = listOf(chargeToInclude), warrantType = "REMAND", appearanceDate = LocalDate.now().minusDays(10L))
    val courtCase = DpsDataCreator.dpsCreateCourtCase(appearances = listOf(appearanceToExclude, appearanceToInclude))
    val (courtCaseUuid) = createCourtCase(courtCase)

    webTestClient.get()
      .uri("/court-case/$courtCaseUuid/validation-dates?appearanceUuidToExclude=${appearanceToExclude.appearanceUuid}")
      .headers { it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI")) }
      .exchange()
      .expectStatus().isOk
      .expectBody(CourtCaseValidationDate::class.java)
      .isEqualTo(CourtCaseValidationDate(offenceStartToInclude, appearanceToInclude.appearanceDate))
  }

  @Test
  fun `should return null when no valid dates exist`() {
    val appearance = DpsDataCreator.dpsCreateCourtAppearance(charges = emptyList())
    val courtCase = DpsDataCreator.dpsCreateCourtCase(appearances = listOf(appearance))
    val (courtCaseUuid) = createCourtCase(courtCase)

    webTestClient.get()
      .uri("/court-case/$courtCaseUuid/validation-dates?appearanceUuidToExclude=${UUID.randomUUID()}")
      .headers { it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI")) }
      .exchange()
      .expectStatus().isOk
      .expectBody(CourtCaseValidationDate::class.java)
      .isEqualTo(CourtCaseValidationDate(null, null))
  }

  @Test
  fun `no token results in unauthorized`() {
    val (courtCaseUuid) = createCourtCase()
    webTestClient.get()
      .uri("/court-case/$courtCaseUuid/validation-dates?appearanceUuidToExclude=${UUID.randomUUID()}")
      .exchange()
      .expectStatus().isUnauthorized
  }

  @Test
  fun `token with incorrect role is forbidden`() {
    val (courtCaseUuid) = createCourtCase()
    webTestClient.get()
      .uri("/court-case/$courtCaseUuid/validation-dates?appearanceUuidToExclude=${UUID.randomUUID()}")
      .headers { it.authToken(roles = listOf("ROLE_OTHER_FUNCTION")) }
      .exchange()
      .expectStatus().isForbidden
  }
}
