package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.courtappearanceschedule

import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.courtappearanceschedule.SearchCourtAppearanceSchedulesRequest
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.CourtAppearanceEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.service.LegacyAppearanceTypeService
import java.time.format.DateTimeFormatter
import java.util.*

class SearchCourtAppearanceSchedulesTests : IntegrationTestBase() {

  @Test
  fun `able to search by appearance uuids`() {
    val (courtCaseUuid, createdCourtCase) = createCourtCase()
    val appearance = createdCourtCase.appearances.first()
    val nextCourtAppearance = appearance.nextCourtAppearance
    val futureAppearance = courtAppearanceRepository.findByCourtCaseCaseUniqueIdentifierAndStatusId(
      courtCaseUuid,
      CourtAppearanceEntityStatus.FUTURE,
    )
    val appearanceSubtype = courtAppearanceSubtypeRepository.findByAppearanceSubtypeUuid(nextCourtAppearance!!.courtAppearanceSubtypeUuid!!)!!
    val searchRequest = SearchCourtAppearanceSchedulesRequest(listOf(appearance.appearanceUuid, futureAppearance.appearanceUuid))
    webTestClient
      .post()
      .uri("/search/court-appearance-schedules")
      .bodyValue(searchRequest)
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
  fun `searching by random UUID returns empty list`() {
    val searchRequest = SearchCourtAppearanceSchedulesRequest(listOf(UUID.randomUUID()))
    webTestClient
      .post()
      .uri("/search/court-appearance-schedules")
      .bodyValue(searchRequest)
      .headers {
        it.authToken(roles = listOf("ROLE_COURT_APPEARANCES__COURT_APPEARANCE_SCHEDULER__RO"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.courtAppearances")
      .isEmpty
  }

  @Test
  fun `return duplicate court appearance records`() {
    val (_, response) = createBookingCourtCase()
    val appearanceUuid = response.appearances.first().appearanceUuid
    val searchRequest = SearchCourtAppearanceSchedulesRequest(listOf(appearanceUuid))
    webTestClient
      .post()
      .uri("/search/court-appearance-schedules")
      .bodyValue(searchRequest)
      .headers {
        it.authToken(roles = listOf("ROLE_COURT_APPEARANCES__COURT_APPEARANCE_SCHEDULER__RO"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.courtAppearances[?(@.id == '$appearanceUuid')].isDuplicate")
      .isEqualTo(true)
  }

  @Test
  fun `no token results in unauthorized`() {
    val searchRequest = SearchCourtAppearanceSchedulesRequest(listOf(UUID.randomUUID()))
    webTestClient
      .post()
      .uri("/search/court-appearance-schedules")
      .bodyValue(searchRequest)
      .headers {
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `token with incorrect role is forbidden`() {
    val searchRequest = SearchCourtAppearanceSchedulesRequest(listOf(UUID.randomUUID()))
    webTestClient
      .post()
      .uri("/search/court-appearance-schedules")
      .bodyValue(searchRequest)
      .headers {
        it.authToken(roles = listOf("ROLE_OTHER_FUNCTION"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isForbidden
  }
}
