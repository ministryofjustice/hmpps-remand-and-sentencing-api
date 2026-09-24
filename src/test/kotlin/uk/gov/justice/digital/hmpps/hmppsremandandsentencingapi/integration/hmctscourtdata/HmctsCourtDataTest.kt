package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.hmctscourtdata

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
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
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.ChargeOutcome
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CourtAppearance
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CourtAppearanceOutcome
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.NextCourtAppearance
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.UploadedDocument
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.courtappearanceschedule.DeleteCourtAppearanceStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.event.EventSource
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.wiremock.CourtDataIngestionApiExtension
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.wiremock.DocumentManagementApiExtension
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ReferenceEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.Constants
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID
import java.util.stream.Stream

class HmctsCourtDataTest : IntegrationTestBase() {

  @Test
  fun `Test get appearance from hmcts data`() {
    val prisonerNumber = "PRIS123"
    val courtRegisterId = UUID.randomUUID().toString()
    val hmctsCourtHearing = HmctsCourtHearing(
      hearingId = HMCTS_HEARING_ID,
      courtName = "My court",
      courtCode = courtRegisterId,
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
          chargeId = UUID.randomUUID(),
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
              code = "RI",
              description = "Remand in custody",
            ),
          ),
        ),
        HmctsCourtCharge(
          chargeId = UUID.randomUUID(),
          listingNumber = 2,
          offenceLegislation = "Contrary to section 1(1) and 7 of the Theft Act 1968.",
          pleaDate = LocalDate.of(2026, 8, 15),
          pleaValue = "NOT_GUILTY",
          startDate = LocalDate.of(2026, 6, 14),
          endDate = null,
          title = "Another crime",
          wording = "Another crime",
          code = "X123ABC",
          results = listOf(
            HmctsCourtResult(
              code = "WDRN",
              description = "Withdrawn",
            ),
          ),
        ),
        HmctsCourtCharge(
          chargeId = UUID.randomUUID(),
          listingNumber = 3,
          offenceLegislation = "Contrary to section 1(1) and 7 of the Theft Act 1968.",
          pleaDate = LocalDate.of(2026, 8, 15),
          pleaValue = "NOT_GUILTY",
          startDate = LocalDate.of(2026, 6, 14),
          endDate = null,
          title = "Another crime",
          wording = "Another crime",
          code = "X123ABC",
          results = listOf(
            HmctsCourtResult(
              code = "WDRN",
              description = "Withdrawn",
            ),
            HmctsCourtResult(
              code = "RI",
              description = "Remand in custody",
            ),
          ),
        ),
      ),
      nextHearing = HmctsNextCourtHearing(
        courtName = "Central London County Court",
        hmctsCourtId = UUID.randomUUID(),
        hmppsCourtId = UUID.randomUUID().toString(),
        hearingDate = LocalDateTime.of(2026, 8, 15, 10, 0),
        hearingId = UUID.randomUUID(),
      ),
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
        hmctsCourtHearingId = hmctsCourtHearing.hearingId,
        outcome = null,
        courtCode = courtRegisterId,
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
          hmctsCourtHearingId = hmctsCourtHearing.nextHearing.hearingId,
        ),
        charges = listOf(
          Charge(
            chargeUuid = response.charges.first().chargeUuid,
            hmctsChargeId = hmctsCourtHearing.charges.first().chargeId,
            offenceCode = "TH68001",
            offenceStartDate = LocalDate.of(2026, 6, 15),
            offenceEndDate = LocalDate.of(2026, 7, 15),
            outcome = ChargeOutcome(
              outcomeUuid = UUID.fromString("315280e5-d53e-43b3-8ba6-44da25676ce2"),
              outcomeName = "Remand in custody",
              nomisCode = "4531",
              outcomeType = "REMAND",
              displayOrder = 570,
              dispositionCode = "INTERIM",
              hmctsCode = "RI",
              status = ReferenceEntityStatus.ACTIVE,
            ),
            aggravatingFactors = emptyList(), sentence = null, legacyData = null, mergedFromCase = null, createdAt = response.charges.first().createdAt, findingOfDomesticAbuse = null,
          ),
          Charge(
            chargeUuid = response.charges[1].chargeUuid,
            hmctsChargeId = hmctsCourtHearing.charges[1].chargeId,
            offenceCode = "X123ABC",
            offenceStartDate = LocalDate.of(2026, 6, 14),
            offenceEndDate = null,
            outcome = ChargeOutcome(
              outcomeUuid = UUID.fromString("6d2eb21d-ec02-48fa-9fcd-02e73b8e45ca"),
              outcomeName = "Withdrawn",
              nomisCode = "2051",
              outcomeType = "NON_CUSTODIAL",
              displayOrder = 150,
              dispositionCode = "FINAL",
              hmctsCode = "WDRN",
              status = ReferenceEntityStatus.ACTIVE,
            ),
            aggravatingFactors = emptyList(), sentence = null, legacyData = null, mergedFromCase = null, createdAt = response.charges[1].createdAt, findingOfDomesticAbuse = null,
          ),
          Charge(
            chargeUuid = response.charges[2].chargeUuid,
            hmctsChargeId = hmctsCourtHearing.charges[2].chargeId,
            offenceCode = "X123ABC",
            offenceStartDate = LocalDate.of(2026, 6, 14),
            offenceEndDate = null,
            outcome = null,
            aggravatingFactors = emptyList(), sentence = null, legacyData = null, mergedFromCase = null, createdAt = response.charges[2].createdAt, findingOfDomesticAbuse = null,
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

  @Test
  fun `Test get appearance from with offence outcomes all the same hmcts data`() {
    val prisonerNumber = "PRIS123"
    val courtRegisterId = UUID.randomUUID().toString()
    val hmctsCourtHearing = HmctsCourtHearing(
      hearingId = HMCTS_HEARING_ID,
      courtName = "My court",
      courtCode = courtRegisterId,
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
              code = "RI",
              description = "Remand in custody",
            ),
          ),
          chargeId = UUID.randomUUID(),
        ),
        HmctsCourtCharge(
          listingNumber = 2,
          offenceLegislation = "Contrary to section 1(1) and 7 of the Theft Act 1968.",
          pleaDate = LocalDate.of(2026, 8, 15),
          pleaValue = "NOT_GUILTY",
          startDate = LocalDate.of(2026, 6, 14),
          endDate = null,
          title = "Another crime",
          wording = "Another crime",
          code = "X123ABC",
          results = listOf(
            HmctsCourtResult(
              code = "RI",
              description = "Remand in custody",
            ),
          ),
          chargeId = UUID.randomUUID(),
        ),
      ),
      nextHearing = null,
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
        outcome = CourtAppearanceOutcome(
          outcomeUuid = UUID.fromString("2f585681-7b1a-44fb-a0cb-f9a4b1d9cda8"),
          outcomeName = "Remand in custody",
          nomisCode = "4531",
          outcomeType = "REMAND",
          displayOrder = 70,
          relatedChargeOutcomeUuid = UUID.fromString("315280e5-d53e-43b3-8ba6-44da25676ce2"),
          isSubList = false, dispositionCode = "INTERIM", status = ReferenceEntityStatus.ACTIVE, warrantType = "NON_SENTENCING",
        ),
        courtCode = courtRegisterId,
        courtCaseReference = "ABC123",
        criminalAppealOfficeReference = null,
        appearanceDate = LocalDate.parse("2026-01-01"),
        warrantType = "NON_SENTENCING",
        nextCourtAppearance = null,
        hmctsCourtHearingId = hmctsCourtHearing.hearingId,
        charges = listOf(
          Charge(
            chargeUuid = response.charges.first().chargeUuid,
            hmctsChargeId = hmctsCourtHearing.charges.first().chargeId,
            offenceCode = "TH68001",
            offenceStartDate = LocalDate.of(2026, 6, 15),
            offenceEndDate = LocalDate.of(2026, 7, 15),
            outcome = ChargeOutcome(
              outcomeUuid = UUID.fromString("315280e5-d53e-43b3-8ba6-44da25676ce2"),
              outcomeName = "Remand in custody",
              nomisCode = "4531",
              outcomeType = "REMAND",
              displayOrder = 570,
              dispositionCode = "INTERIM",
              hmctsCode = "RI",
              status = ReferenceEntityStatus.ACTIVE,
            ),
            aggravatingFactors = emptyList(), sentence = null, legacyData = null, mergedFromCase = null, createdAt = response.charges.first().createdAt, findingOfDomesticAbuse = null,
          ),
          Charge(
            chargeUuid = response.charges[1].chargeUuid,
            hmctsChargeId = hmctsCourtHearing.charges[1].chargeId,
            offenceCode = "X123ABC",
            offenceStartDate = LocalDate.of(2026, 6, 14),
            offenceEndDate = null,
            outcome = ChargeOutcome(
              outcomeUuid = UUID.fromString("315280e5-d53e-43b3-8ba6-44da25676ce2"),
              outcomeName = "Remand in custody",
              nomisCode = "4531",
              outcomeType = "REMAND",
              displayOrder = 570,
              dispositionCode = "INTERIM",
              hmctsCode = "RI",
              status = ReferenceEntityStatus.ACTIVE,
            ),
            aggravatingFactors = emptyList(), sentence = null, legacyData = null, mergedFromCase = null, createdAt = response.charges[1].createdAt, findingOfDomesticAbuse = null,
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

  @Test
  fun `Test get minimal appearance from hmcts data`() {
    val prisonerNumber = "PRIS123"
    val hmctsCourtHearing = HmctsCourtHearing(
      hearingId = HMCTS_HEARING_ID,
      courtName = "My court",
      courtCode = null,
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
          chargeId = UUID.randomUUID(),
          listingNumber = null,
          offenceLegislation = null,
          pleaDate = null,
          pleaValue = null,
          startDate = LocalDate.of(2026, 6, 15),
          endDate = null,
          title = "Theft from the person of another",
          wording = "Theft from the person of another",
          code = "TH68001",
          results = listOf(
            HmctsCourtResult(
              code = "XXX",
              description = "Unknown outcome",
            ),
          ),
        ),
      ),
      nextHearing = null,
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
        hmctsCourtHearingId = hmctsCourtHearing.hearingId,
        outcome = null,
        courtCode = Constants.nilUUID.toString(),
        courtCaseReference = "ABC123",
        criminalAppealOfficeReference = null,
        appearanceDate = LocalDate.parse("2026-01-01"),
        warrantType = "NON_SENTENCING",
        nextCourtAppearance = null,
        charges = listOf(
          Charge(
            chargeUuid = response.charges.first().chargeUuid,
            hmctsChargeId = hmctsCourtHearing.charges.first().chargeId,
            offenceCode = "TH68001",
            offenceStartDate = LocalDate.of(2026, 6, 15),
            offenceEndDate = null,
            outcome = null,
            aggravatingFactors = emptyList(), sentence = null, legacyData = null, mergedFromCase = null, createdAt = response.charges.first().createdAt, findingOfDomesticAbuse = null,
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

  @ParameterizedTest(name = "Tests that offence results are mapped from hmcts {0} to ras outcome {1}")
  @MethodSource("offenceResultsProvider")
  fun `Test get offence outcome mappings`(hmctsCode: String, expectedRasOutcomeText: String) {
    val prisonerNumber = "PRIS123"
    val courtRegisterId = UUID.randomUUID().toString()
    val hmctsCourtHearing = HmctsCourtHearing(
      hearingId = HMCTS_HEARING_ID,
      courtName = "My court",
      courtCode = courtRegisterId,
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
          chargeId = UUID.randomUUID(),
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
              code = hmctsCode,
              description = hmctsCode,
            ),
          ),
        ),
      ),
      nextHearing = null,
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

    Assertions.assertThat(response.charges.size).isEqualTo(1)
    Assertions.assertThat(response.charges.first().outcome?.outcomeName).isEqualTo(expectedRasOutcomeText)
    Assertions.assertThat(response.charges.first().outcome?.hmctsCode).isEqualTo(hmctsCode)
  }

  companion object {
    val HMCTS_HEARING_ID = UUID.randomUUID()
    val REMAND_WARRANT_DOCUMENT_ID = UUID.randomUUID()

    @JvmStatic
    fun offenceResultsProvider(): Stream<Arguments> = Stream.of(
      Arguments.of("RI", "Remand in custody"),
      Arguments.of("CCII", "Send to Crown Court for trial "),
      Arguments.of("RC", "Remand on conditional bail"),
      Arguments.of("CCSI", "Commit to Crown Court for sentence in custody"),
      Arguments.of("NSP", "No separate penalty"),
//      Arguments.of("FCOMP", "Pay compensation"), Only exists in environment data.
      Arguments.of("CTROF", "Lie on file"),
      Arguments.of("SUSPS", "Suspended imprisonment"),
      Arguments.of("DISCH", "Discharged"),
      Arguments.of("WDRN", "Withdrawn"),
      Arguments.of("REMUB", "Remand on unconditional bail"),
      Arguments.of("A", "Adjourned"),
      Arguments.of("COEW", "Community order"),
      Arguments.of("DISC", "Discontinuance"),
      Arguments.of("DISM", "Dismissed"),
    )
  }
}
