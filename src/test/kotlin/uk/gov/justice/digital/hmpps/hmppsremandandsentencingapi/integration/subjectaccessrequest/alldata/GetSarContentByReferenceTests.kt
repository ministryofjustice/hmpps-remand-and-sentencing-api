package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.subjectaccessrequest.alldata

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.whenever
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.subjectaccessrequest.alldata.Charge
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.subjectaccessrequest.alldata.CourtAppearance
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.subjectaccessrequest.alldata.CourtCase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.subjectaccessrequest.alldata.ImmigrationDetention
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.subjectaccessrequest.alldata.Period
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.subjectaccessrequest.alldata.Prisoner
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.subjectaccessrequest.alldata.Recall
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.subjectaccessrequest.alldata.Sentence
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.subjectaccessrequest.PrisonerDetailsService
import java.time.LocalDate
import java.time.ZonedDateTime

@ActiveProfiles("sar", "test", "test-all-sar-data")
@DisplayName("GetSarContentByReferenceTests (All Data)")
class GetSarContentByReferenceTests : IntegrationTestBase() {

  @MockitoBean
  lateinit var allDataPrisonerDetailsService: PrisonerDetailsService<Prisoner>

  @Test
  fun `get immigrationDetentions by valid prisoner id`() {
    whenever(
      allDataPrisonerDetailsService
        .getPrisonerDetails("A6764DZ", null, null),
    ).thenReturn(
      Prisoner(
        prisonerNumber = "A6764DZ",
        prisonerName = "RALPH DOG",
        courtCases = listOf(
          CourtCase(
            courtName = "Glasgow High Court",
            caseStatus = "ACTIVE",
            createdAt = ZonedDateTime.parse("2026-02-03T10:02+00:00[Europe/London]"),
            updatedAt = ZonedDateTime.parse("2026-02-03T10:02+00:00[Europe/London]"),
            courtAppearance = CourtAppearance(
              appearanceDate = LocalDate.parse("2026-02-03"),
              appearanceOutcomeName = "Imprisonment",
              warrantType = "SENTENCING",
              convictionDate = null,
              nextAppearanceDate = null,
              charges = listOf(
                Charge(
                  offenseCode = "RF96124",
                  offenseDescription = "A person procuring or persuading, or attempting to procure or persuade a reserve force naval rating or marine, liable",
                  terrorRelated = null,
                  foreignPowerRelated = null,
                  domesticViolenceRelated = false,
                  offenseStartDate = LocalDate.parse("1997-01-01"),
                  offenseEndDate = null,
                  chargeOutcome = "Imprisonment",
                  sentences = listOf(
                    Sentence(
                      sentenceType = "ORA Breach Top Up Supervision",
                      sentenceServeType = "BOTUS",
                      periods = listOf(
                        Period(
                          years = 0,
                          months = 6,
                          weeks = 0,
                          days = 0,
                        ),
                      ),
                      periodOrder = "CONCURRENT",
                      isRecallable = false,
                    ),
                  ),
                ),
              ),
            ),
          ),
        ),
        recalls = listOf(
          Recall(
            recallType = "LR",
            revocationDate = LocalDate.parse("2025-06-10"),
            returnToCustodyDate = null,
            inPrisonOnRevocationDate = true,
            recallSentenceStatus = "DELETED",
          ),
          Recall(
            recallType = "CUR_HDC",
            revocationDate = null,
            returnToCustodyDate = null,
            inPrisonOnRevocationDate = false,
            recallSentenceStatus = "ACTIVE",
          ),
          Recall(
            recallType = "LR",
            revocationDate = LocalDate.parse("2026-03-09"),
            returnToCustodyDate = null,
            inPrisonOnRevocationDate = true,
            recallSentenceStatus = "ACTIVE",
          ),
        ),
        immigrationDetentions = listOf(
          ImmigrationDetention(
            immigrationDetentionRecordType = "DEPORTATION_ORDER",
            homeOfficeReferenceNumber = "124222111",
            recordDate = LocalDate.parse("2026-02-09"),
            noLongerOfInterestReason = null,
            noLongerOfInterestComment = null,
          ),
          ImmigrationDetention(
            immigrationDetentionRecordType = "NO_LONGER_OF_INTEREST",
            homeOfficeReferenceNumber = null,
            recordDate = LocalDate.parse("2026-03-01"),
            noLongerOfInterestReason = "RIGHT_TO_REMAIN",
            noLongerOfInterestComment = "",
          ),
        ),
      ),
    )

    webTestClient
      .get()
      .uri { uriBuilder ->
        uriBuilder
          .path("/subject-access-request")
          .queryParam("prn", "A6764DZ")
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
      .json(
        """
        {
          "attachments": [],
          "content": {
            "prisonerNumber": "A6764DZ",
            "prisonerName": "RALPH DOG",
            "courtCases": [
              {
                "courtName": "Glasgow High Court",
                "caseStatus": "ACTIVE",
                "createdAt": "2026-02-03 10:02",
                "updatedAt": "2026-02-03 10:02",
                "courtAppearance": {
                  "appearanceDate": "2026-02-03",
                  "appearanceOutcomeName": "Imprisonment",
                  "warrantType": "SENTENCING",
                  "convictionDate": "No Data Held",
                  "nextAppearanceDate": "No Data Held",
                  "charges": [
                    {
                      "offenseCode": "RF96124",
                      "offenseDescription": "A person procuring or persuading, or attempting to procure or persuade a reserve force naval rating or marine, liable",
                      "terrorRelated": "No",
                      "foreignPowerRelated": "No",
                      "domesticViolenceRelated": "No",
                      "offenseStartDate": "1997-01-01",
                      "offenseEndDate": "No Data Held",
                      "chargeOutcome": "Imprisonment",
                      "sentences": [
                        {
                          "sentenceType": "ORA Breach Top Up Supervision",
                          "sentenceServeType": "BOTUS",
                          "periods": [
                            {
                              "years": 0,
                              "months": 6,
                              "weeks": 0,
                              "days": 0
                            }
                          ],
                          "periodOrder": "CONCURRENT",
                          "isRecallable": "No"
                        }
                      ]
                    }
                  ]
                }
              }              
            ],
            "recalls": [
              {
                "recallType": "LR",
                "revocationDate": "2025-06-10",
                "returnToCustodyDate": "No Data Held",
                "inPrisonOnRevocationDate": "Yes",
                "recallSentenceStatus": "DELETED"
              },
              {
                "recallType": "CUR_HDC",
                "revocationDate": "No Data Held",
                "returnToCustodyDate": "No Data Held",
                "inPrisonOnRevocationDate": "No",
                "recallSentenceStatus": "ACTIVE"
              },
              {
                "recallType": "LR",
                "revocationDate": "2026-03-09",
                "returnToCustodyDate": "No Data Held",
                "inPrisonOnRevocationDate": "Yes",
                "recallSentenceStatus": "ACTIVE"
              }
            ],
            "immigrationDetentions": [
              {
                "immigrationDetentionRecordType": "DEPORTATION_ORDER",
                "homeOfficeReferenceNumber": "124222111",
                "recordDate": "2026-02-09",
                "noLongerOfInterestReason": "No Data Held",
                "noLongerOfInterestComment": "No Data Held"
              },
              {
                "immigrationDetentionRecordType": "NO_LONGER_OF_INTEREST",
                "homeOfficeReferenceNumber": "No Data Held",
                "recordDate": "2026-03-01",
                "noLongerOfInterestReason": "RIGHT_TO_REMAIN",
                "noLongerOfInterestComment": ""
              }
            ]
          }
        }
        """.trimIndent(),
      )
  }

  @Test
  fun `get empty court cases, recalls & immigrationDetentions by valid prisoner id with no data yet associated`() {
    whenever(
      allDataPrisonerDetailsService
        .getPrisonerDetails("foo-bar", null, null),
    ).thenReturn(Prisoner(prisonerNumber = "foo-bar"))
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
      .isOk
      .expectBody()
      .consumeWith(::println)
      .json(
        """
        {
          "attachments": [],
          "content": {
            "prisonerNumber": "foo-bar",
            "prisonerName": "No Data Held",
            "courtCases": [],
            "recalls": [],
            "immigrationDetentions": []
          }
        }
        """.trimIndent(),
      )
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

  @Test
  fun `get 500 when internal exception thrown`() {
    whenever(
      allDataPrisonerDetailsService
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
