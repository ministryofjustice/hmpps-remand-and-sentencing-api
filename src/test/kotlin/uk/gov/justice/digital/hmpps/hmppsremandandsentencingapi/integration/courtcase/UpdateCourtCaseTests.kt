package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.courtcase

import org.hamcrest.text.MatchesPattern
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.DpsDataCreator
import java.time.LocalDate
import java.util.UUID

class UpdateCourtCaseTests : IntegrationTestBase() {

  @Test
  fun `update court case`() {
    val courtCase = createCourtCase()
    val appearance = DpsDataCreator.dpsCreateCourtAppearance(courtCaseUuid = courtCase.first, appearanceUUID = courtCase.second.appearances.first().appearanceUuid, lifetimeUuid = courtCase.second.appearances.first().lifetimeUuid, courtCaseReference = "ADIFFERENTCOURTCASEREFERENCE")
    val editedCourtCase = courtCase.second.copy(appearances = listOf(appearance))
    webTestClient
      .put()
      .uri("/court-case/${courtCase.first}")
      .bodyValue(editedCourtCase)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.courtCaseUuid")
      .value(MatchesPattern.matchesPattern("([a-f0-9]{8}(-[a-f0-9]{4}){4}[a-f0-9]{8})"))
  }

  @Test
  fun `delete an appearance if emitted from list of appearances`() {
    val appearance = DpsDataCreator.dpsCreateCourtAppearance()
    val secondAppearance = DpsDataCreator.dpsCreateCourtAppearance(appearanceDate = LocalDate.now().minusDays(7))
    val courtCase = DpsDataCreator.dpsCreateCourtCase(appearances = listOf(appearance, secondAppearance))
    val courtCaseUuid = UUID.randomUUID()
    webTestClient
      .put()
      .uri("/court-case/$courtCaseUuid")
      .bodyValue(courtCase)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isOk

    val courtCaseWithoutSecondAppearance = courtCase.copy(appearances = listOf(appearance))
    webTestClient
      .put()
      .uri("/court-case/$courtCaseUuid")
      .bodyValue(courtCaseWithoutSecondAppearance)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isOk

    webTestClient
      .get()
      .uri("/court-case/$courtCaseUuid")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING"))
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.latestAppearance.appearanceUuid")
      .isEqualTo(appearance.appearanceUuid.toString())
      .jsonPath("$.appearances.[?(@.appearanceUuid == '${secondAppearance.appearanceUuid}')]")
      .doesNotExist()
      .jsonPath("$.appearances.[?(@.appearanceUuid == '${appearance.appearanceUuid}')]")
      .exists()
  }

  @Test
  fun `cannot edit prisoner id of a court case`() {
    val courtCase = createCourtCase()
    val editedCourtCase = courtCase.second.copy(prisonerId = "ADIFFERENTPRISONER")

    webTestClient
      .put()
      .uri("/court-case/${courtCase.first}")
      .bodyValue(editedCourtCase)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isBadRequest
  }

  @Test
  fun `no token results in unauthorized`() {
    val createCourtCase = DpsDataCreator.dpsCreateCourtCase()
    webTestClient
      .put()
      .uri("/court-case/${UUID.randomUUID()}")
      .bodyValue(createCourtCase)
      .headers {
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `token with incorrect role is forbidden`() {
    val createCourtCase = DpsDataCreator.dpsCreateCourtCase()
    webTestClient
      .put()
      .uri("/court-case/${UUID.randomUUID()}")
      .bodyValue(createCourtCase)
      .headers {
        it.authToken(roles = listOf("ROLE_OTHER_FUNCTION"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isForbidden
  }
}
