package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.courtappearance

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.DpsDataCreator

class DeleteCourtAppearanceTests : IntegrationTestBase() {
  @Test
  fun `delete court appearance should change court appearance status to be deleted and court case as well if no more court appearance is ACTIVE`() {
    // Given a court appearance exists
    val appearance = DpsDataCreator.dpsCreateCourtAppearance()
    val courtCase = createCourtCase(DpsDataCreator.dpsCreateCourtCase(appearances = listOf(appearance)))
    val createdAppearance = courtCase.second.appearances.first()

    val appearanceId = courtAppearanceRepository.findByAppearanceUuid(createdAppearance.appearanceUuid)!!.id

    // When the court appearance is deleted
    webTestClient.delete()
      .uri("/court-appearance/${createdAppearance.appearanceUuid}?courtCaseUuid=${courtCase.first}")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isNoContent
      .expectBody()

    val deletedAppearance = courtAppearanceRepository.findById(appearanceId).get()
    assertEquals(EntityStatus.DELETED, deletedAppearance.statusId)

    val deletedCourtCase = courtCaseRepository.findByCaseUniqueIdentifier(courtCase.first)
    assertEquals(EntityStatus.DELETED, deletedCourtCase?.statusId)
  }

  @Test
  fun `court case status should still be active if any court appearance is active`() {
    // Given a court appearance exists
    val appearance1 = DpsDataCreator.dpsCreateCourtAppearance()
    val appearance2 = DpsDataCreator.dpsCreateCourtAppearance()
    val courtCase = createCourtCase(DpsDataCreator.dpsCreateCourtCase(appearances = listOf(appearance1, appearance2)))
    val createdAppearance = courtCase.second.appearances.first()

    val appearanceId = courtAppearanceRepository.findByAppearanceUuid(createdAppearance.appearanceUuid)!!.id

    // When the court appearance is deleted
    webTestClient.delete()
      .uri("/court-appearance/${createdAppearance.appearanceUuid}?courtCaseUuid=${courtCase.first}")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isNoContent
      .expectBody()

    // Then the court case status should still be active
    val deletedAppearance = courtAppearanceRepository.findById(appearanceId).get()
    assertEquals(EntityStatus.DELETED, deletedAppearance.statusId)

    val deletedCourtCase = courtCaseRepository.findByCaseUniqueIdentifier(courtCase.first)
    assertEquals(EntityStatus.ACTIVE, deletedCourtCase?.statusId)
  }
}
