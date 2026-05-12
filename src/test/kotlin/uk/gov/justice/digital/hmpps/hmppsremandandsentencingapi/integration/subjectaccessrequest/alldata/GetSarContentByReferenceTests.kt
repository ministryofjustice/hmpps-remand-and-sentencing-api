package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.subjectaccessrequest.alldata

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.whenever
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.subjectaccessrequest.alldata.util.ExpectResponseData.emptyFullDataResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.wiremock.AdjustmentsApiExtension.Companion.adjustmentsApi
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.wiremock.CourtRegisterApiExtension.Companion.courtRegisterApi
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.wiremock.PrisonApiExtension.Companion.prisonApi
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ImmigrationDetentionRecordType.IS91
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.subjectaccessrequest.PrisonerDetailsService
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.DpsDataCreator
import java.time.LocalDate

@ActiveProfiles("sar", "test", "test-all-sar-data")
@DisplayName("GetSarContentByReferenceTests (All Data)")
class GetSarContentByReferenceTests : IntegrationTestBase() {

  @Test
  fun `get immigrationDetentions by valid prisoner id`() {
    val (sentenceOne, _) = createCourtCaseTwoSentences()
    prisonApi.stubGetPrisonerDetails(DpsDataCreator.DEFAULT_PRISONER_ID)
    courtRegisterApi.stubGetCourtRegister("COURT1")
    adjustmentsApi.stubAllowCreateAdjustments()
    createRecall(
      DpsDataCreator.dpsCreateRecall(
        sentenceIds = listOf(
          sentenceOne.sentenceUuid,
        ),
      ),
    )
    createImmigrationDetention(
      DpsDataCreator.dpsCreateImmigrationDetention(
        prisonerId = DpsDataCreator.DEFAULT_PRISONER_ID,
        immigrationDetentionRecordType = IS91,
        recordDate = LocalDate.of(2021, 1, 1),
        createdByUsername = "aUser",
        createdByPrison = "PRI",
        appearanceOutcomeUuid = IMMIGRATION_IS91_UUID,
      ),
    )

    // Define expected dynamic dates
    val today = LocalDate.now()
    val convictionDate = today.minusDays(7)
    val nextAppearanceDate = today.plusDays(2)

    webTestClient
      .get()
      .uri { uriBuilder ->
        uriBuilder
          .path("/subject-access-request")
          .queryParam("prn", DpsDataCreator.DEFAULT_PRISONER_ID)
          .build()
      }
      .headers {
        it.authToken(roles = listOf("ROLE_SAR_DATA_ACCESS"))
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      // Prisoner Details
      .jsonPath("$.content.prisonerNumber").isEqualTo("PRI123")
      .jsonPath("$.content.prisonerName").isEqualTo("Cormac Meza")
      // Court Case 1
      .jsonPath("$.content.courtCases[0].courtName").isEqualTo("Crown Court #1")
      .jsonPath("$.content.courtCases[0].caseStatus").isEqualTo("ACTIVE")
      .jsonPath("$.content.courtCases[0].createdAt").exists()
      .jsonPath("$.content.courtCases[0].updatedAt").exists()
      // Court Case 1 - Latest Appearance
      .jsonPath("$.content.courtCases[0].latestCourtAppearance.appearanceDate").isEqualTo(today.toString())
      .jsonPath("$.content.courtCases[0].latestCourtAppearance.appearanceOutcomeName").isEqualTo("Imprisonment")
      .jsonPath("$.content.courtCases[0].latestCourtAppearance.warrantType").isEqualTo("SENTENCING")
      .jsonPath("$.content.courtCases[0].latestCourtAppearance.convictionDate").isEqualTo(convictionDate.toString())
      .jsonPath("$.content.courtCases[0].latestCourtAppearance.nextAppearanceDate").isEqualTo(nextAppearanceDate.toString())
      // Court Case 1 - Latest Appearance - Charges
      .jsonPath("$.content.courtCases[0].latestCourtAppearance.charges[0].offenceCode").isEqualTo("OFF123")
      .jsonPath("$.content.courtCases[0].latestCourtAppearance.charges[0].offenceDescription").isEqualTo("No Data Held")
      .jsonPath("$.content.courtCases[0].latestCourtAppearance.charges[0].terrorRelated").isEqualTo("Yes")
      .jsonPath("$.content.courtCases[0].latestCourtAppearance.charges[0].foreignPowerRelated").isEqualTo("No")
      .jsonPath("$.content.courtCases[0].latestCourtAppearance.charges[0].domesticViolenceRelated").isEqualTo("No")
      .jsonPath("$.content.courtCases[0].latestCourtAppearance.charges[0].offenceStartDate").isEqualTo(today.toString())
      .jsonPath("$.content.courtCases[0].latestCourtAppearance.charges[0].offenceEndDate").isEqualTo("No Data Held")
      .jsonPath("$.content.courtCases[0].latestCourtAppearance.charges[0].chargeOutcome").isEqualTo("Imprisonment")
      .jsonPath("$.content.courtCases[0].latestCourtAppearance.charges[0].liveSentence.sentenceTypeDescription").isEqualTo("Serious Offence Sec 250 Sentencing Code (U18)")
      .jsonPath("$.content.courtCases[0].latestCourtAppearance.charges[0].liveSentence.sentenceTypeClassification").isEqualTo("STANDARD")
      .jsonPath("$.content.courtCases[0].latestCourtAppearance.charges[0].liveSentence.periodLengths[0].years").isEqualTo(1)
      .jsonPath("$.content.courtCases[0].latestCourtAppearance.charges[0].liveSentence.sentenceServeType").isEqualTo("FORTHWITH")
      // Court Case 1 - Appearances (full list)
      .jsonPath("$.content.courtCases[0].appearances[0].appearanceDate").isEqualTo(today.toString())
      .jsonPath("$.content.courtCases[0].appearances[0].appearanceOutcomeName").isEqualTo("Imprisonment")
      .jsonPath("$.content.courtCases[0].appearances[0].warrantType").isEqualTo("SENTENCING")
      .jsonPath("$.content.courtCases[0].appearances[0].convictionDate").isEqualTo(convictionDate.toString())
      .jsonPath("$.content.courtCases[0].appearances[0].nextAppearanceDate").isEqualTo(nextAppearanceDate.toString())
      // Court Case 2
      .jsonPath("$.content.courtCases[1].courtName").isEqualTo("No Data Held")
      .jsonPath("$.content.courtCases[1].caseStatus").isEqualTo("ACTIVE")
      .jsonPath("$.content.courtCases[1].createdAt").exists()
      .jsonPath("$.content.courtCases[1].updatedAt").exists()
      .jsonPath("$.content.courtCases[1].latestCourtAppearance.appearanceDate").isEqualTo("No Data Held")
      .jsonPath("$.content.courtCases[1].appearances[0].appearanceDate").isEqualTo("2021-01-01")
      .jsonPath("$.content.courtCases[1].appearances[0].appearanceOutcomeName").isEqualTo("Immigration Detainee")
      .jsonPath("$.content.courtCases[1].appearances[0].warrantType").isEqualTo("IMMIGRATION")
      // Recalls
      .jsonPath("$.content.recalls[0].recallType").isEqualTo("FTR_14")
      .jsonPath("$.content.recalls[0].revocationDate").isEqualTo("2024-01-02")
      .jsonPath("$.content.recalls[0].returnToCustodyDate").isEqualTo("2024-02-03")
      .jsonPath("$.content.recalls[0].inPrisonOnRevocationDate").isEqualTo("No")
      .jsonPath("$.content.recalls[0].recallSentenceStatus").isEqualTo("ACTIVE")
      // Immigration Detentions
      .jsonPath("$.content.immigrationDetentions[0].immigrationDetentionRecordType").isEqualTo("IS91")
      .jsonPath("$.content.immigrationDetentions[0].recordDate").isEqualTo("2021-01-01")
  }

  @Test
  fun `get empty court cases, recalls & immigrationDetentions by valid prisoner id with no data yet associated`() {
    createCourtCaseTwoSentences()
    webTestClient
      .get()
      .uri { uriBuilder ->
        uriBuilder
          .path("/subject-access-request")
          .queryParam("prn", DpsDataCreator.DEFAULT_PRISONER_ID)
          .queryParam("fromDate", "2010-01-01")
          .queryParam("toDate", "2010-01-01")
          .build()
      }
      .headers {
        it.authToken(roles = listOf("ROLE_SAR_DATA_ACCESS"))
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .consumeWith(::println)
      .json(emptyFullDataResponse())
  }

  @Test
  fun `get 209 by attempting use of crn`() {
    webTestClient
      .get()
      .uri { uriBuilder ->
        uriBuilder
          .path("/subject-access-request")
          .queryParam("crn", "foo-bar")
          .build()
      }
      .headers {
        it.authToken(roles = listOf("ROLE_SAR_DATA_ACCESS"))
      }
      .exchange()
      .expectStatus()
      .isEqualTo(209)
  }

  @Test
  fun `get 204 when no data returned from service`() {
    webTestClient
      .get()
      .uri { uriBuilder ->
        uriBuilder
          .path("/subject-access-request")
          .queryParam("prn", "null")
          .build()
      }
      .headers {
        it.authToken(roles = listOf("ROLE_SAR_DATA_ACCESS"))
      }
      .exchange()
      .expectStatus()
      .isEqualTo(204)
  }

  @Test
  fun `get 400 when query not properly formed`() {
    webTestClient
      .get()
      .uri { uriBuilder ->
        uriBuilder
          .path("/subject-access-request")
          .queryParam("invalid-param", "null")
          .build()
      }
      .headers {
        it.authToken(roles = listOf("ROLE_SAR_DATA_ACCESS"))
      }
      .exchange()
      .expectStatus()
      .isEqualTo(400)
  }

  @Test
  fun `get 401 when auth token missing or not valid`() {
    webTestClient
      .get()
      .uri { uriBuilder ->
        uriBuilder
          .path("/subject-access-request")
          .queryParam("invalid-param", "null")
          .build()
      }
      .exchange()
      .expectStatus()
      .isEqualTo(401)
  }

  @Test
  fun `get 403 when auth token role not SAR_DATA_ACCESS`() {
    webTestClient
      .get()
      .uri { uriBuilder ->
        uriBuilder
          .path("/subject-access-request")
          .queryParam("invalid-param", "null")
          .build()
      }
      .headers {
        it.authToken(roles = listOf("ROLE_LIMITED_ACCESS"))
      }
      .exchange()
      .expectStatus()
      .isEqualTo(403)
  }

  @Nested
  @DisplayName("Internal Server Errors")
  inner class ErrorScenarios {

    @MockitoBean
    private lateinit var prisonerDetailsService: PrisonerDetailsService

    @Test
    fun `get 500 when internal exception thrown`() {
      whenever(
        prisonerDetailsService
          .getPrisonerDetails("foo-bar", null, null),
      ).thenThrow(RuntimeException::class.java)
      webTestClient
        .get()
        .uri { uriBuilder ->
          uriBuilder
            .path("/subject-access-request")
            .queryParam("prn", "foo-bar")
            .build()
        }
        .headers {
          it.authToken(roles = listOf("ROLE_SAR_DATA_ACCESS"))
        }
        .exchange()
        .expectStatus()
        .isEqualTo(500)
    }
  }
}
