package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.courtcase

import org.hamcrest.text.MatchesPattern
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateCharge
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateCourtAppearance
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateCourtCase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import java.time.LocalDate
import java.util.UUID

class UpdateCourtCaseTests : IntegrationTestBase() {

  @Test
  fun `update court case`() {
    val courtCase = createCourtCase()
    val charge = CreateCharge(UUID.randomUUID(), "OFF123", LocalDate.now(), null, "OUT123")
    val appearance = CreateCourtAppearance(courtCase.first, courtCase.second.appearances.first().appearanceUuid, "OUT123", "COURT1", "ADIFFERENTCOURTCASEREFERENCE", LocalDate.now(), null, "REMAND", 1, null, listOf(charge))
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
    val charge = CreateCharge(UUID.randomUUID(), "OFF123", LocalDate.now(), null, "OUT123")
    val appearance = CreateCourtAppearance(null, UUID.randomUUID(), "OUT123", "COURT1", "COURTREF1", LocalDate.now(), null, "REMAND", 1, null, listOf(charge))
    val secondAppearance = CreateCourtAppearance(null, UUID.randomUUID(), "OUT123", "COURT1", "COURTREF1", LocalDate.now().minusDays(7L), null, "REMAND", 1, null, listOf(charge))
    val courtCase = CreateCourtCase("PRISONER1", listOf(appearance, secondAppearance))
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
    val charge = CreateCharge(UUID.randomUUID(), "OFF123", LocalDate.now(), null, "OUT123")
    val appearance = CreateCourtAppearance(UUID.randomUUID().toString(), UUID.randomUUID(), "OUT123", "COURT1", "GH123456789", LocalDate.now(), null, "REMAND", 1, null, listOf(charge))
    val courtCase = CreateCourtCase("PRI123", listOf(appearance))
    webTestClient
      .put()
      .uri("/court-case/${UUID.randomUUID()}")
      .bodyValue(courtCase)
      .headers {
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `token with incorrect role is forbidden`() {
    val charge = CreateCharge(UUID.randomUUID(), "OFF123", LocalDate.now(), null, "OUT123")
    val appearance = CreateCourtAppearance(UUID.randomUUID().toString(), UUID.randomUUID(), "OUT123", "COURT1", "GH123456789", LocalDate.now(), null, "REMAND", 1, null, listOf(charge))
    val courtCase = CreateCourtCase("PRI123", listOf(appearance))
    webTestClient
      .put()
      .uri("/court-case/${UUID.randomUUID()}")
      .bodyValue(courtCase)
      .headers {
        it.authToken(roles = listOf("ROLE_OTHER_FUNCTION"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isForbidden
  }
}
