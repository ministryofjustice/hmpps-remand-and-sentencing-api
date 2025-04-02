package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.charge

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CourtCase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateCourtCaseResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.util.DataCreator
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.DpsDataCreator
import java.util.UUID

class LegacyUpdateChargeTests : IntegrationTestBase() {

  @Test
  fun `update charge in all court appearances`() {
    val dpsCharge = DpsDataCreator.dpsCreateCharge()
    val firstAppearance = DpsDataCreator.dpsCreateCourtAppearance(charges = listOf(dpsCharge))
    val secondAppearance = DpsDataCreator.dpsCreateCourtAppearance(charges = listOf(dpsCharge))
    val createCourtCase = DpsDataCreator.dpsCreateCourtCase(appearances = listOf(firstAppearance, secondAppearance))
    val courtCaseResponse = webTestClient
      .post()
      .uri("/court-case")
      .bodyValue(createCourtCase)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isCreated
      .returnResult(CreateCourtCaseResponse::class.java)
      .responseBody.blockFirst()!!

    val toUpdate = DataCreator.legacyUpdateWholeCharge(offenceCode = "ANOTHERCODE")
    webTestClient
      .put()
      .uri("/legacy/charge/${dpsCharge.chargeUuid}")
      .bodyValue(toUpdate)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_CHARGE_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isNoContent

    val courtCase = webTestClient
      .get()
      .uri("/court-case/${courtCaseResponse.courtCaseUuid}")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isOk
      .returnResult(CourtCase::class.java)
      .responseBody.blockFirst()!!

    courtCase.appearances.flatMap { it.charges }
      .forEach { charge ->
        Assertions.assertEquals(toUpdate.offenceCode, charge.offenceCode)
      }
  }

  @Test
  fun `must not update appearance when no court appearance exists`() {
    val toUpdate = DataCreator.legacyCreateCharge()
    webTestClient
      .put()
      .uri("/legacy/charge/${UUID.randomUUID()}")
      .bodyValue(toUpdate)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_CHARGE_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isNotFound
  }

  @Test
  fun `no token results in unauthorized`() {
    val toUpdate = DataCreator.legacyCreateCharge()
    webTestClient
      .put()
      .uri("/legacy/charge/${UUID.randomUUID()}")
      .bodyValue(toUpdate)
      .headers {
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `token with incorrect role is forbidden`() {
    val toUpdate = DataCreator.legacyCreateCharge()
    webTestClient
      .put()
      .uri("/legacy/charge/${UUID.randomUUID()}")
      .bodyValue(toUpdate)
      .headers {
        it.authToken(roles = listOf("ROLE_OTHER_FUNCTION"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isForbidden
  }
}
