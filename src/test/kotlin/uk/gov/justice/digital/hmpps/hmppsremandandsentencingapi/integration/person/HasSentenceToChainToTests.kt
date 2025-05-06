package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.person

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.HasSentenceToChainToResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import java.time.format.DateTimeFormatter

class HasSentenceToChainToTests : IntegrationTestBase() {

  @Test
  fun `returns true when there is an active sentence in the past`() {
    val (_, createCourtCase) = createCourtCase()
    val appearance = createCourtCase.appearances.first()

    val result = webTestClient.get()
      .uri {
        it.path("/person/{prisonerId}/has-sentence-to-chain-to")
          .queryParam("beforeOrOnAppearanceDate", appearance.appearanceDate.plusDays(5).format(DateTimeFormatter.ISO_DATE))
          .build(createCourtCase.prisonerId)
      }
      .headers { it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI")) }
      .exchange()
      .expectStatus()
      .isOk
      .returnResult(HasSentenceToChainToResponse::class.java)
      .responseBody.blockFirst()!!

    Assertions.assertThat(result.hasSentenceToChainTo).isTrue
  }

  @Test
  fun `returns false when there is only an active sentence in the future`() {
    val (_, createCourtCase) = createCourtCase()
    val appearance = createCourtCase.appearances.first()

    val result = webTestClient.get()
      .uri {
        it.path("/person/{prisonerId}/has-sentence-to-chain-to")
          .queryParam("beforeOrOnAppearanceDate", appearance.appearanceDate.minusDays(5).format(DateTimeFormatter.ISO_DATE))
          .build(createCourtCase.prisonerId)
      }
      .headers { it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI")) }
      .exchange()
      .expectStatus()
      .isOk
      .returnResult(HasSentenceToChainToResponse::class.java)
      .responseBody.blockFirst()!!

    Assertions.assertThat(result.hasSentenceToChainTo).isFalse
  }

  @Test
  fun `no token results in unauthorized`() {
    val (_, createCourtCase) = createCourtCase()
    val appearance = createCourtCase.appearances.first()
    webTestClient.get()
      .uri {
        it.path("/person/{prisonerId}/has-sentence-to-chain-to")
          .queryParam("beforeOrOnAppearanceDate", appearance.appearanceDate.plusDays(5).format(DateTimeFormatter.ISO_DATE))
          .build(createCourtCase.prisonerId)
      }
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `token with incorrect role is forbidden`() {
    val (_, createCourtCase) = createCourtCase()
    val appearance = createCourtCase.appearances.first()
    webTestClient.get()
      .uri {
        it.path("/person/{prisonerId}/has-sentence-to-chain-to")
          .queryParam("beforeOrOnAppearanceDate", appearance.appearanceDate.plusDays(5).format(DateTimeFormatter.ISO_DATE))
          .build(createCourtCase.prisonerId)
      }
      .headers { it.authToken(roles = listOf("ROLE_OTHER_FUNCTION")) }
      .exchange()
      .expectStatus()
      .isForbidden
  }
}
