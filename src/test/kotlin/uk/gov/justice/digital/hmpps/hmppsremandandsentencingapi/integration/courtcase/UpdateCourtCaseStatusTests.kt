package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.courtcase

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.UpdateCourtCaseStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.util.DataCreator
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.CourtCaseEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.DpsDataCreator
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
      .bodyValue(UpdateCourtCaseStatus(status = CourtCaseEntityStatus.ACTIVE, reason = "This should be ignored"))
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
  fun `cannot change the status of a DELETED court case`() {
    val appearance = DpsDataCreator.dpsCreateNonSentencedCourtAppearance(
      charges = listOf(DpsDataCreator.dpsCreateNonSentencedCharge()),
    )
    val courtCase = createCourtCase(DpsDataCreator.dpsCreateNonSentencedCourtCase(appearances = listOf(appearance)))
    val createdAppearance = courtCase.second.appearances.first()

    webTestClient
      .delete()
      .uri("/court-appearance/${createdAppearance.appearanceUuid}")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isNoContent

    assertThat(courtCaseRepository.findByCaseUniqueIdentifier(courtCase.first)!!.statusId).isEqualTo(CourtCaseEntityStatus.DELETED)

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
      .isBadRequest

    assertThat(courtCaseRepository.findByCaseUniqueIdentifier(courtCase.first)!!.statusId).isEqualTo(CourtCaseEntityStatus.DELETED)
  }

  @Test
  fun `cannot change the status of a MERGED court case`() {
    val sourceCourtCase = DataCreator.migrationCreateCourtCase()
    val targetCourtCase = DataCreator.migrationCreateCourtCase(caseId = 2)
    val courtCases = DataCreator.migrationCreateCourtCases(courtCases = listOf(sourceCourtCase, targetCourtCase))
    val response = migrateCases(courtCases)
    val sourceCourtCaseUuid = response.courtCases.first { it.caseId == sourceCourtCase.caseId }.courtCaseUuid
    val targetCourtCaseUuid = response.courtCases.first { it.caseId == targetCourtCase.caseId }.courtCaseUuid

    linkCases(sourceCourtCaseUuid, targetCourtCaseUuid)

    assertThat(courtCaseRepository.findByCaseUniqueIdentifier(sourceCourtCaseUuid)!!.statusId).isEqualTo(CourtCaseEntityStatus.MERGED)

    webTestClient
      .put()
      .uri("/court-case/$sourceCourtCaseUuid/status")
      .bodyValue(UpdateCourtCaseStatus(status = CourtCaseEntityStatus.ACTIVE, reason = null))
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isBadRequest

    assertThat(courtCaseRepository.findByCaseUniqueIdentifier(sourceCourtCaseUuid)!!.statusId).isEqualTo(CourtCaseEntityStatus.MERGED)
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
