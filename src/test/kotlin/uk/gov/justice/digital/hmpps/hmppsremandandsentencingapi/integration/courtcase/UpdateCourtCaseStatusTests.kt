package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.courtcase

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.UpdateCourtCaseStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.CourtCaseEntityStatus
import java.util.UUID

class UpdateCourtCaseStatusTests : IntegrationTestBase() {

  @Test
  fun `mark court case as inactive persists status and reason and emits court-case updated event`() {
    val courtCase = createCourtCase()

    webTestClient
      .put()
      .uri("/court-case/${courtCase.first}/status")
      .bodyValue(UpdateCourtCaseStatus(status = CourtCaseEntityStatus.INACTIVE, reason = "Duplicate case raised in error"))
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isNoContent

    val updatedCourtCase = courtCaseRepository.findByCaseUniqueIdentifier(courtCase.first)!!
    assertThat(updatedCourtCase.statusId).isEqualTo(CourtCaseEntityStatus.INACTIVE)
    assertThat(updatedCourtCase.reason).isEqualTo("Duplicate case raised in error")

    val courtCaseLogs = courtCaseHistoryRepository.findAll().filter { it.caseUniqueIdentifier == courtCase.first }
    assertThat(courtCaseLogs).isNotEmpty
    assertThat(courtCaseLogs.last().statusId).isEqualTo(CourtCaseEntityStatus.INACTIVE)
    assertThat(courtCaseLogs.last().reason).isEqualTo("Duplicate case raised in error")

    val eventTypes = getMessages(1).map { it.eventType }
    assertThat(eventTypes).contains("court-case.updated")
  }

  @Test
  fun `mark court case as active persists status and clears reason`() {
    val courtCase = createCourtCase()
    webTestClient
      .put()
      .uri("/court-case/${courtCase.first}/status")
      .bodyValue(UpdateCourtCaseStatus(status = CourtCaseEntityStatus.INACTIVE, reason = "Duplicate case raised in error"))
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isNoContent
    purgeQueues()

    webTestClient
      .put()
      .uri("/court-case/${courtCase.first}/status")
      .bodyValue(UpdateCourtCaseStatus(status = CourtCaseEntityStatus.ACTIVE, reason = null))
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isNoContent

    val updatedCourtCase = courtCaseRepository.findByCaseUniqueIdentifier(courtCase.first)!!
    assertThat(updatedCourtCase.statusId).isEqualTo(CourtCaseEntityStatus.ACTIVE)
    assertThat(updatedCourtCase.reason).isNull()

    val eventTypes = getMessages(1).map { it.eventType }
    assertThat(eventTypes).contains("court-case.updated")
  }

  @Test
  fun `cannot set status to a value other than ACTIVE or INACTIVE`() {
    val courtCase = createCourtCase()

    webTestClient
      .put()
      .uri("/court-case/${courtCase.first}/status")
      .bodyValue(UpdateCourtCaseStatus(status = CourtCaseEntityStatus.MERGED, reason = null))
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isBadRequest
  }

  @Test
  fun `unknown court case uuid returns not found`() {
    webTestClient
      .put()
      .uri("/court-case/${UUID.randomUUID()}/status")
      .bodyValue(UpdateCourtCaseStatus(status = CourtCaseEntityStatus.INACTIVE, reason = "Some reason"))
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isNotFound
  }

  @Test
  fun `no token results in unauthorized`() {
    webTestClient
      .put()
      .uri("/court-case/${UUID.randomUUID()}/status")
      .bodyValue(UpdateCourtCaseStatus(status = CourtCaseEntityStatus.INACTIVE, reason = "Some reason"))
      .headers {
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `token with incorrect role is forbidden`() {
    webTestClient
      .put()
      .uri("/court-case/${UUID.randomUUID()}/status")
      .bodyValue(UpdateCourtCaseStatus(status = CourtCaseEntityStatus.INACTIVE, reason = "Some reason"))
      .headers {
        it.authToken(roles = listOf("ROLE_OTHER_FUNCTION"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isForbidden
  }
}
