package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.courtappearance

import org.assertj.core.api.Assertions
import org.hamcrest.core.IsNull
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityStatus
import java.util.UUID

class LegacyDeleteCourtAppearanceTests : IntegrationTestBase() {

  @Test
  fun `can delete court appearance`() {
    val (lifetimeUuid) = createLegacyCourtAppearance()
    webTestClient
      .delete()
      .uri("/legacy/court-appearance/$lifetimeUuid")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_APPEARANCE_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isOk
    val message = getMessages(1)[0]
    Assertions.assertThat(message.eventType).isEqualTo("court-appearance.deleted")
    Assertions.assertThat(message.additionalInformation.get("source").asText()).isEqualTo("NOMIS")
  }

  @Test
  fun `deleting a future appearance removes the next court appearance of original appearance`() {
    val (courtCaseUuid, courtCase) = createCourtCase()
    val originalAppearance = courtCase.appearances.first()
    val futureAppearance = courtAppearanceRepository.findByCourtCaseCaseUniqueIdentifierAndStatusId(
      courtCaseUuid,
      EntityStatus.FUTURE,
    )
    webTestClient
      .delete()
      .uri("/legacy/court-appearance/${futureAppearance.appearanceUuid}")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_APPEARANCE_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isOk

    webTestClient
      .get()
      .uri("/court-appearance/${originalAppearance.appearanceUuid!!}")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.nextCourtAppearance")
      .value(IsNull.nullValue())
  }

  @Test
  fun `deleting appearance deletes all links to charges`() {
    val (courtCaseUuid, courtCase) = createCourtCase()
    val originalAppearance = courtCase.appearances.first()
    webTestClient
      .delete()
      .uri("/legacy/court-appearance/${originalAppearance.appearanceUuid}")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_APPEARANCE_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isOk
    Assertions.assertThat(appearanceChargeRepository.countByAppearanceAppearanceUuid(originalAppearance.appearanceUuid)).isEqualTo(0)
  }

  @Test
  fun `no token results in unauthorized`() {
    webTestClient
      .delete()
      .uri("/legacy/court-appearance/${UUID.randomUUID()}")
      .headers {
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `token with incorrect role is forbidden`() {
    webTestClient
      .delete()
      .uri("/legacy/court-appearance/${UUID.randomUUID()}")
      .headers {
        it.authToken(roles = listOf("ROLE_OTHER_FUNCTION"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isForbidden
  }
}
