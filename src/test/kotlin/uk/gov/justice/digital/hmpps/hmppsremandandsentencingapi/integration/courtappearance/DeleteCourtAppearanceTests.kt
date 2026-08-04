package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.courtappearance

import org.assertj.core.api.Assertions
import org.awaitility.kotlin.await
import org.awaitility.kotlin.untilAsserted
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.event.HmppsPeriodLengthMessage
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.wiremock.DocumentManagementApiExtension.Companion.documentManagementApi
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.CourtAppearanceEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.CourtCaseEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.PeriodLengthType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.DpsDataCreator
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.DpsDataCreator.Factory.dpsCreateCourtAppearance
import java.time.LocalDate

class DeleteCourtAppearanceTests : IntegrationTestBase() {
  @Test
  fun `delete court appearance should change court appearance status to be deleted and court case as well if no more court appearance is ACTIVE`() {
    // Given a court appearance exists
    val appearance = DpsDataCreator.dpsCreateNonSentencedCourtAppearance(
      charges = listOf(
        DpsDataCreator.dpsCreateNonSentencedCharge(),
        DpsDataCreator.dpsCreateNonSentencedCharge(),
      ),
    )
    val courtCase = createCourtCase(DpsDataCreator.dpsCreateNonSentencedCourtCase(appearances = listOf(appearance)))
    val createdAppearance = courtCase.second.appearances.first()

    // When the court appearance is deleted
    webTestClient.delete()
      .uri("/court-appearance/${createdAppearance.appearanceUuid}")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isNoContent
      .expectBody()

    val deletedAppearance = courtAppearanceRepository.findByAppearanceUuid(createdAppearance.appearanceUuid)!!
    assertEquals(CourtAppearanceEntityStatus.DELETED, deletedAppearance.statusId)
    assertEquals(0, appearanceChargeRepository.countByAppearanceAppearanceUuid(createdAppearance.appearanceUuid))

    val deletedCourtCase = courtCaseRepository.findByCaseUniqueIdentifier(courtCase.first)
    assertEquals(CourtCaseEntityStatus.DELETED, deletedCourtCase?.statusId)
  }

  @Test
  fun `court case status should still be active if any court appearance is active`() {
    // Given a court appearance exists
    val appearance1 = DpsDataCreator.dpsCreateNonSentencedCourtAppearance()
    val appearance2 = DpsDataCreator.dpsCreateNonSentencedCourtAppearance()
    val courtCase = createCourtCase(DpsDataCreator.dpsCreateNonSentencedCourtCase(appearances = listOf(appearance1, appearance2)))
    val createdAppearance = courtCase.second.appearances.first()

    // When the court appearance is deleted
    webTestClient.delete()
      .uri("/court-appearance/${createdAppearance.appearanceUuid}")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isNoContent
      .expectBody()

    // Then the court case status should still be active
    val deletedAppearance = courtAppearanceRepository.findByAppearanceUuid(createdAppearance.appearanceUuid)!!
    assertEquals(CourtAppearanceEntityStatus.DELETED, deletedAppearance.statusId)

    val deletedCourtCase = courtCaseRepository.findByCaseUniqueIdentifier(courtCase.first)
    assertEquals(CourtCaseEntityStatus.ACTIVE, deletedCourtCase?.statusId)
  }

  @Test
  fun `deleting a court appearance updates document metadata to Deleted`() {
    val (uploadedDocument) = uploadDocument()
    documentManagementApi.stubUpdateDocumentStatus(uploadedDocument.documentUUID.toString())
    val appearance = DpsDataCreator.dpsCreateNonSentencedCourtAppearance(documents = listOf(uploadedDocument))
    val courtCase = createCourtCase(DpsDataCreator.dpsCreateNonSentencedCourtCase(appearances = listOf(appearance)))
    val createdAppearance = courtCase.second.appearances.first()

    documentManagementApi.stubUpdateDocumentStatus(uploadedDocument.documentUUID.toString())

    webTestClient.delete()
      .uri("/court-appearance/${createdAppearance.appearanceUuid}")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isNoContent
      .expectBody()

    await untilAsserted {
      verifyDocumentMetadataUpdated(uploadedDocument.documentUUID, "DELETED")
    }
  }

  @Test
  fun `deleting a court appearance should finish even if updating document fails`() {
    val (uploadedDocument) = uploadDocument()
    documentManagementApi.stubUpdateDocumentStatusToFail(uploadedDocument.documentUUID.toString())
    val appearance = DpsDataCreator.dpsCreateNonSentencedCourtAppearance(documents = listOf(uploadedDocument))
    val courtCase = createCourtCase(DpsDataCreator.dpsCreateNonSentencedCourtCase(appearances = listOf(appearance)))
    val createdAppearance = courtCase.second.appearances.first()

    webTestClient.delete()
      .uri("/court-appearance/${createdAppearance.appearanceUuid}")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isNoContent
      .expectBody()

    await untilAsserted {
      verifyDocumentMetadataUpdated(uploadedDocument.documentUUID, "DELETED")
    }

    val deletedAppearance = courtAppearanceRepository.findByAppearanceUuid(createdAppearance.appearanceUuid)!!
    assertEquals(CourtAppearanceEntityStatus.DELETED, deletedAppearance.statusId)
  }

  @Test
  fun `deleting appearance with period length associated to a sentence emits period length deleted event`() {
    val sentencedCharge = DpsDataCreator.dpsCreateCharge()
    val sentencingAppearance = dpsCreateCourtAppearance(appearanceDate = LocalDate.now().minusDays(10), nextCourtAppearance = null, charges = listOf(sentencedCharge))
    val (courtCaseUuid) = createCourtCase(DpsDataCreator.dpsCreateCourtCase(appearances = listOf(sentencingAppearance)))
    val breachPeriodLength = DpsDataCreator.dpsCreatePeriodLength(type = PeriodLengthType.BREACH_OF_SUPERVISION_REQUIREMENTS, days = 41, years = null)
    val breachAppearance = dpsCreateCourtAppearance(
      courtCaseUuid = courtCaseUuid,
      warrantType = "BREACH_OF_SUPERVISION_REQUIREMENTS",
      overallSentenceLength = null,
      nextCourtAppearance = null,
      charges = listOf(
        sentencedCharge.copy(sentence = null),
      ),
      periodLengths = listOf(breachPeriodLength),
    )
    putCourtAppearance(breachAppearance.appearanceUuid, breachAppearance)
    purgeQueues()
    webTestClient.delete()
      .uri("/court-appearance/${breachAppearance.appearanceUuid}")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isNoContent
      .expectBody()

    val events = getMessages(4)
    Assertions.assertThat(events).anyMatch { it.eventType == "sentence.period-length.deleted" }
    val periodLengthInsertedEvent = events.first { it.eventType == "sentence.period-length.deleted" }
    val additionalInformation = objectMapper.treeToValue(periodLengthInsertedEvent.additionalInformation, HmppsPeriodLengthMessage::class.java)
    Assertions.assertThat(additionalInformation.courtAppearanceId).isEqualTo(sentencingAppearance.appearanceUuid.toString())
    Assertions.assertThat(additionalInformation.courtChargeId).isEqualTo(sentencedCharge.chargeUuid.toString())
  }

  @Test
  fun `deleting an appearance with a sentenced charge is not supported`() {
    val (_, createdCourtCase) = createCourtCase()
    val createdAppearance = createdCourtCase.appearances.first()
    webTestClient.delete()
      .uri("/court-appearance/${createdAppearance.appearanceUuid}")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isEqualTo(HttpStatus.CONFLICT)
  }
}
