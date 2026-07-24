package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.thingstodo

import org.assertj.core.api.Assertions
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.dto.HmctsCourHearing
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.client.dto.HmctsCourHearingDocument
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.HearingThingsToDoData
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.ThingToDoType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.ThingsToDo
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.wiremock.CourtDataIngestionApiExtension
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.DpsDataCreator
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import java.util.stream.Stream

class ThingsToDoTest : IntegrationTestBase() {

  @ParameterizedTest(name = "Things to do {0}")
  @MethodSource("thingsToDoArguments")
  fun `Test get things to do`(testMessage: String, hearing: HmctsCourHearing, existingCourtCase: Boolean, expectedThingsToDo: ThingsToDo) {
    CourtDataIngestionApiExtension.courtDataIngestionApi.stubCourtHearingsByPrisoner(
      PRISONER_ID,
      listOf(hearing),
    )
    if (existingCourtCase) {
      createCourtCase(DpsDataCreator.dpsCreateCourtCase(prisonerId = PRISONER_ID))
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
    val HEARING = HmctsCourHearing(
      hearingId = HMCTS_HEARING_ID,
      courtName = "My court",
      courtId = UUID.randomUUID(),
      hearingDate = LocalDateTime.of(2026, 1, 1, 1, 1, 1),
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
        REMAND_HEARING,
        false,
        ThingsToDo(
          prisonerId = PRISONER_ID,
          thingsToDo = listOf(ThingToDoType.NEW_REMAND_WARRANT),
          hearingThingsToDoData = HearingThingsToDoData(
            HMCTS_HEARING_ID,
            "ABC123",
            LocalDate.of(2026, 1, 1),
          ),
        ),
      ),
      Arguments.of(
        "No existing court cases with remand warrant and multiple case reference gives no thing to do",
        REMAND_HEARING.copy(
          caseReferences = listOf("ABC123", "DEF456"),
        ),
        false,
        ThingsToDo(
          prisonerId = PRISONER_ID,
          thingsToDo = emptyList(),
          hearingThingsToDoData = null,
        ),
      ),
      Arguments.of(
        "No existing court cases with sentencing warrant give sentencing thing to do",
        SENTENCING_HEARING,
        false,
        ThingsToDo(
          prisonerId = PRISONER_ID,
          thingsToDo = listOf(ThingToDoType.NEW_SENTENCING_WARRANT),
          hearingThingsToDoData = HearingThingsToDoData(
            HMCTS_HEARING_ID,
            "ABC123",
            LocalDate.of(2026, 1, 1),
          ),
        ),
      ),
      Arguments.of(
        "No existing court cases with pcr only document gives no thing to do",
        HEARING.copy(
          documents = listOf(
            REMAND_WARRANT.copy(
              documentType = "PRISON_COURT_REGISTER",
            ),
          ),
        ),
        false,
        ThingsToDo(
          prisonerId = PRISONER_ID,
          thingsToDo = emptyList(),
          hearingThingsToDoData = null,
        ),
      ),
      Arguments.of(
        "Existing court cases with remand warrant gives no thing to do",
        REMAND_HEARING,
        true,
        ThingsToDo(
          prisonerId = PRISONER_ID,
          thingsToDo = emptyList(),
          hearingThingsToDoData = null,
        ),
      ),
    )
  }
}
