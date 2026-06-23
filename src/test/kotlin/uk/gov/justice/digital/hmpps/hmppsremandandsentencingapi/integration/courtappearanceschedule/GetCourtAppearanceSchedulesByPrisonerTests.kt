package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.courtappearanceschedule

import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.CourtAppearanceEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.service.LegacyAppearanceTypeService
import java.time.format.DateTimeFormatter

class GetCourtAppearanceSchedulesByPrisonerTests : IntegrationTestBase() {

  @Test
  fun `get court appearance schedules by prisoner`() {
    val (courtCaseUuid, createdCourtCase) = createCourtCase()
    val appearance = createdCourtCase.appearances.first()
    val nextCourtAppearance = appearance.nextCourtAppearance
    val futureAppearance = courtAppearanceRepository.findByCourtCaseCaseUniqueIdentifierAndStatusId(
      courtCaseUuid,
      CourtAppearanceEntityStatus.FUTURE,
    )
    val appearanceSubtype = courtAppearanceSubtypeRepository.findByAppearanceSubtypeUuid(nextCourtAppearance!!.courtAppearanceSubtypeUuid!!)!!
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
      .jsonPath("$.courtAppearances[?(@.id == '${appearance.appearanceUuid}')]")
      .exists()
      .jsonPath("$.courtAppearances[?(@.id == '${appearance.appearanceUuid}')].personIdentifier")
      .isEqualTo(createdCourtCase.prisonerId)
      .jsonPath("$.courtAppearances[?(@.id == '${appearance.appearanceUuid}')].courtCode")
      .isEqualTo(appearance.courtCode)
      .jsonPath("$.courtAppearances[?(@.id == '${appearance.appearanceUuid}')].reason.code")
      .isEqualTo(LegacyAppearanceTypeService.DEFAULT_APPEARANCE_TYPE_NOMIS_CODE)
      .jsonPath("$.courtAppearances[?(@.id == '${appearance.appearanceUuid}')].start")
      .isEqualTo(appearance.appearanceDate.atStartOfDay().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
      .jsonPath("$.courtAppearances[?(@.id == '${appearance.appearanceUuid}')].isDuplicate")
      .isEqualTo(false)
      .jsonPath("$.courtAppearances[?(@.id == '${futureAppearance.appearanceUuid}')].courtCode")
      .isEqualTo(nextCourtAppearance.courtCode)
      .jsonPath("$.courtAppearances[?(@.id == '${futureAppearance.appearanceUuid}')].reason.code")
      .isEqualTo(appearanceSubtype.nomisCode)
  }

  @Test
  fun `no token results in unauthorized`() {
    val (_, createdCourtCase) = createCourtCase()
    webTestClient
      .get()
      .uri("/person/${createdCourtCase.prisonerId}/court-appearance-schedules")
      .headers {
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `token with incorrect role is forbidden`() {
    val (_, createdCourtCase) = createCourtCase()
    webTestClient
      .get()
      .uri("/person/${createdCourtCase.prisonerId}/court-appearance-schedules")
      .headers {
        it.authToken(roles = listOf("ROLE_OTHER_FUNCTION"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isForbidden
  }
}
