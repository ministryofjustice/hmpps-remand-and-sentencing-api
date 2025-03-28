package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.sentencetype

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.core.ParameterizedTypeReference
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.LegacySentenceType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.model.LegacySentenceTypeGroupingSummary
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.model.RecallType

class GetLegacyTypeByLegacySentenceType : IntegrationTestBase() {

  private inline fun <reified T> typeReference() = object : ParameterizedTypeReference<T>() {}

  @Test
  fun `getLegacyAllSentenceTypes returns legacy sentence types`() {
    val result = webTestClient.get()
      .uri("/sentence-type/legacy/all")
      .headers { it.authToken() }
      .exchange()
      .expectStatus().isOk
      .expectBody(typeReference<List<LegacySentenceType>>())
      .returnResult().responseBody!!

    Assertions.assertThat(result).isNotEmpty()
  }

  @Test
  fun `getLegacySentenceType returns legacy sentence types for valid key`() {
    val legacyKey = "ADIMP_ORA"

    val result = webTestClient.get()
      .uri("/sentence-type/legacy/$legacyKey")
      .headers { it.authToken() }
      .exchange()
      .expectStatus().isOk
      .expectBody(typeReference<List<LegacySentenceType>>())
      .returnResult().responseBody!!

    Assertions.assertThat(result).isNotEmpty()
  }

  @Test
  fun `getLegacySentenceType returns empty list for unknown key`() {
    val unknownKey = "MINIMP_ORA"

    val result = webTestClient.get()
      .uri("/sentence-type/legacy/$unknownKey")
      .headers { it.authToken() }
      .exchange()
      .expectStatus().isOk
      .expectBody(typeReference<List<LegacySentenceType>>())
      .returnResult().responseBody!!

    Assertions.assertThat(result).isEmpty()
  }

  @Test
  fun `getGroupedLegacySummaries returns grouped summaries`() {
    val result = webTestClient.get()
      .uri("/sentence-type/legacy/all/summary")
      .headers { it.authToken() }
      .exchange()
      .expectStatus().isOk
      .expectBody(typeReference<List<LegacySentenceTypeGroupingSummary>>())
      .returnResult().responseBody!!

    Assertions.assertThat(result).isNotEmpty
    Assertions.assertThat(result).allSatisfy {
      Assertions.assertThat(it.nomisSentenceTypeReference).isNotBlank()
      Assertions.assertThat(it.nomisDescription).isNotBlank()
    }
  }

  @Test
  fun `getLegacySentenceTypeSummary returns summary for valid key`() {
    val legacyKey = "ADIMP_ORA"

    val result = webTestClient.get()
      .uri("/sentence-type/legacy/$legacyKey/summary")
      .headers { it.authToken() }
      .exchange()
      .expectStatus().isOk
      .expectBody(LegacySentenceTypeGroupingSummary::class.java)
      .returnResult().responseBody!!

    Assertions.assertThat(result.nomisSentenceTypeReference).isEqualTo(legacyKey)
    Assertions.assertThat(result.nomisDescription).isNotBlank()
    Assertions.assertThat(result.isIndeterminate).isFalse()
    assertEquals(result.recall, RecallType.NONE)
  }

  @Test
  fun `getLegacySentenceTypeSummary returns summary for valid key indeterminate`() {
    val legacyKey = "MLP"

    val result = webTestClient.get()
      .uri("/sentence-type/legacy/$legacyKey/summary")
      .headers { it.authToken() }
      .exchange()
      .expectStatus().isOk
      .expectBody(LegacySentenceTypeGroupingSummary::class.java)
      .returnResult().responseBody!!

    Assertions.assertThat(result.nomisSentenceTypeReference).isEqualTo(legacyKey)
    Assertions.assertThat(result.nomisDescription).isNotBlank()
    Assertions.assertThat(result.isIndeterminate).isTrue()
    assertEquals(result.recall, RecallType.NONE)
  }

  @Test
  fun `getLegacySentenceTypeSummary returns summary for valid key recall`() {
    val legacyKey = "LR_ORA"

    val result = webTestClient.get()
      .uri("/sentence-type/legacy/$legacyKey/summary")
      .headers { it.authToken() }
      .exchange()
      .expectStatus().isOk
      .expectBody(LegacySentenceTypeGroupingSummary::class.java)
      .returnResult().responseBody!!

    Assertions.assertThat(result.nomisSentenceTypeReference).isEqualTo(legacyKey)
    Assertions.assertThat(result.nomisDescription).isNotBlank()
    Assertions.assertThat(result.isIndeterminate).isFalse()
    Assertions.assertThat(result.recall).isNotNull()
  }

  @Test
  fun `getLegacySentenceTypeSummary returns summary for valid key recall and indeterminate`() {
    val legacyKey = "LR_ALP"

    val result = webTestClient.get()
      .uri("/sentence-type/legacy/$legacyKey/summary")
      .headers { it.authToken() }
      .exchange()
      .expectStatus().isOk
      .expectBody(LegacySentenceTypeGroupingSummary::class.java)
      .returnResult().responseBody!!

    Assertions.assertThat(result.nomisSentenceTypeReference).isEqualTo(legacyKey)
    Assertions.assertThat(result.nomisDescription).isNotBlank()
    Assertions.assertThat(result.isIndeterminate).isTrue()
    Assertions.assertThat(result.recall).isNotNull
  }
}
