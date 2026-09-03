package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.hmctscourtdata

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.dto.CourtRegister
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.dto.DocumentManagementApiDocument
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.dto.HmctsCourHearingDocument
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.dto.HmctsCourtCharge
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.dto.HmctsCourtHearing
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.dto.HmctsCourtResult
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.dto.HmctsNextCourtHearing
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.AppearanceType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.Charge
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CourtAppearance
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.NextCourtAppearance
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.UploadedDocument
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.courtappearanceschedule.DeleteCourtAppearanceStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.event.EventSource
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.wiremock.CourtDataIngestionApiExtension
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.wiremock.CourtRegisterApiExtension
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.wiremock.DocumentManagementApiExtension
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.Constants
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID

class HmctsCourtDataTest : IntegrationTestBase() {

  @Test
  fun `Test get appearance from hmcts data`() {
    val prisonerNumber = "PRIS123"
    val hmctsCourtHearing = HmctsCourtHearing(
      hearingId = HMCTS_HEARING_ID,
      courtName = "My court",
      courtId = UUID.randomUUID(),
      hearingDate = LocalDate.of(2026, 1, 1),
      caseReferences = listOf("ABC123", "EFG456"),
      hearingType = "First hearing",
      documents = listOf(
        HmctsCourHearingDocument(
          "REMAND_WARRANT",
          REMAND_WARRANT_DOCUMENT_ID,
        ),
      ),
      charges = listOf(
        HmctsCourtCharge(
          listingNumber = 1,
          offenceLegislation = "Contrary to section 1(1) and 7 of the Theft Act 1968.",
          pleaDate = LocalDate.of(2026, 8, 15),
          pleaValue = "NOT_GUILTY",
          startDate = LocalDate.of(2026, 6, 15),
          endDate = LocalDate.of(2026, 7, 15),
          title = "Theft from the person of another",
          wording = "Theft from the person of another",
          code = "TH68001",
          results = listOf(
            HmctsCourtResult(
              code = "RIB",
              description = "Remanded in custody with bail direction",
            ),
          ),
        ),
      ),
      nextHearing = HmctsNextCourtHearing(
        courtName = "Central London County Court",
        hmctsCourtId = UUID.randomUUID(),
        hmppsCourtId = UUID.randomUUID().toString(),
        hearingDate = LocalDateTime.of(2026, 8, 15, 10, 0),
      ),
    )
    val courtRegister = CourtRegister(
      courtName = "My court",
      courtId = UUID.randomUUID().toString(),
      courtDescription = "My court description",
    )
    CourtDataIngestionApiExtension.courtDataIngestionApi.stubCourtHearing(
      hmctsCourtHearing,
      prisonerNumber,
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
      .uri("/hmcts-court-data/${HMCTS_HEARING_ID}/prisoner/$prisonerNumber/appearance")
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
        nextCourtAppearance = NextCourtAppearance(
          appearanceDate = LocalDate.of(2026, 8, 15),
          appearanceTime = LocalTime.of(10, 0),
          courtCode = hmctsCourtHearing.nextHearing?.hmppsCourtId!!,
          appearanceType = AppearanceType(appearanceTypeUuid = Constants.nilUUID, description = "Unknown appearance type", displayOrder = 1, hasSubtypes = false),
          futureSkeletonAppearanceUuid = Constants.nilUUID,
          courtAppearanceSubType = null,
        ),
        charges = listOf(
          Charge(
            chargeUuid = response.charges.first().chargeUuid,
            offenceCode = "TH68001",
            offenceStartDate = LocalDate.of(2026, 6, 15),
            offenceEndDate = LocalDate.of(2026, 7, 15),
            outcome = null,
            aggravatingFactors = emptyList(), sentence = null, legacyData = null, mergedFromCase = null, createdAt = response.charges.first().createdAt,
          ),
        ),
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
        periodLengths = emptyList(),
      ),
    )
  }

  companion object {
    val HMCTS_HEARING_ID = UUID.randomUUID()
    val REMAND_WARRANT_DOCUMENT_ID = UUID.randomUUID()
  }
}
