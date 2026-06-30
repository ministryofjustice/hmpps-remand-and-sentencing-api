package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.courtappearanceschedule

import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.DpsDataCreator
import java.time.format.DateTimeFormatter

class UpdateCourtAppearanceSchedulesTests : IntegrationTestBase() {

  @Test
  fun `can update court appearances`() {
    val (_, createdCourtCase) = createCourtCase(DpsDataCreator.dpsCreateCourtCase(appearances = listOf(DpsDataCreator.dpsCreateNonSentencedCourtAppearance())))
    val courtAppearance = createdCourtCase.appearances.first()

    val updateCourtAppearanceSchedule = DpsDataCreator.updateCourtAppearanceSchedule()

    webTestClient
      .put()
      .uri("/court-appearance-schedule/${courtAppearance.appearanceUuid}")
      .bodyValue(updateCourtAppearanceSchedule)
      .headers {
        it.authToken(roles = listOf("ROLE_COURT_APPEARANCES__COURT_APPEARANCE_SCHEDULER__RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isNoContent

    webTestClient
      .get()
      .uri("/person/${createdCourtCase.prisonerId}/court-appearance-schedules")
      .headers {
        it.authToken(roles = listOf("ROLE_COURT_APPEARANCES__COURT_APPEARANCE_SCHEDULER__RO"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.courtAppearances[?(@.id == '${courtAppearance.appearanceUuid}')].courtCode")
      .isEqualTo(updateCourtAppearanceSchedule.courtCode)
      .jsonPath("$.courtAppearances[?(@.id == '${courtAppearance.appearanceUuid}')].reason.code")
      .isEqualTo(updateCourtAppearanceSchedule.reasonCode)
      .jsonPath("$.courtAppearances[?(@.id == '${courtAppearance.appearanceUuid}')].start")
      .isEqualTo(updateCourtAppearanceSchedule.start.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
      .jsonPath("$.courtAppearances[?(@.id == '${courtAppearance.appearanceUuid}')].comments")
      .isEqualTo(updateCourtAppearanceSchedule.comments)
  }
}
