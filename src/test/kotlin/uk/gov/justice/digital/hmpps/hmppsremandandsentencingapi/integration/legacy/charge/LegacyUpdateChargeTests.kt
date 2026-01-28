package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.charge

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CourtCase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateCourtCaseResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.util.DataCreator
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.DpsDataCreator
import java.time.LocalDate
import java.util.UUID

class LegacyUpdateChargeTests : IntegrationTestBase() {

  @Test
  fun `update charge in all court appearances`() {
    val dpsCharge = DpsDataCreator.dpsCreateCharge(sentence = null, outcomeUuid = UUID.fromString("315280e5-d53e-43b3-8ba6-44da25676ce2"))
    val firstAppearance = DpsDataCreator.dpsCreateCourtAppearance(charges = listOf(dpsCharge))
    val secondAppearance = DpsDataCreator.dpsCreateCourtAppearance(charges = listOf(dpsCharge.copy(outcomeUuid = UUID.fromString("8976a19b-ab84-4881-b8c7-cf7b1978a262"))))
    val createCourtCase = DpsDataCreator.dpsCreateCourtCase(appearances = listOf(firstAppearance, secondAppearance))
    val courtCaseResponse = webTestClient
      .post()
      .uri("/court-case")
      .bodyValue(createCourtCase)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
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
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
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
  fun `update charges in source court case when they have been linked`() {
    val charge = DataCreator.migrationCreateCharge(chargeNOMISId = 889)
    val sourceCourtCase = DataCreator.migrationCreateCourtCase(appearances = listOf(DataCreator.migrationCreateCourtAppearance(eventId = 556, charges = listOf(charge))))
    val targetCourtCase = DataCreator.migrationCreateCourtCase(caseId = 2, appearances = listOf(DataCreator.migrationCreateCourtAppearance(eventId = 560, charges = listOf(charge.copy(mergedFromCaseId = sourceCourtCase.caseId, mergedFromDate = LocalDate.now())))))
    val courtCases = DataCreator.migrationCreateCourtCases(courtCases = listOf(sourceCourtCase, targetCourtCase))
    val response = migrateCases(courtCases)
    val sourceCourtCaseUuid = response.courtCases.first { it.caseId == sourceCourtCase.caseId }.courtCaseUuid
    val chargeUuid = response.charges.first { it.chargeNOMISId == charge.chargeNOMISId }.chargeUuid

    val toUpdate = DataCreator.legacyUpdateWholeCharge(offenceCode = "ANOTHERCODE")
    webTestClient
      .put()
      .uri("/legacy/charge/$chargeUuid")
      .bodyValue(toUpdate)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_CHARGE_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isNoContent

    webTestClient
      .get()
      .uri {
        it.path("/court-case/paged/search")
          .queryParam("prisonerId", courtCases.prisonerId)
          .build()
      }
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.content[?(@.courtCaseUuid == '$sourceCourtCaseUuid')].latestCourtAppearance.charges[?(@.chargeUuid == '$chargeUuid')].offenceCode")
      .isEqualTo(toUpdate.offenceCode)
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
