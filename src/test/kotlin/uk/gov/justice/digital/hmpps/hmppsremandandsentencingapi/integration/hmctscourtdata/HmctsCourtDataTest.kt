package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.hmctscourtdata

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.description
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.dto.DocumentManagementApiDocument
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.dto.HmctsCourHearingDocument
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.dto.HmctsCourtCharge
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.dto.HmctsCourtHearing
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.dto.HmctsCourtResult
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.dto.HmctsNextCourtHearing
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.dto.PrisonSearchDetails
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
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.wiremock.PrisonSearchExtension
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.PeriodLengthType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ReferenceEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.SentenceEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.SentenceTypeClassification
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.HmctsCourtDataService
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.HmctsCourtDataService.Companion.blankNextCourtAppearance
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.Constants
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.DpsDataCreator.Factory.DEFAULT_PRISONER_ID
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
    PrisonSearchExtension.prisonSearch.stubPrisonSearchDetails(
      DEFAULT_PRISONER_ID,
      PrisonSearchDetails(
        "KMI",
        "Kirkham (HMP)",
        ADULT_DOB,
      ),
    )
  }

  @Test
  fun `Test get remand appearance from hmcts data`() {
    val hmctsCourtHearing = HMCTS_REMAND_HEARING
    CourtDataIngestionApiExtension.courtDataIngestionApi.stubCourtHearing(
      hmctsCourtHearing,
      DEFAULT_PRISONER_ID,
    )

    val response = webTestClient
      .get()
      .uri("/hmcts-court-data/${HMCTS_HEARING_ID}/prisoner/$DEFAULT_PRISONER_ID/appearance")
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
          outcomeUuid = UUID.fromString("2f585681-7b1a-44fb-a0cb-f9a4b1d9cda8"),
          outcomeName = "Remand in custody",
          nomisCode = "4531",
          outcomeType = "REMAND",
          displayOrder = 70,
          relatedChargeOutcomeUuid = UUID.fromString("315280e5-d53e-43b3-8ba6-44da25676ce2"),
          isSubList = false,
          dispositionCode = "INTERIM",
          status = ReferenceEntityStatus.ACTIVE,
          warrantType = "NON_SENTENCING",
        ),
        courtCode = COURT_REGISTER_ID,
        courtCaseReference = "ABC123",
        criminalAppealOfficeReference = null,
        appearanceDate = LocalDate.parse("2026-01-01"),
        warrantType = "NON_SENTENCING",
        nextCourtAppearance = NextCourtAppearance(
          appearanceDate = LocalDate.of(2026, 8, 15),
          appearanceTime = LocalTime.of(10, 0),
          courtCode = hmctsCourtHearing.nextHearing?.hmppsCourtId!!,
          appearanceType = AppearanceType(
            appearanceTypeUuid = Constants.nilUUID,
            description = "Unknown appearance type",
            displayOrder = 1,
            hasSubtypes = false,
          ),
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
              status = ReferenceEntityStatus.ACTIVE,
            ),
            aggravatingFactors = emptyList(),
            sentence = null,
            legacyData = null,
            mergedFromCase = null,
            createdAt = response.charges.first().createdAt,
            findingOfDomesticAbuse = null,
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
  fun `Test get appearance from hmcts standard sentence data`() {
    val hmctsCourtHearing = HMCTS_SENTENCE_HEARING
    CourtDataIngestionApiExtension.courtDataIngestionApi.stubCourtHearing(
      hmctsCourtHearing,
      DEFAULT_PRISONER_ID,
    )

    val response = webTestClient
      .get()
      .uri("/hmcts-court-data/${HMCTS_HEARING_ID}/prisoner/$DEFAULT_PRISONER_ID/appearance")
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
          isSubList = false,
          dispositionCode = "FINAL",
          status = ReferenceEntityStatus.ACTIVE,
          warrantType = "SENTENCING",
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
              status = ReferenceEntityStatus.ACTIVE,
            ),
            aggravatingFactors = listOf(
              AggravatingFactor(
                code = "OAFPC",
                title = "Offence aggravated by foreign power condition being met",
                description = "Offence aggravated by foreign power condition being met",
                displayOrder = 10,
              ),
              AggravatingFactor(
                code = "OATC",
                title = "Offence Aggravated by Terrorist Connection",
                description = "Offence Aggravated by Terrorist Connection",
                displayOrder = 20,
              ),
            ),
            sentence = Sentence(
              sentenceUuid = response.charges.first().sentence!!.sentenceUuid,
              chargeNumber = "-1",
              periodLengths = listOf(
                PeriodLength(
                  years = 1,
                  months = null,
                  weeks = null,
                  days = 60,
                  periodOrder = "years,months,weeks,days",
                  periodLengthType = PeriodLengthType.SENTENCE_LENGTH,
                  legacyData = null,
                  periodLengthUuid = response.charges.first().sentence!!.periodLengths.first().periodLengthUuid,
                ),
              ),
              sentenceServeType = "FORTHWITH",
              sentenceType = SentenceType(
                sentenceTypeUuid = UUID.fromString("e138374d-810f-4718-a81a-1c9d4745031e"),
                description = "ORA SDS (Offender rehabilitation act standard determinate sentence)",
                classification = SentenceTypeClassification.STANDARD,
                hintText = null,
                displayOrder = 40,
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
  fun `Test get appearance from hmcts standard sentence data with other non sentencing mapped results`() {
    val hmctsCourtHearing = HMCTS_SENTENCE_HEARING.copy(
      charges = listOf(
        HMCTS_SENTENCED_CHARGE.copy(
          results = HMCTS_SENTENCED_CHARGE.results + HmctsCourtResult(
            code = "COEW",
            description = "Community order",
            keyValuePairs = emptyList(),
          ),
        ),
      ),
    )
    CourtDataIngestionApiExtension.courtDataIngestionApi.stubCourtHearing(
      hmctsCourtHearing,
      DEFAULT_PRISONER_ID,
    )

    val response = webTestClient
      .get()
      .uri("/hmcts-court-data/${HMCTS_HEARING_ID}/prisoner/$DEFAULT_PRISONER_ID/appearance")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus().isOk
      .returnResult(CourtAppearance::class.java)
      .responseBody.blockFirst()!!

    Assertions.assertThat(response.charges.first().outcome).isEqualTo(
      ChargeOutcome(
        outcomeUuid = UUID.fromString("f17328cf-ceaa-43c2-930a-26cf74480e18"),
        outcomeName = "Imprisonment",
        nomisCode = "1002",
        outcomeType = "SENTENCING",
        displayOrder = 10,
        dispositionCode = "FINAL",
        status = ReferenceEntityStatus.ACTIVE,
      ),
    )
    Assertions.assertThat(response.charges.first().sentence).isNotNull
  }

  @Test
  fun `Test get appearance from hmcts DTO sentence data`() {
    val hmctsCourtHearing = HMCTS_SENTENCE_HEARING.copy(
      charges = listOf(
        HMCTS_SENTENCED_CHARGE.copy(
          results = listOf(
            HMCTS_SENTENCED_RESULT.copy(
              code = "DTO",
              keyValuePairs = listOf(
                ResultKeyValue(
                  key = "Period of detention and training",
                  value = "2 Years 10 Weeks",
                ),
              ),
            ),
          ),
          startDate = LocalDate.of(2016, 8, 15),
          endDate = null,
          convictionDate = CONVICTION_2020_ACT_DATE,
        ),
      ),
    )
    PrisonSearchExtension.prisonSearch.stubPrisonSearchDetails(
      DEFAULT_PRISONER_ID,
      PrisonSearchDetails(
        "KMI",
        "Kirkham (HMP)",
        CONVICTION_2020_ACT_DATE.minusYears(17),
      ),
    )
    CourtDataIngestionApiExtension.courtDataIngestionApi.stubCourtHearing(
      hmctsCourtHearing,
      DEFAULT_PRISONER_ID,
    )

    val response = webTestClient
      .get()
      .uri("/hmcts-court-data/${HMCTS_HEARING_ID}/prisoner/$DEFAULT_PRISONER_ID/appearance")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus().isOk
      .returnResult(CourtAppearance::class.java)
      .responseBody.blockFirst()!!

    Assertions.assertThat(response.charges.first().sentence).isEqualTo(
      Sentence(
        sentenceUuid = response.charges.first().sentence!!.sentenceUuid,
        chargeNumber = "-1",
        periodLengths = listOf(
          PeriodLength(
            years = 2,
            months = null,
            weeks = 10,
            days = null,
            periodOrder = "years,months,weeks,days",
            periodLengthType = PeriodLengthType.TERM_LENGTH,
            legacyData = null,
            periodLengthUuid = response.charges.first().sentence!!.periodLengths.first().periodLengthUuid,
          ),
        ),
        sentenceServeType = "FORTHWITH",
        sentenceType = SentenceType(
          sentenceTypeUuid = UUID.fromString("903ca33b-e264-4a16-883d-fee03a2a3396"),
          description = "ORA Detention and Training Order",
          classification = SentenceTypeClassification.DTO,
          hintText = null,
          displayOrder = 230,
        ),
        convictionDate = CONVICTION_2020_ACT_DATE,
        fineAmount = null,
        legacyData = null,
        consecutiveToSentenceUuid = null,
        hasRecall = false,
        status = SentenceEntityStatus.ACTIVE,
      ),
    )
  }

  @Test
  fun `Test get appearance from hmcts EDS sentence data`() {
    val hmctsCourtHearing = HMCTS_SENTENCE_HEARING.copy(
      charges = listOf(
        HMCTS_SENTENCED_CHARGE.copy(
          results = listOf(
            HMCTS_SENTENCED_RESULT.copy(
              code = "EXTDVSU",
              keyValuePairs = listOf(
                ResultKeyValue(
                  key = "Appropriate custodial term",
                  value = "5 Years",
                ),
                ResultKeyValue(
                  key = "Extension period",
                  value = "30 Months",
                ),
              ),
            ),
          ),
          convictionDate = CONVICTION_2020_ACT_DATE,
        ),
      ),
    )
    PrisonSearchExtension.prisonSearch.stubPrisonSearchDetails(
      DEFAULT_PRISONER_ID,
      PrisonSearchDetails(
        "KMI",
        "Kirkham (HMP)",
        CONVICTION_2020_ACT_DATE.minusYears(17),
      ),
    )
    CourtDataIngestionApiExtension.courtDataIngestionApi.stubCourtHearing(
      hmctsCourtHearing,
      DEFAULT_PRISONER_ID,
    )

    val response = webTestClient
      .get()
      .uri("/hmcts-court-data/${HMCTS_HEARING_ID}/prisoner/$DEFAULT_PRISONER_ID/appearance")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus().isOk
      .returnResult(CourtAppearance::class.java)
      .responseBody.blockFirst()!!

    Assertions.assertThat(response.charges.first().sentence).isEqualTo(
      Sentence(
        sentenceUuid = response.charges.first().sentence!!.sentenceUuid,
        chargeNumber = "-1",
        periodLengths = listOf(
          PeriodLength(
            years = 5,
            months = null,
            weeks = null,
            days = null,
            periodOrder = "years,months,weeks,days",
            periodLengthType = PeriodLengthType.CUSTODIAL_TERM,
            legacyData = null,
            periodLengthUuid = response.charges.first().sentence!!.periodLengths.first().periodLengthUuid,
          ),
          PeriodLength(
            years = null,
            months = 30,
            weeks = null,
            days = null,
            periodOrder = "years,months,weeks,days",
            periodLengthType = PeriodLengthType.LICENCE_PERIOD,
            legacyData = null,
            periodLengthUuid = response.charges.first().sentence!!.periodLengths.last().periodLengthUuid,
          ),
        ),
        sentenceServeType = "FORTHWITH",
        sentenceType = SentenceType(
          sentenceTypeUuid = UUID.fromString("f1ddc9d6-3c2e-419a-b54d-4e05c92a0f55"),
          description = "EDS (Extended Determinate Sentence)",
          classification = SentenceTypeClassification.EXTENDED,
          hintText = null,
          displayOrder = 20,
        ),
        convictionDate = CONVICTION_2020_ACT_DATE,
        fineAmount = null,
        legacyData = null,
        consecutiveToSentenceUuid = null,
        hasRecall = false,
        status = SentenceEntityStatus.ACTIVE,
      ),
    )
  }

  @Test
  fun `Test get appearance from hmcts MLI sentence data`() {
    val hmctsCourtHearing = HMCTS_SENTENCE_HEARING.copy(
      charges = listOf(
        HMCTS_SENTENCED_CHARGE.copy(
          results = listOf(
            HMCTS_SENTENCED_RESULT.copy(
              code = "MLI",
              keyValuePairs = listOf(
                ResultKeyValue(
                  key = "Minimum term",
                  value = "30 Years",
                ),
              ),
            ),
          ),
          convictionDate = CONVICTION_2020_ACT_DATE,
        ),
      ),
    )
    CourtDataIngestionApiExtension.courtDataIngestionApi.stubCourtHearing(
      hmctsCourtHearing,
      DEFAULT_PRISONER_ID,
    )

    val response = webTestClient
      .get()
      .uri("/hmcts-court-data/${HMCTS_HEARING_ID}/prisoner/$DEFAULT_PRISONER_ID/appearance")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus().isOk
      .returnResult(CourtAppearance::class.java)
      .responseBody.blockFirst()!!

    Assertions.assertThat(response.charges.first().sentence).isEqualTo(
      Sentence(
        sentenceUuid = response.charges.first().sentence!!.sentenceUuid,
        chargeNumber = "-1",
        periodLengths = listOf(
          PeriodLength(
            years = 30,
            months = null,
            weeks = null,
            days = null,
            periodOrder = "years,months,weeks,days",
            periodLengthType = PeriodLengthType.TARIFF_LENGTH,
            legacyData = null,
            periodLengthUuid = response.charges.first().sentence!!.periodLengths.first().periodLengthUuid,
          ),
        ),
        sentenceServeType = "FORTHWITH",
        sentenceType = null,
        convictionDate = CONVICTION_2020_ACT_DATE,
        fineAmount = null,
        legacyData = null,
        consecutiveToSentenceUuid = null,
        hasRecall = false,
        status = SentenceEntityStatus.ACTIVE,
      ),
    )
  }

  @ParameterizedTest(name = "Tests that sentences are mapped: {0}")
  @MethodSource("sentenceTestProvider")
  fun `Test sentence type mapping`(testName: String, charge: HmctsCourtCharge, sentenceType: UUID?, dob: LocalDate) {
    val hmctsCourtHearing = HMCTS_SENTENCE_HEARING.copy(
      charges = listOf(charge),
    )
    CourtDataIngestionApiExtension.courtDataIngestionApi.stubCourtHearing(
      hmctsCourtHearing,
      DEFAULT_PRISONER_ID,
    )
    PrisonSearchExtension.prisonSearch.stubPrisonSearchDetails(
      DEFAULT_PRISONER_ID,
      PrisonSearchDetails(
        "KMI",
        "Kirkham (HMP)",
        dob,
      ),
    )

    val response = webTestClient
      .get()
      .uri("/hmcts-court-data/${HMCTS_HEARING_ID}/prisoner/$DEFAULT_PRISONER_ID/appearance")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus().isOk
      .returnResult(CourtAppearance::class.java)
      .responseBody.blockFirst()!!

    Assertions.assertThat(response.charges.first().sentence?.sentenceType?.sentenceTypeUuid).isEqualTo(sentenceType)
  }

  @Test
  fun `Test get remand appearance from with offence outcomes different outcome types`() {
    val hmctsCourtHearing = HMCTS_REMAND_HEARING.copy(
      charges = listOf(
        HMCTS_REMANDED_CHARGE.copy(),
        HMCTS_REMANDED_CHARGE.copy(
          results = listOf(
            HmctsCourtResult(
              code = "WDRN",
              description = "Withdrwan",
              keyValuePairs = emptyList(),
            ),
          ),
        ),
      ),
    )
    CourtDataIngestionApiExtension.courtDataIngestionApi.stubCourtHearing(
      hmctsCourtHearing,
      DEFAULT_PRISONER_ID,
    )

    val response = webTestClient
      .get()
      .uri("/hmcts-court-data/${HMCTS_HEARING_ID}/prisoner/$DEFAULT_PRISONER_ID/appearance")
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
        isSubList = false,
        dispositionCode = "INTERIM",
        status = ReferenceEntityStatus.ACTIVE,
        warrantType = "NON_SENTENCING",
      ),
    )
  }

  @Test
  fun `Test get remand appearance from with offence outcomes different outcomes same types`() {
    val hmctsCourtHearing = HMCTS_REMAND_HEARING.copy(
      charges = listOf(
        HMCTS_REMANDED_CHARGE.copy(),
        HMCTS_REMANDED_CHARGE.copy(
          results = listOf(
            HmctsCourtResult(
              code = "CCII",
              description = "Crown court remand",
              keyValuePairs = emptyList(),
            ),
          ),
        ),
      ),
    )
    CourtDataIngestionApiExtension.courtDataIngestionApi.stubCourtHearing(
      hmctsCourtHearing,
      DEFAULT_PRISONER_ID,
    )

    val response = webTestClient
      .get()
      .uri("/hmcts-court-data/${HMCTS_HEARING_ID}/prisoner/$DEFAULT_PRISONER_ID/appearance")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus().isOk
      .returnResult(CourtAppearance::class.java)
      .responseBody.blockFirst()!!

    Assertions.assertThat(response.outcome).isNull()
  }

  @Test
  fun `Test get minimal appearance from hmcts data`() {
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
      DEFAULT_PRISONER_ID,
    )

    val response = webTestClient
      .get()
      .uri("/hmcts-court-data/${HMCTS_HEARING_ID}/prisoner/$DEFAULT_PRISONER_ID/appearance")
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
            aggravatingFactors = emptyList(),
            sentence = null,
            legacyData = null,
            mergedFromCase = null,
            createdAt = response.charges.first().createdAt,
            findingOfDomesticAbuse = null,
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
      DEFAULT_PRISONER_ID,
    )

    val response = webTestClient
      .get()
      .uri("/hmcts-court-data/${HMCTS_HEARING_ID}/prisoner/$DEFAULT_PRISONER_ID/appearance")
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
      DEFAULT_PRISONER_ID,
    )

    val response = webTestClient
      .get()
      .uri("/hmcts-court-data/${HMCTS_HEARING_ID}/prisoner/$DEFAULT_PRISONER_ID/appearance")
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
                  HMCTS_REMAND_HEARING.nextHearing?.hearingDate?.toLocalDate()!!
                    .format(HmctsCourtDataService.PCR_DATE_FORMAT),
                ),
              ),
            ),
          ),
        ),
      ),
    )
    CourtDataIngestionApiExtension.courtDataIngestionApi.stubCourtHearing(
      hmctsCourtHearing,
      DEFAULT_PRISONER_ID,
    )

    val response = webTestClient
      .get()
      .uri("/hmcts-court-data/${HMCTS_HEARING_ID}/prisoner/$DEFAULT_PRISONER_ID/appearance")
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
      DEFAULT_PRISONER_ID,
    )

    val response = webTestClient
      .get()
      .uri("/hmcts-court-data/${HMCTS_HEARING_ID}/prisoner/$DEFAULT_PRISONER_ID/appearance")
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
    val hmctsCourtHearing = HMCTS_REMAND_HEARING.copy(
      charges = listOf(
        HMCTS_REMANDED_CHARGE.copy(convictionDate = HMCTS_REMAND_HEARING.hearingDate),
        HMCTS_REMANDED_CHARGE.copy(),
      ),
    )
    CourtDataIngestionApiExtension.courtDataIngestionApi.stubCourtHearing(
      hmctsCourtHearing,
      DEFAULT_PRISONER_ID,
    )

    val response = webTestClient
      .get()
      .uri("/hmcts-court-data/${HMCTS_HEARING_ID}/prisoner/$DEFAULT_PRISONER_ID/appearance")
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
    val hmctsCourtHearing = HMCTS_REMAND_HEARING.copy(
      charges = listOf(
        HMCTS_REMANDED_CHARGE.copy(
          results = listOf(
            HmctsCourtResult(
              code = hmctsCode,
              description = "Code description",
              keyValuePairs = emptyList(),
            ),
          ),
        ),
      ),
    )
    CourtDataIngestionApiExtension.courtDataIngestionApi.stubCourtHearing(
      hmctsCourtHearing,
      DEFAULT_PRISONER_ID,
    )

    val response = webTestClient
      .get()
      .uri("/hmcts-court-data/${HMCTS_HEARING_ID}/prisoner/$DEFAULT_PRISONER_ID/appearance")
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

    @JvmStatic
    fun sentenceTestProvider(): Stream<Arguments> = Stream.of(
      Arguments.of(
        "2020 ORA Standard Determinate Sentences",
        HMCTS_SENTENCED_CHARGE.copy(
          results = listOf(HMCTS_SENTENCED_RESULT),
          startDate = LocalDate.of(2021, 8, 15),
          endDate = null,
          convictionDate = CONVICTION_2020_ACT_DATE,
        ),
        UUID.fromString("e138374d-810f-4718-a81a-1c9d4745031e"),
        ADULT_DOB,
      ),
      Arguments.of(
        "2003 ORA Standard Determinate Sentences",
        HMCTS_SENTENCED_CHARGE.copy(
          results = listOf(HMCTS_SENTENCED_RESULT),
          startDate = LocalDate.of(2021, 8, 15),
          endDate = null,
          convictionDate = CONVICTION_2003_ACT_DATE,
        ),
        UUID.fromString("6d0948c1-243b-4a2d-8b23-0721a5a1a949"),
        ADULT_DOB,
      ),
      Arguments.of(
        "2020 Standard Determinate Sentences (Non-ORA)",
        HMCTS_SENTENCED_CHARGE.copy(
          results = listOf(HMCTS_SENTENCED_RESULT),
          startDate = LocalDate.of(2014, 8, 15),
          endDate = null,
          convictionDate = CONVICTION_2020_ACT_DATE,
        ),
        UUID.fromString("02fe3513-40a6-47e9-a72d-9dafdd936a0e"),
        ADULT_DOB,
      ),
      Arguments.of(
        "2003 Standard Determinate Sentences (Non-ORA)",
        HMCTS_SENTENCED_CHARGE.copy(
          results = listOf(HMCTS_SENTENCED_RESULT),
          startDate = LocalDate.of(2014, 8, 15),
          endDate = null,
          convictionDate = CONVICTION_2003_ACT_DATE,
        ),
        UUID.fromString("8d04557c-8e54-4e2a-844f-272163fca833"),
        ADULT_DOB,
      ),
      Arguments.of(
        "2020 ORA Young Offender Instituition",
        HMCTS_SENTENCED_CHARGE.copy(
          results = listOf(HMCTS_SENTENCED_RESULT.copy(code = "YOI")),
          startDate = LocalDate.of(2021, 8, 15),
          endDate = null,
          convictionDate = CONVICTION_2020_ACT_DATE,
        ),
        UUID.fromString("d64283fd-7e48-4ed8-a98c-d68e938a5661"),
        CONVICTION_2020_ACT_DATE.minusYears(19),
      ),
      Arguments.of(
        "2003 ORA Young Offender Instituition",
        HMCTS_SENTENCED_CHARGE.copy(
          results = listOf(HMCTS_SENTENCED_RESULT.copy(code = "YOI")),
          startDate = LocalDate.of(2021, 8, 15),
          endDate = null,
          convictionDate = CONVICTION_2003_ACT_DATE,
        ),
        null, // UUID.fromString("babc059d-53aa-423c-9129-be2ba86026ad"), more than one matching sentence type currently.
        CONVICTION_2003_ACT_DATE.minusYears(17),
      ),
      Arguments.of(
        "2020 Young Offender Instituition (Non-ORA)",
        HMCTS_SENTENCED_CHARGE.copy(
          results = listOf(HMCTS_SENTENCED_RESULT.copy(code = "YOI")),
          startDate = LocalDate.of(2014, 8, 15),
          endDate = null,
          convictionDate = CONVICTION_2020_ACT_DATE,
        ),
        UUID.fromString("3bfbf00d-747e-4576-8d96-aa4f8f9832ef"),
        CONVICTION_2020_ACT_DATE.minusYears(19),
      ),
      Arguments.of(
        "2003 Young Offender Instituition (Non-ORA)",
        HMCTS_SENTENCED_CHARGE.copy(
          results = listOf(HMCTS_SENTENCED_RESULT.copy(code = "YOI")),
          startDate = LocalDate.of(2014, 8, 15),
          endDate = null,
          convictionDate = CONVICTION_2003_ACT_DATE,
        ),
        null, // UUID.fromString("87ef5d34-5912-411a-8733-2b8cf0cfc8e5"), more than one matching sentence type currently.
        CONVICTION_2003_ACT_DATE.minusYears(17),
      ),
      Arguments.of(
        "2020 ORA Detention and Training Order",
        HMCTS_SENTENCED_CHARGE.copy(
          results = listOf(HMCTS_SENTENCED_RESULT.copy(code = "DTO")),
          startDate = LocalDate.of(2016, 8, 15),
          endDate = null,
          convictionDate = CONVICTION_2020_ACT_DATE,
        ),
        UUID.fromString("903ca33b-e264-4a16-883d-fee03a2a3396"),
        LocalDate.of(2023, 8, 15).minusYears(17),
      ),
      Arguments.of(
        "2020 Detention and Training Order (Non-ORA)",
        HMCTS_SENTENCED_CHARGE.copy(
          results = listOf(HMCTS_SENTENCED_RESULT.copy(code = "DTO")),
          startDate = LocalDate.of(2014, 8, 15),
          endDate = null,
          convictionDate = CONVICTION_2020_ACT_DATE,
        ),
        UUID.fromString("cab9e914-e0de-48d0-9e72-0e1fc9a19cf4"),
        LocalDate.of(2023, 8, 15).minusYears(17),
      ),
      Arguments.of(
        "2003 Detention and Training Order(Non-ORA)",
        HMCTS_SENTENCED_CHARGE.copy(
          results = listOf(HMCTS_SENTENCED_RESULT.copy(code = "DTO")),
          startDate = LocalDate.of(2014, 8, 15),
          endDate = null,
          convictionDate = CONVICTION_2003_ACT_DATE,
        ),
        UUID.fromString("e8098305-f909-4cf5-9b06-f71a4e4d9df0"),
        CONVICTION_2003_ACT_DATE.minusYears(17),
      ),
      Arguments.of(
        "2020 Extended Determinate Sentence (U18)",
        HMCTS_SENTENCED_CHARGE.copy(
          results = listOf(HMCTS_SENTENCED_RESULT.copy(code = "EXTDVSU")),
          startDate = LocalDate.of(2014, 8, 15),
          endDate = null,
          convictionDate = CONVICTION_2020_ACT_DATE,
        ),
        UUID.fromString("f1ddc9d6-3c2e-419a-b54d-4e05c92a0f55"),
        CONVICTION_2020_ACT_DATE.minusYears(16),
      ),
      Arguments.of(
        "2020 Extended Determinate Sentence (18-20)",
        HMCTS_SENTENCED_CHARGE.copy(
          results = listOf(HMCTS_SENTENCED_RESULT.copy(code = "EXTDVS")),
          startDate = LocalDate.of(2014, 8, 15),
          endDate = null,
          convictionDate = CONVICTION_2020_ACT_DATE,
        ),
        null, // UUID.fromString("18d5af6d-2fa7-4166-a4c9-8381a1e3c7e0"), More than one sentence type.
        CONVICTION_2020_ACT_DATE.minusYears(19),
      ),
      Arguments.of(
        "2020 Extended Determinate Sentence (21+)",
        HMCTS_SENTENCED_CHARGE.copy(
          results = listOf(HMCTS_SENTENCED_RESULT.copy(code = "EXTIVS")),
          startDate = LocalDate.of(2014, 8, 15),
          endDate = null,
          convictionDate = CONVICTION_2020_ACT_DATE,
        ),
        null, // UUID.fromString("18d5af6d-2fa7-4166-a4c9-8381a1e3c7e0"), More than one sentence type
        ADULT_DOB,
      ),
      Arguments.of(
        "2020 Adult Mandatory Life Sentences",
        HMCTS_SENTENCED_CHARGE.copy(
          results = listOf(HMCTS_SENTENCED_RESULT.copy(code = "MLI")),
          startDate = LocalDate.of(2014, 8, 15),
          endDate = null,
          convictionDate = CONVICTION_2020_ACT_DATE,
        ),
        null, // UUID.fromString("0da58738-2db6-4fba-8f65-438284019756"), More than one sentence type
        ADULT_DOB,
      ),
      Arguments.of(
        "2003 Adult Mandatory Life Sentences",
        HMCTS_SENTENCED_CHARGE.copy(
          results = listOf(HMCTS_SENTENCED_RESULT.copy(code = "MLI")),
          startDate = LocalDate.of(2014, 8, 15),
          endDate = null,
          convictionDate = CONVICTION_2003_ACT_DATE,
        ),
        null, // UUID.fromString("e3b69eb7-6362-4d8b-affe-480abcfd35f7"), More than one sentence type
        ADULT_DOB,
      ),
    )

    val HMCTS_SENTENCED_RESULT = HmctsCourtResult(
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
    )
    val HMCTS_SENTENCED_CHARGE = HmctsCourtCharge(
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
        HMCTS_SENTENCED_RESULT,
      ),
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
      ),
      nextHearing = HmctsNextCourtHearing(
        courtName = "Central London County Court",
        hmctsCourtId = UUID.randomUUID(),
        hmppsCourtId = UUID.randomUUID().toString(),
        hearingDate = LocalDateTime.of(2026, 8, 15, 10, 0),
        hearingId = UUID.randomUUID(),
      ),
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
      charges = listOf(HMCTS_SENTENCED_CHARGE),
      nextHearing = null,
    )
    val ADULT_DOB = LocalDate.of(1991, 1, 1)
    val SENTENCING_ACT_DATE = LocalDate.of(2020, 12, 1)
    val CONVICTION_2003_ACT_DATE = SENTENCING_ACT_DATE.minusYears(1)
    val CONVICTION_2020_ACT_DATE = SENTENCING_ACT_DATE.plusYears(1)
  }
}
