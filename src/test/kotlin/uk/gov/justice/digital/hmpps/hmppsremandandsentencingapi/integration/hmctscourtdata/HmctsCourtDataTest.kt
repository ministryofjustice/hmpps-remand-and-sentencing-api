package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.hmctscourtdata

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.dto.CourtRegister
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.dto.DocumentManagementApiDocument
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.dto.HmctsCourHearing
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.dto.HmctsCourHearingDocument
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CourtAppearance
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.UploadedDocument
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.courtappearanceschedule.DeleteCourtAppearanceStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.event.EventSource
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.wiremock.CourtDataIngestionApiExtension
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.wiremock.CourtRegisterApiExtension
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.wiremock.DocumentManagementApiExtension
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class HmctsCourtDataTest : IntegrationTestBase() {

  @Test
  fun `Test get appearance from hmcts data`() {
    val hmctsCourtHearing = HmctsCourHearing(
      hearingId = HMCTS_HEARING_ID,
      courtName = "My court",
      courtId = UUID.randomUUID(),
      hearingDate = LocalDateTime.of(2026, 1, 1, 1, 1, 1),
      caseReferences = listOf("ABC123", "EFG456"),
      hearingType = "First hearing",
      documents = listOf(
        HmctsCourHearingDocument(
          "REMAND_WARRANT",
          REMAND_WARRANT_DOCUMENT_ID,
        ),
      ),
    )
    val courtRegister = CourtRegister(
      courtName = "My court",
      courtId = UUID.randomUUID().toString(),
      courtDescription = "My court description",
    )
    CourtDataIngestionApiExtension.courtDataIngestionApi.stubCourtHearing(
      hmctsCourtHearing,
    )
    DocumentManagementApiExtension.documentManagementApi.stubGetDocumentsFromIds(
      listOf(
        DocumentManagementApiDocument(
          REMAND_WARRANT_DOCUMENT_ID,
          documentFilename = "RemandWarrant.pdf",
        ),
      ),
    )
    CourtRegisterApiExtension.courtRegisterApi.stubGetHmctsCourtRegister(
      hmctsCourtHearing.courtId,
      courtRegister,
    )

    val response = webTestClient
      .get()
      .uri("/hmcts-court-data/${HMCTS_HEARING_ID}/appearance")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus().isOk
      .returnResult(CourtAppearance::class.java)
      .responseBody.blockFirst()!!

    Assertions.assertThat(response).isEqualTo(
      CourtAppearance(
        appearanceUuid = response.appearanceUuid, // Random UUID
        outcome = null,
        courtCode = courtRegister.courtId,
        courtCaseReference = "ABC123",
        criminalAppealOfficeReference = null,
        appearanceDate = LocalDate.parse("2026-01-01"),
        warrantType = "NON_SENTENCING",
        nextCourtAppearance = null,
        charges = emptyList(),
        overallSentenceLength = null,
        overallConvictionDate = null,
        legacyData = null,
        documents = listOf(
          UploadedDocument(
            documentUUID = REMAND_WARRANT_DOCUMENT_ID,
            documentType = "HMCTS_WARRANT",
            fileName = "RemandWarrant.pdf",
          ),
        ),
        source = EventSource.DPS,
        deleteStatus = DeleteCourtAppearanceStatus.SUPPORTED,
      ),
    )
  }

  companion object {
    val HMCTS_HEARING_ID = UUID.randomUUID()
    val REMAND_WARRANT_DOCUMENT_ID = UUID.randomUUID()
  }
}
