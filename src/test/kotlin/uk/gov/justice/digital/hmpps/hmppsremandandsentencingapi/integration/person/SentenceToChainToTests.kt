package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.person

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.SentencesToChainToResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import java.time.format.DateTimeFormatter

class SentenceToChainToTests : IntegrationTestBase() {

  @Test
  fun `returns sentences to chain to grouped by appearance when there is an active sentence in the past`() {
    val (_, createCourtCase) = createCourtCase()
    val appearance = createCourtCase.appearances.first()
    val charge = appearance.charges.first()
    val sentence = charge.sentence!!
    val result = webTestClient.get()
      .uri {
        it.path("/person/{prisonerId}/sentences-to-chain-to")
          .queryParam("beforeOrOnAppearanceDate", appearance.appearanceDate.plusDays(5).format(DateTimeFormatter.ISO_DATE))
          .build(createCourtCase.prisonerId)
      }
      .headers { it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI")) }
      .exchange()
      .expectStatus()
      .isOk
      .returnResult(SentencesToChainToResponse::class.java)
      .responseBody.blockFirst()!!

    Assertions.assertThat(result.appearances).hasSize(1)
    val appearanceToChainTo = result.appearances[0]
    Assertions.assertThat(appearanceToChainTo.appearanceDate).isEqualTo(appearance.appearanceDate)
    Assertions.assertThat(appearanceToChainTo.courtCode).isEqualTo(appearance.courtCode)
    Assertions.assertThat(appearanceToChainTo.courtCaseReference).isEqualTo(appearance.courtCaseReference)
    val sentenceToChainTo = appearanceToChainTo.sentences.first()
    Assertions.assertThat(sentenceToChainTo.sentenceUuid).isEqualTo(sentence.sentenceUuid!!)
    Assertions.assertThat(sentenceToChainTo.offenceCode).isEqualTo(charge.offenceCode)
    Assertions.assertThat(sentenceToChainTo.offenceStartDate).isEqualTo(charge.offenceStartDate)
    Assertions.assertThat(sentenceToChainTo.offenceEndDate).isEqualTo(charge.offenceEndDate)
    Assertions.assertThat(sentenceToChainTo.countNumber).isEqualTo(sentence.chargeNumber)
  }

  @Test
  fun `returns no appearances when there is only an active sentence in the future`() {
    val (_, createCourtCase) = createCourtCase()
    val appearance = createCourtCase.appearances.first()

    val result = webTestClient.get()
      .uri {
        it.path("/person/{prisonerId}/sentences-to-chain-to")
          .queryParam("beforeOrOnAppearanceDate", appearance.appearanceDate.minusDays(5).format(DateTimeFormatter.ISO_DATE))
          .build(createCourtCase.prisonerId)
      }
      .headers { it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI")) }
      .exchange()
      .expectStatus()
      .isOk
      .returnResult(SentencesToChainToResponse::class.java)
      .responseBody.blockFirst()!!

    Assertions.assertThat(result.appearances).isEmpty()
  }

  @Test
  fun `no token results in unauthorized`() {
    val (_, createCourtCase) = createCourtCase()
    val appearance = createCourtCase.appearances.first()
    webTestClient.get()
      .uri {
        it.path("/person/{prisonerId}/sentences-to-chain-to")
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
        it.path("/person/{prisonerId}/sentences-to-chain-to")
          .queryParam("beforeOrOnAppearanceDate", appearance.appearanceDate.plusDays(5).format(DateTimeFormatter.ISO_DATE))
          .build(createCourtCase.prisonerId)
      }
      .headers { it.authToken(roles = listOf("ROLE_OTHER_FUNCTION")) }
      .exchange()
      .expectStatus()
      .isForbidden
  }
}
