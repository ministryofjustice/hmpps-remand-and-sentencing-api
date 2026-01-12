package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.courtcase

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CourtCase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.util.DataCreator
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.DpsDataCreator
import java.util.UUID

class LegacyUpdateCourtCaseTests : IntegrationTestBase() {

  @Test
  fun `update court case`() {
    val createdCourtCase = createLegacyCourtCase()
    val toUpdate = DataCreator.legacyCreateCourtCase(active = false)
    webTestClient
      .put()
      .uri("/legacy/court-case/${createdCourtCase.first}")
      .bodyValue(toUpdate)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_COURT_CASE_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isNoContent

    val message = getMessages(1)[0]
    Assertions.assertThat(message.eventType).isEqualTo("court-case.updated")
    Assertions.assertThat(message.additionalInformation.get("source").asText()).isEqualTo("NOMIS")
  }

  @Test
  fun `write back booking id does not override case references`() {
    val (courtCaseUuid, dpsCourtCase) = createCourtCase()
    val toUpdate = DataCreator.legacyCreateCourtCase(bookingId = 43869)
    webTestClient
      .put()
      .uri("/legacy/court-case/$courtCaseUuid")
      .bodyValue(toUpdate)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_COURT_CASE_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isNoContent
    val retrievedCourtCase = webTestClient
      .get()
      .uri("/court-case/$courtCaseUuid")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
      }
      .exchange()
      .expectStatus()
      .isOk
      .returnResult(CourtCase::class.java)
      .responseBody.blockFirst()!!

    Assertions.assertThat(retrievedCourtCase.legacyData!!.caseReferences).extracting<String> { it.offenderCaseReference }.containsExactlyInAnyOrder(dpsCourtCase.appearances.first().courtCaseReference)
  }

  @Test
  fun `update immigration detention court case`() {
    val createImmigrationDetention = DpsDataCreator.dpsCreateImmigrationDetention()
    createImmigrationDetention(createImmigrationDetention)
    val courtCase = courtCaseRepository.findAllByPrisonerId(createImmigrationDetention.prisonerId).first()
    val toUpdate = DataCreator.legacyCreateCourtCase(bookingId = 43869)
    webTestClient
      .put()
      .uri("/legacy/court-case/${courtCase.caseUniqueIdentifier}")
      .bodyValue(toUpdate)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_COURT_CASE_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isNoContent
  }

  @Test
  fun `no token results in unauthorized`() {
    val legacyCreateCourtCase = DataCreator.legacyCreateCourtCase()
    webTestClient
      .put()
      .uri("/legacy/court-case/${UUID.randomUUID()}")
      .bodyValue(legacyCreateCourtCase)
      .headers {
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `token with incorrect role is forbidden`() {
    val legacyCreateCourtCase = DataCreator.legacyCreateCourtCase()
    webTestClient
      .put()
      .uri("/legacy/court-case/${UUID.randomUUID()}")
      .bodyValue(legacyCreateCourtCase)
      .headers {
        it.authToken(roles = listOf("ROLE_OTHER_FUNCTION"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isForbidden
  }
}
