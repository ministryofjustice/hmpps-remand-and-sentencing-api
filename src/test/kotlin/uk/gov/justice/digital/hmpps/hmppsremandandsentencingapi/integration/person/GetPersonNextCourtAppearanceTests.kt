package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.person

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.DpsDataCreator
import java.time.format.DateTimeFormatter

class GetPersonNextCourtAppearanceTests : IntegrationTestBase() {

  @Test
  fun `get person next court appearance`() {
    val (_, createdCourtCase) = createCourtCase()
    val courtAppearance = createdCourtCase.appearances.first()
    val nextCourtAppearance = courtAppearance.nextCourtAppearance!!
    webTestClient.get()
      .uri("/person/${createdCourtCase.prisonerId}/next-court-appearance")
      .headers { it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__COURT_APPEARANCE__RO")) }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.caseReference")
      .isEqualTo(courtAppearance.courtCaseReference!!)
      .jsonPath("$.courtCode")
      .isEqualTo(nextCourtAppearance.courtCode)
      .jsonPath("$.appearanceType.appearanceTypeUuid")
      .isEqualTo(nextCourtAppearance.appearanceTypeUuid.toString())
      .jsonPath("$.courtAppearanceSubType.appearanceSubtypeUuid")
      .isEqualTo(nextCourtAppearance.courtAppearanceSubtypeUuid.toString())
      .jsonPath("$.dateTime")
      .isEqualTo(nextCourtAppearance.appearanceDate.atTime(nextCourtAppearance.appearanceTime).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
  }

  @Test
  fun `return 404 when there are no next court appearances in the future`() {
    val (_, createdCourtCase) = createCourtCase(
      createCourtCase = DpsDataCreator.dpsCreateCourtCase(
        appearances = listOf(
          DpsDataCreator.dpsCreateNonSentencedCourtAppearance(nextCourtAppearance = null),
        ),
      ),
    )
    webTestClient.get()
      .uri("/person/${createdCourtCase.prisonerId}/next-court-appearance")
      .headers { it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__COURT_APPEARANCE__RO")) }
      .exchange()
      .expectStatus()
      .isNotFound
  }

  @Test
  fun `no token results in unauthorized`() {
    val (_, createdCourtCase) = createCourtCase()
    webTestClient.get()
      .uri("/person/${createdCourtCase.prisonerId}/next-court-appearance")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `token with incorrect role is forbidden`() {
    val (_, createdCourtCase) = createCourtCase()
    webTestClient.get()
      .uri("/person/${createdCourtCase.prisonerId}/next-court-appearance")
      .headers {
        it.authToken(roles = listOf("ROLE_OTHER_FUNCTION"))
      }
      .exchange()
      .expectStatus()
      .isForbidden
  }
}
