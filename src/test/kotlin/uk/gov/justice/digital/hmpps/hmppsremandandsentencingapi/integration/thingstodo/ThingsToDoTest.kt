package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.thingstodo

import org.assertj.core.api.Assertions
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.dto.HmctsCourHearingDocument
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.dto.HmctsCourtHearing
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateCourtAppearance
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.HearingThingsToDoData
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.HearingThingsToDoWarrantType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.ThingToDo
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.ThingToDoType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.ThingsToDo
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.wiremock.CourtDataIngestionApiExtension
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.DpsDataCreator
import java.time.LocalDate
import java.util.UUID
import java.util.stream.Stream

class ThingsToDoTest : IntegrationTestBase() {

  @ParameterizedTest(name = "Things to do {0}")
  @MethodSource("thingsToDoArguments")
  fun `Test get things to do`(testMessage: String, hearings: List<HmctsCourtHearing>, createCourtAppearance: CreateCourtAppearance?, expectedHearingData: List<HearingThingsToDoData>?) {
    CourtDataIngestionApiExtension.courtDataIngestionApi.stubCourtHearingsByPrisoner(
      PRISONER_ID,
      hearings,
    )
    val createdCourtCase = if (createCourtAppearance != null) {
      createCourtCase(DpsDataCreator.dpsCreateCourtCase(prisonerId = PRISONER_ID, appearances = listOf(createCourtAppearance)))
    } else {
      null
    }

    val response = webTestClient
      .get()
      .uri("/things-to-do/prisoner/$PRISONER_ID")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_SENTENCE_RO"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus().isOk
      .returnResult(ThingsToDo::class.java)
      .responseBody.blockFirst()!!

    val expectedThingsToDo = ThingsToDo(
      prisonerId = PRISONER_ID,
      thingsToDo = expectedHearingData.orEmpty().map {
        ThingToDo(
          type = ThingToDoType.NEW_WARRANT,
          hearingThingsToDoData = it.copy(
            courtCaseUuid = if (it.courtCaseUuid != null) createdCourtCase!!.first else null,
          ),
        )
      },
    )
    Assertions.assertThat(response).isEqualTo(expectedThingsToDo)
  }

  companion object {
    val HMCTS_HEARING_ID = UUID.randomUUID()
    val DOCUMENT_ID = UUID.randomUUID()
    val PRISONER_ID = "ABC123"
    val SENTENCING_WARRANT = HmctsCourHearingDocument(
      "SENTENCING_WARRANT",
      DOCUMENT_ID,
    )
    val REMAND_WARRANT = HmctsCourHearingDocument(
      "REMAND_WARRANT",
      DOCUMENT_ID,
    )
    val HEARING = HmctsCourtHearing(
      hearingId = HMCTS_HEARING_ID,
      courtName = "My court",
      courtId = UUID.randomUUID(),
      hearingDate = LocalDate.of(2026, 1, 1),
      caseReferences = listOf("ABC123"),
      hearingType = "First hearing",
      documents = emptyList(),
    )
    val SENTENCING_HEARING = HEARING.copy(
      documents = listOf(SENTENCING_WARRANT),
    )
    val REMAND_HEARING = HEARING.copy(
      documents = listOf(REMAND_WARRANT),
    )

    @JvmStatic
    fun thingsToDoArguments(): Stream<Arguments> = Stream.of(
      Arguments.of(
        "No existing court cases with remand warrant give a thing to do",
        listOf(REMAND_HEARING),
        null,
        listOf(
          HearingThingsToDoData(
            hearingId = HMCTS_HEARING_ID,
            courtCaseReference = "ABC123",
            hearingDate = LocalDate.of(2026, 1, 1),
            hearingType = "First hearing",
            warrantType = HearingThingsToDoWarrantType.REMAND,
            courtCaseUuid = null,
          ),
        ),
      ),
      Arguments.of(
        "No existing court cases with remand warrant and multiple case reference gives no thing to do",
        listOf(
          REMAND_HEARING.copy(
            caseReferences = listOf("ABC123", "DEF456"),
          ),
        ),
        null,
        null,
      ),
      Arguments.of(
        "No existing court cases with sentencing warrant give sentencing thing to do",
        listOf(SENTENCING_HEARING),
        null,
        listOf(
          HearingThingsToDoData(
            hearingId = HMCTS_HEARING_ID,
            courtCaseReference = "ABC123",
            hearingDate = LocalDate.of(2026, 1, 1),
            hearingType = "First hearing",
            warrantType = HearingThingsToDoWarrantType.SENTENCING,
            courtCaseUuid = null,
          ),
        ),
      ),
      Arguments.of(
        "No existing court cases with pcr only document gives no thing to do",
        listOf(
          HEARING.copy(
            documents = listOf(
              REMAND_WARRANT.copy(
                documentType = "PRISON_COURT_REGISTER",
              ),
            ),
          ),
        ),
        null,
        null,
      ),
      Arguments.of(
        "Existing court case with same reference with remand warrant gives thing to do with case reference",
        listOf(REMAND_HEARING),
        DpsDataCreator.dpsCreateCourtAppearance(
          courtCaseReference = REMAND_HEARING.caseReferences[0],
        ),
        listOf(
          HearingThingsToDoData(
            hearingId = HMCTS_HEARING_ID,
            courtCaseReference = "ABC123",
            hearingDate = LocalDate.of(2026, 1, 1),
            hearingType = "First hearing",
            warrantType = HearingThingsToDoWarrantType.REMAND,
            courtCaseUuid = "EXISTING ID",
          ),
        ),
      ),
      Arguments.of(
        "Existing court case with same reference with sentencing warrant gives no thing to do",
        listOf(SENTENCING_HEARING),
        DpsDataCreator.dpsCreateCourtAppearance(
          courtCaseReference = SENTENCING_HEARING.caseReferences[0],
        ),
        null,
      ),
      Arguments.of(
        "Existing court case with different reference with remand warrant gives no remand thing to do",
        listOf(REMAND_HEARING),
        DpsDataCreator.dpsCreateCourtAppearance(
          courtCaseReference = "OTHERCASEREF123",
        ),
        listOf(
          HearingThingsToDoData(
            hearingId = HMCTS_HEARING_ID,
            courtCaseReference = "ABC123",
            hearingDate = LocalDate.of(2026, 1, 1),
            hearingType = "First hearing",
            warrantType = HearingThingsToDoWarrantType.REMAND,
            courtCaseUuid = null,
          ),
        ),
      ),
      Arguments.of(
        "No court case with multiple hearings give multiple things to do",
        listOf(REMAND_HEARING, SENTENCING_HEARING.copy(hearingDate = LocalDate.of(2026, 6, 1))),
        null,
        listOf(
          HearingThingsToDoData(
            hearingId = HMCTS_HEARING_ID,
            courtCaseReference = "ABC123",
            hearingDate = LocalDate.of(2026, 6, 1),
            hearingType = "First hearing",
            warrantType = HearingThingsToDoWarrantType.SENTENCING,
            courtCaseUuid = null,
          ),
          HearingThingsToDoData(
            hearingId = HMCTS_HEARING_ID,
            courtCaseReference = "ABC123",
            hearingDate = LocalDate.of(2026, 1, 1),
            hearingType = "First hearing",
            warrantType = HearingThingsToDoWarrantType.REMAND,
            courtCaseUuid = null,
          ),
        ),
      ),
    )
  }
}
