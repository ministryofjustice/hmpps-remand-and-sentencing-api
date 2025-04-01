package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.sentenceType

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.core.ParameterizedTypeReference
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacySentenceType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.model.LegacySentenceTypeGroupingSummary
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.model.RecallTypeIdentifier
import java.time.LocalDate

class GetLegacyTypeByLegacySentenceType : IntegrationTestBase() {

  private inline fun <reified T> typeReference() = object : ParameterizedTypeReference<T>() {}

  @Test
  fun `getLegacyAllSentenceTypes is sorted by nomisActive desc, nomisExpiryDate desc, sentencingAct asc`() {
    val result = webTestClient.get()
      .uri("/legacy/sentence-type/all")
      .headers { it.authToken() }
      .exchange()
      .expectStatus().isOk
      .expectBody(typeReference<List<LegacySentenceType>>())
      .returnResult().responseBody!!

    data class SortKey(
      val nomisActive: Boolean,
      val nomisExpiryDate: LocalDate?,
      val sentencingAct: String,
    )

    result.forEach {
      check(
        (it.nomisActive && it.nomisExpiryDate == null) ||
          (!it.nomisActive && it.nomisExpiryDate != null),
      ) {
        "Invalid state: active=${it.nomisActive} but expiryDate=${it.nomisExpiryDate} - $it"
      }
    }
    val sortKeys = result.map {
      SortKey(
        nomisActive = it.nomisActive,
        nomisExpiryDate = it.nomisExpiryDate ?: LocalDate.MIN,
        sentencingAct = it.sentencingAct.toString(),
      )
    }

    val expected = sortKeys.sortedWith(
      compareByDescending<SortKey> { it.nomisActive }
        .thenByDescending { it.nomisExpiryDate } // only applies if nomisActive == false
        .thenBy { it.sentencingAct },
    )

    assertEquals(expected, sortKeys, "Legacy sentence types are not sorted as expected")
  }

  @Test
  fun `getLegacySentenceType returns legacy sentence types for valid key`() {
    val legacyKey = "ADIMP_ORA"

    val result = webTestClient.get()
      .uri("/legacy/sentence-type/?nomisSentenceTypeReference={nomisSentenceTypeReference}", legacyKey)
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
      .uri("/legacy/sentence-type/?nomisSentenceTypeReference={nomisSentenceTypeReference}", unknownKey)
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
      .uri("/legacy/sentence-type/all/summary")
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
      .uri("/legacy/sentence-type/summary?nomisSentenceTypeReference={nomisSentenceTypeReference}", legacyKey)
      .headers { it.authToken() }
      .exchange()
      .expectStatus().isOk
      .expectBody(LegacySentenceTypeGroupingSummary::class.java)
      .returnResult().responseBody!!

    Assertions.assertThat(result.nomisSentenceTypeReference).isEqualTo(legacyKey)
    Assertions.assertThat(result.nomisDescription).isNotBlank()
    Assertions.assertThat(result.isIndeterminate).isFalse()
    assertEquals(result.recall, RecallTypeIdentifier.NONE)
  }

  @Test
  fun `getLegacySentenceTypeSummary returns summary for valid key indeterminate`() {
    val legacyKey = "MLP"

    val result = webTestClient.get()
      .uri("/legacy/sentence-type/summary?nomisSentenceTypeReference={nomisSentenceTypeReference}", legacyKey)
      .headers { it.authToken() }
      .exchange()
      .expectStatus().isOk
      .expectBody(LegacySentenceTypeGroupingSummary::class.java)
      .returnResult().responseBody!!

    Assertions.assertThat(result.nomisSentenceTypeReference).isEqualTo(legacyKey)
    Assertions.assertThat(result.nomisDescription).isNotBlank()
    Assertions.assertThat(result.isIndeterminate).isTrue()
    assertEquals(result.recall, RecallTypeIdentifier.NONE.toDomain())
  }

  @Test
  fun `getLegacySentenceTypeSummary returns summary for valid key recall`() {
    val legacyKey = "LR_ORA"

    val result = webTestClient.get()
      .uri("/legacy/sentence-type/summary?nomisSentenceTypeReference={nomisSentenceTypeReference}", legacyKey)
      .headers { it.authToken() }
      .exchange()
      .expectStatus().isOk
      .expectBody(LegacySentenceTypeGroupingSummary::class.java)
      .returnResult().responseBody!!

    Assertions.assertThat(result.nomisSentenceTypeReference).isEqualTo(legacyKey)
    Assertions.assertThat(result.nomisDescription).isNotBlank()
    Assertions.assertThat(result.isIndeterminate).isFalse()
    assertEquals(result.recall, RecallTypeIdentifier.STANDARD_RECALL.toDomain())
  }

  @Test
  fun `getLegacySentenceTypeSummary returns summary for valid key recall and indeterminate`() {
    val legacyKey = "LR_ALP"

    val result = webTestClient.get()
      .uri("/legacy/sentence-type/summary?nomisSentenceTypeReference={nomisSentenceTypeReference}", legacyKey)
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
