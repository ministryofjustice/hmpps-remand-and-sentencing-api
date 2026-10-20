package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.person

import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.UpdateCourtCaseStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.CourtCaseEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.DpsDataCreator

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
}
