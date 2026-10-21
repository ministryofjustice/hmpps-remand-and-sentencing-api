package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.person

import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.UpdateCourtCaseStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.CourtCaseEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.DpsDataCreator
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class GetPersonChargesTests : IntegrationTestBase() {

  @Test
  fun `retrieve all charges for a person, no query parameters means active only`() {
    val prisonerId = "PRI123"
    val (_, createdCourtCase) = createCourtCase(createCourtCase = DpsDataCreator.dpsCreateNonSentencedCourtCase(prisonerId = prisonerId))
    val createdCharge = createdCourtCase.appearances.first().charges.first()
    val (inactiveCourtCaseUuid, createdInactiveCourtCase) = createCourtCase(createCourtCase = DpsDataCreator.dpsCreateNonSentencedCourtCase(prisonerId = prisonerId))
    val inactiveCharge = createdInactiveCourtCase.appearances.first().charges.first()
    webTestClient
      .put()
      .uri("/court-case/$inactiveCourtCaseUuid/status")
      .bodyValue(UpdateCourtCaseStatus(status = CourtCaseEntityStatus.INACTIVE, reason = "No longer required"))
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isNoContent

    webTestClient
      .get()
      .uri("/person/$prisonerId/charges")
      .headers { it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__CHARGE__RO")) }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath(("$.charges[?(@.chargeUuid == '${createdCharge.chargeUuid}')]"))
      .exists()
      .jsonPath(("$.charges[?(@.chargeUuid == '${inactiveCharge.chargeUuid}')]"))
      .doesNotExist()
  }

  @Test
  fun `can return inactive charges for a person`() {
    val prisonerId = "PRI123"
    val (_, createdCourtCase) = createCourtCase(createCourtCase = DpsDataCreator.dpsCreateNonSentencedCourtCase(prisonerId = prisonerId))
    val createdCharge = createdCourtCase.appearances.first().charges.first()
    val (inactiveCourtCaseUuid, createdInactiveCourtCase) = createCourtCase(createCourtCase = DpsDataCreator.dpsCreateNonSentencedCourtCase(prisonerId = prisonerId))
    val inactiveCharge = createdInactiveCourtCase.appearances.first().charges.first()
    webTestClient
      .put()
      .uri("/court-case/$inactiveCourtCaseUuid/status")
      .bodyValue(UpdateCourtCaseStatus(status = CourtCaseEntityStatus.INACTIVE, reason = "No longer required"))
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isNoContent

    webTestClient
      .get()
      .uri {
        it.path("/person/$prisonerId/charges").queryParam("statuses", "INACTIVE")
          .build()
      }
      .headers { it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__CHARGE__RO")) }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath(("$.charges[?(@.chargeUuid == '${createdCharge.chargeUuid}')]"))
      .doesNotExist()
      .jsonPath(("$.charges[?(@.chargeUuid == '${inactiveCharge.chargeUuid}')]"))
      .exists()
  }

  @Test
  fun `can filter using from date`() {
    val prisonerId = "PRI123"
    val fromDate = LocalDate.now().minusDays(10)
    val (_, beforeCourtCase) = createCourtCase(
      createCourtCase = DpsDataCreator.dpsCreateNonSentencedCourtCase(
        prisonerId = prisonerId,
        appearances = listOf(
          DpsDataCreator.dpsCreateNonSentencedCourtAppearance(appearanceDate = fromDate.minusDays(10), nextCourtAppearance = null),
        ),
      ),
    )
    val beforeFromDateCharge = beforeCourtCase.appearances.first().charges.first()
    val (_, afterCourtCase) = createCourtCase(
      createCourtCase = DpsDataCreator.dpsCreateNonSentencedCourtCase(
        prisonerId = prisonerId,
        appearances = listOf(
          DpsDataCreator.dpsCreateNonSentencedCourtAppearance(appearanceDate = fromDate.plusDays(2), nextCourtAppearance = null),
        ),
      ),
    )
    val afterFromDateCharge = afterCourtCase.appearances.first().charges.first()
    webTestClient
      .get()
      .uri {
        it.path("/person/$prisonerId/charges")
          .queryParam("fromDate", fromDate.format(DateTimeFormatter.ISO_DATE))
          .build()
      }
      .headers { it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__CHARGE__RO")) }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath(("$.charges[?(@.chargeUuid == '${beforeFromDateCharge.chargeUuid}')]"))
      .doesNotExist()
      .jsonPath(("$.charges[?(@.chargeUuid == '${afterFromDateCharge.chargeUuid}')]"))
      .exists()
  }

  @Test
  fun `can filter using to date`() {
    val prisonerId = "PRI123"
    val toDate = LocalDate.now().minusDays(10)
    val (_, beforeCourtCase) = createCourtCase(
      createCourtCase = DpsDataCreator.dpsCreateNonSentencedCourtCase(
        prisonerId = prisonerId,
        appearances = listOf(
          DpsDataCreator.dpsCreateNonSentencedCourtAppearance(appearanceDate = toDate.minusDays(10), nextCourtAppearance = null),
        ),
      ),
    )
    val beforeToDateCharge = beforeCourtCase.appearances.first().charges.first()
    val (_, afterCourtCase) = createCourtCase(
      createCourtCase = DpsDataCreator.dpsCreateNonSentencedCourtCase(
        prisonerId = prisonerId,
        appearances = listOf(
          DpsDataCreator.dpsCreateNonSentencedCourtAppearance(appearanceDate = toDate.plusDays(2), nextCourtAppearance = null),
        ),
      ),
    )
    val afterToDateCharge = afterCourtCase.appearances.first().charges.first()
    webTestClient
      .get()
      .uri {
        it.path("/person/$prisonerId/charges")
          .queryParam("toDate", toDate.format(DateTimeFormatter.ISO_DATE))
          .build()
      }
      .headers { it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__CHARGE__RO")) }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath(("$.charges[?(@.chargeUuid == '${beforeToDateCharge.chargeUuid}')]"))
      .exists()
      .jsonPath(("$.charges[?(@.chargeUuid == '${afterToDateCharge.chargeUuid}')]"))
      .doesNotExist()
  }
}
