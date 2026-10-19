package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.hmctscourtdata

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.dto.DocumentManagementApiDocument
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.dto.HmctsCourHearingDocument
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.dto.HmctsCourtCharge
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.dto.HmctsCourtHearing
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.dto.HmctsCourtResult
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.dto.HmctsNextCourtHearing
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.dto.ResultKeyValue
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.AggravatingFactor
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.AppearanceType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.Charge
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.ChargeOutcome
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CourtAppearance
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CourtAppearanceOutcome
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.NextCourtAppearance
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.PeriodLength
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.Sentence
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.SentenceType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.UploadedDocument
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.courtappearanceschedule.DeleteCourtAppearanceStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.event.EventSource
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.wiremock.CourtDataIngestionApiExtension
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.wiremock.DocumentManagementApiExtension
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.PeriodLengthType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ReferenceEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.SentenceEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.SentenceTypeClassification
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.HmctsCourtDataService
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.HmctsCourtDataService.Companion.blankNextCourtAppearance
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.Constants
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID
import java.util.stream.Stream

class HmctsCourtDataTest : IntegrationTestBase() {
  @BeforeEach
  fun setUp() {
    DocumentManagementApiExtension.documentManagementApi.stubGetDocumentsFromIds(
      listOf(
        DocumentManagementApiDocument(
          DOCUMENT_ID,
          documentFilename = "warrant.pdf",
        ),
      ),
    )
  }

  @Test
  fun `Test get remand appearance from hmcts data`() {
    val prisonerNumber = "PRIS123"
    val hmctsCourtHearing = HMCTS_REMAND_HEARING
    CourtDataIngestionApiExtension.courtDataIngestionApi.stubCourtHearing(
      hmctsCourtHearing,
      prisonerNumber,
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
        courtCode = COURT_REGISTER_ID,
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
            documentUUID = DOCUMENT_ID,
            documentType = "HMCTS_WARRANT",
            fileName = "warrant.pdf",
          ),
        ),
        source = EventSource.DPS,
        deleteStatus = DeleteCourtAppearanceStatus.SUPPORTED,
        periodLengths = emptyList(),
      ),
    )
  }

  @Test
  fun `Test get appearance from hmcts sentence data`() {
    val prisonerNumber = "PRIS123"
    val hmctsCourtHearing = HMCTS_SENTENCE_HEARING
    CourtDataIngestionApiExtension.courtDataIngestionApi.stubCourtHearing(
      hmctsCourtHearing,
      prisonerNumber,
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
        outcome = CourtAppearanceOutcome(
          outcomeUuid = UUID.fromString("62412083-9892-48c9-bf01-7864af4a8b3c"),
          outcomeName = "Imprisonment",
          nomisCode = "1002",
          outcomeType = "SENTENCING",
          displayOrder = 10,
          relatedChargeOutcomeUuid = UUID.fromString("f17328cf-ceaa-43c2-930a-26cf74480e18"),
          isSubList = false, dispositionCode = "FINAL", status = ReferenceEntityStatus.ACTIVE, warrantType = "SENTENCING",
        ),
        courtCode = COURT_REGISTER_ID,
        courtCaseReference = "ABC123",
        criminalAppealOfficeReference = null,
        appearanceDate = LocalDate.parse("2026-10-01"),
        warrantType = "SENTENCING",
        nextCourtAppearance = null,
        charges = listOf(
          Charge(
            chargeUuid = response.charges.first().chargeUuid,
            hmctsChargeId = hmctsCourtHearing.charges.first().chargeId,
            offenceCode = "TH68001",
            offenceStartDate = LocalDate.of(2026, 6, 15),
            offenceEndDate = LocalDate.of(2026, 7, 15),
            outcome = ChargeOutcome(
              outcomeUuid = UUID.fromString("f17328cf-ceaa-43c2-930a-26cf74480e18"),
              outcomeName = "Imprisonment",
              nomisCode = "1002",
              outcomeType = "SENTENCING",
              displayOrder = 10,
              dispositionCode = "FINAL",
              hmctsCode = "IMP",
              status = ReferenceEntityStatus.ACTIVE,
            ),
            aggravatingFactors = listOf(
              AggravatingFactor(code = "OAFPC", title = "Offence aggravated by foreign power condition being met", description = "Offence aggravated by foreign power condition being met", displayOrder = 10),
              AggravatingFactor(code = "OATC", title = "Offence Aggravated by Terrorist Connection", description = "Offence Aggravated by Terrorist Connection", displayOrder = 20),
            ),
            sentence = Sentence(
              sentenceUuid = response.charges.first().sentence?.sentenceUuid!!,
              chargeNumber = null,
              periodLengths = listOf(
                PeriodLength(
                  years = 1,
                  months = null,
                  weeks = null,
                  days = 60,
                  periodOrder = "years,months,weeks,days",
                  periodLengthType = PeriodLengthType.SENTENCE_LENGTH,
                  legacyData = null,
                  periodLengthUuid = response.charges.first().sentence?.periodLengths?.first()?.periodLengthUuid!!,
                ),
              ),
              sentenceServeType = "FORTHWITH",
              sentenceType = SentenceType(
                sentenceTypeUuid = UUID.fromString("02fe3513-40a6-47e9-a72d-9dafdd936a0e"),
                description = "SDS (Standard Determinate Sentence)",
                classification = SentenceTypeClassification.STANDARD,
                hintText = null,
                displayOrder = 10,
              ),
              convictionDate = LocalDate.of(2026, 10, 1),
              fineAmount = null,
              legacyData = null,
              consecutiveToSentenceUuid = null,
              hasRecall = false,
              status = SentenceEntityStatus.ACTIVE,
            ),
            legacyData = null,
            mergedFromCase = null,
            createdAt = response.charges.first().createdAt,
            findingOfDomesticAbuse = true,
          ),
        ),
        overallConvictionDate = LocalDate.of(2026, 10, 1),
        legacyData = null,
        documents = listOf(
          UploadedDocument(
            documentUUID = DOCUMENT_ID,
            documentType = "HMCTS_WARRANT",
            fileName = "warrant.pdf",
          ),
        ),
        source = EventSource.DPS,
        deleteStatus = DeleteCourtAppearanceStatus.SUPPORTED,
        periodLengths = listOf(
          PeriodLength(
            years = 1,
            months = null,
            weeks = null,
            days = 60,
            periodOrder = "years,months,weeks,days",
            periodLengthType = PeriodLengthType.OVERALL_SENTENCE_LENGTH,
            legacyData = null,
            periodLengthUuid = response.periodLengths.first().periodLengthUuid,
          ),
        ),
      ),
    )
  }

  @Test
  fun `Test get remand appearance from with offence outcomes all the same hmcts data`() {
    val prisonerNumber = "PRIS123"
    val hmctsCourtHearing = HMCTS_REMAND_HEARING.copy(
      charges = listOf(HMCTS_REMANDED_CHARGE.copy(), HMCTS_REMANDED_CHARGE.copy()),
    )
    CourtDataIngestionApiExtension.courtDataIngestionApi.stubCourtHearing(
      hmctsCourtHearing,
      prisonerNumber,
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

    Assertions.assertThat(response.outcome).isEqualTo(
      CourtAppearanceOutcome(
        outcomeUuid = UUID.fromString("2f585681-7b1a-44fb-a0cb-f9a4b1d9cda8"),
        outcomeName = "Remand in custody",
        nomisCode = "4531",
        outcomeType = "REMAND",
        displayOrder = 70,
        relatedChargeOutcomeUuid = UUID.fromString("315280e5-d53e-43b3-8ba6-44da25676ce2"),
        isSubList = false, dispositionCode = "INTERIM", status = ReferenceEntityStatus.ACTIVE, warrantType = "NON_SENTENCING",
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
          DOCUMENT_ID,
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
          convictionDate = null,
          results = listOf(
            HmctsCourtResult(
              code = "XXX",
              description = "Unknown outcome",
              keyValuePairs = emptyList(),
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
            documentUUID = DOCUMENT_ID,
            documentType = "HMCTS_WARRANT",
            fileName = "warrant.pdf",
          ),
        ),
        source = EventSource.DPS,
        deleteStatus = DeleteCourtAppearanceStatus.SUPPORTED,
        periodLengths = emptyList(),
      ),
    )
  }

  @Test
  fun `Test get remand appearance with multiple next hearing results from hmcts data`() {
    val prisonerNumber = "PRIS123"
    val hmctsCourtHearing = HMCTS_REMAND_HEARING.copy(
      charges = listOf(
        HMCTS_REMANDED_CHARGE.copy(
          results = listOf(
            HmctsCourtResult(
              code = "RI",
              description = "Remand in custody",
              keyValuePairs = emptyList(),
            ),
            HmctsCourtResult(
              code = "NHCSSS",
              description = "Next court appearance",
              keyValuePairs = listOf(
                ResultKeyValue(
                  "Date of hearing",
                  "04/12/2026",
                ),
              ),
            ),
          ),
        ),
      ),
    )
    CourtDataIngestionApiExtension.courtDataIngestionApi.stubCourtHearing(
      hmctsCourtHearing,
      prisonerNumber,
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

    Assertions.assertThat(response.nextCourtAppearance).isEqualTo(blankNextCourtAppearance())
  }

  @Test
  fun `Test get remand appearance with NHCSSS different to next court appearance`() {
    val prisonerNumber = "PRIS123"
    val hmctsCourtHearing = HMCTS_REMAND_HEARING.copy(
      charges = listOf(
        HMCTS_REMANDED_CHARGE.copy(
          results = listOf(
            HmctsCourtResult(
              code = "RI",
              description = "Remand in custody",
              keyValuePairs = emptyList(),
            ),
            HmctsCourtResult(
              code = "NHCSSS",
              description = "Next court appearance",
              keyValuePairs = listOf(
                ResultKeyValue(
                  "Date of hearing",
                  "04/12/2026",
                ),
              ),
            ),
          ),
        ),
      ),
    )
    CourtDataIngestionApiExtension.courtDataIngestionApi.stubCourtHearing(
      hmctsCourtHearing,
      prisonerNumber,
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

    Assertions.assertThat(response.nextCourtAppearance).isEqualTo(blankNextCourtAppearance())
  }

  @Test
  fun `Test get remand appearance with NHCSSS same as to next court appearance`() {
    val prisonerNumber = "PRIS123"
    val hmctsCourtHearing = HMCTS_REMAND_HEARING.copy(
      charges = listOf(
        HMCTS_REMANDED_CHARGE.copy(
          results = listOf(
            HmctsCourtResult(
              code = "RI",
              description = "Remand in custody",
              keyValuePairs = emptyList(),
            ),
            HmctsCourtResult(
              code = "NHCSSS",
              description = "Next court appearance",
              keyValuePairs = listOf(
                ResultKeyValue(
                  "Date of hearing",
                  HMCTS_REMAND_HEARING.nextHearing?.hearingDate?.toLocalDate()!!.format(HmctsCourtDataService.PCR_DATE_FORMAT),
                ),
              ),
            ),
          ),
        ),
      ),
    )
    CourtDataIngestionApiExtension.courtDataIngestionApi.stubCourtHearing(
      hmctsCourtHearing,
      prisonerNumber,
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

    Assertions.assertThat(response.nextCourtAppearance).isNotNull
  }

  @Test
  fun `Test get remand appearance with next appearance result different to next court appearance`() {
    val prisonerNumber = "PRIS123"
    val hmctsCourtHearing = HMCTS_REMAND_HEARING.copy(
      charges = listOf(
        HMCTS_REMANDED_CHARGE.copy(
          results = listOf(
            HmctsCourtResult(
              code = "RI",
              description = "Remand in custody",
              keyValuePairs = listOf(
                ResultKeyValue(
                  "Next hearing in magistrates' court",
                  "Date of hearing:12/10/2026\\nTime of hearing:11:00\\nCourthouse organisation name:Leeds Magistrates' Court\\nCourthouse address line 1:PO Box 97\\nCourthouse address line 2:Westgate\\nCourthouse address line 3:Leeds\\nCourthouse address line 4:W. Yorks.\\nCourthouse post code:LS1 3JP\\nCourtroom:Courtroom 04\\nHearing type:Bail Application\\nEstimated duration:20 MINUTES\\nBooking reference:9e9941ea-1085-427f-a09d-5f8ecbf41078",
                ),
              ),
            ),
          ),
        ),
      ),
    )
    CourtDataIngestionApiExtension.courtDataIngestionApi.stubCourtHearing(
      hmctsCourtHearing,
      prisonerNumber,
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

    Assertions.assertThat(response.nextCourtAppearance).isEqualTo(blankNextCourtAppearance())
  }

  @Test
  fun `Test remand in custody with a conviction date results in null outcome`() {
    val prisonerNumber = "PRIS123"
    val hmctsCourtHearing = HMCTS_REMAND_HEARING.copy(
      charges = listOf(
        HMCTS_REMANDED_CHARGE.copy(convictionDate = HMCTS_REMAND_HEARING.hearingDate),
        HMCTS_REMANDED_CHARGE.copy(),
      ),
    )
    CourtDataIngestionApiExtension.courtDataIngestionApi.stubCourtHearing(
      hmctsCourtHearing,
      prisonerNumber,
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

    Assertions.assertThat(response.charges.all { it.outcome == null }).isTrue
    Assertions.assertThat(response.outcome).isNull()
  }

  @ParameterizedTest(name = "Tests that offence results are mapped from hmcts {0} to ras outcome {1}")
  @MethodSource("offenceResultsProvider")
  fun `Test get offence outcome mappings`(hmctsCode: String, expectedRasOutcomeText: String) {
    val prisonerNumber = "PRIS123"
    val hmctsCourtHearing = HmctsCourtHearing(
      hearingId = HMCTS_HEARING_ID,
      courtName = "My court",
      courtCode = COURT_REGISTER_ID,
      courtId = UUID.randomUUID(),
      hearingDate = LocalDate.of(2026, 1, 1),
      caseReferences = listOf("ABC123", "EFG456"),
      hearingType = "First hearing",
      documents = listOf(
        HmctsCourHearingDocument(
          "REMAND_WARRANT",
          DOCUMENT_ID,
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
          convictionDate = null,
          results = listOf(
            HmctsCourtResult(
              code = hmctsCode,
              description = hmctsCode,
              keyValuePairs = emptyList(),
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
    val DOCUMENT_ID = UUID.randomUUID()
    val COURT_REGISTER_ID = UUID.randomUUID().toString()

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

    val HMCTS_SENTENCE_HEARING = HmctsCourtHearing(
      hearingId = HMCTS_HEARING_ID,
      courtName = "My court",
      courtCode = COURT_REGISTER_ID,
      courtId = UUID.randomUUID(),
      hearingDate = LocalDate.of(2026, 10, 1),
      caseReferences = listOf("ABC123", "EFG456"),
      hearingType = "Sentence hearing",
      documents = listOf(
        HmctsCourHearingDocument(
          "SENTENCING_WARRANT",
          DOCUMENT_ID,
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
          convictionDate = LocalDate.of(2026, 10, 1),
          results = listOf(
            HmctsCourtResult(
              code = "GPTAC",
              description = "Guilty plea taken into account Defendant's guilty plea taken into account when imposing sentence.",
              keyValuePairs = emptyList(),
            ),
            HmctsCourtResult(
              code = "OAFPC",
              description = "Aggravating factor one.",
              keyValuePairs = emptyList(),
            ),
            HmctsCourtResult(
              code = "OATC",
              description = "Aggravating factor two.",
              keyValuePairs = emptyList(),
            ),
            HmctsCourtResult(
              code = "FODA",
              description = "Domestic abuse factor.",
              keyValuePairs = emptyList(),
            ),
            HmctsCourtResult(
              code = "IMP",
              description = "Imprisonment",
              keyValuePairs = listOf(
                ResultKeyValue(
                  key = "Imprisonment Period",
                  value = "1 Years 60 Days",
                ),
                ResultKeyValue(
                  key = "Total custodial period",
                  value = "1 Years 60 Days",
                ),
              ),
            ),
          ),
        ),
      ),
      nextHearing = null,
    )

    val HMCTS_REMANDED_CHARGE = HmctsCourtCharge(
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
      convictionDate = null,
      results = listOf(
        HmctsCourtResult(
          code = "RI",
          description = "Remand in custody",
          keyValuePairs = emptyList(),
        ),
      ),
    )
    val HMCTS_WITHDRAWN_CHARGE = HmctsCourtCharge(
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
      convictionDate = null,
      results = listOf(
        HmctsCourtResult(
          code = "WDRN",
          description = "Withdrawn",
          keyValuePairs = emptyList(),
        ),
      ),
    )
    val HCMTS_TWO_MAPPING_CHARGE = HmctsCourtCharge(
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
      convictionDate = null,
      results = listOf(
        HmctsCourtResult(
          code = "WDRN",
          description = "Withdrawn",
          keyValuePairs = emptyList(),
        ),
        HmctsCourtResult(
          code = "RI",
          description = "Remand in custody",
          keyValuePairs = emptyList(),
        ),
      ),
    )
    val HMCTS_REMAND_HEARING = HmctsCourtHearing(
      hearingId = HMCTS_HEARING_ID,
      courtName = "My court",
      courtCode = COURT_REGISTER_ID,
      courtId = UUID.randomUUID(),
      hearingDate = LocalDate.of(2026, 1, 1),
      caseReferences = listOf("ABC123", "EFG456"),
      hearingType = "First hearing",
      documents = listOf(
        HmctsCourHearingDocument(
          "REMAND_WARRANT",
          DOCUMENT_ID,
        ),
      ),
      charges = listOf(
        HMCTS_REMANDED_CHARGE,
        HMCTS_WITHDRAWN_CHARGE,
        HCMTS_TWO_MAPPING_CHARGE,
      ),
      nextHearing = HmctsNextCourtHearing(
        courtName = "Central London County Court",
        hmctsCourtId = UUID.randomUUID(),
        hmppsCourtId = UUID.randomUUID().toString(),
        hearingDate = LocalDateTime.of(2026, 8, 15, 10, 0),
        hearingId = UUID.randomUUID(),
      ),
    )
  }
}
