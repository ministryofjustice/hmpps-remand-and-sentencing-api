package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.sentence

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.DpsDataCreator
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.DpsDataCreator.Factory.dpsCreateSentence
import java.time.format.DateTimeFormatter
import java.util.UUID

class GetSentenceTests : IntegrationTestBase() {

  @Test
  fun `get sentence by uuid`() {
    val createdSentence = createCourtCase().second.appearances.first().charges.first().sentence!!
    webTestClient
      .get()
      .uri("/sentence/${createdSentence.sentenceUuid!!}")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING"))
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.sentenceUuid")
      .isEqualTo(createdSentence.sentenceUuid.toString())
      .jsonPath("$.chargeNumber")
      .isEqualTo(createdSentence.chargeNumber)
      .jsonPath("$.sentenceServeType")
      .isEqualTo(createdSentence.sentenceServeType)
      .jsonPath("$.convictionDate")
      .isEqualTo(createdSentence.convictionDate!!.format(DateTimeFormatter.ISO_DATE))
      .jsonPath("$.sentenceType.sentenceTypeUuid")
      .isEqualTo(createdSentence.sentenceTypeId.toString())
  }

  @Test
  fun `get sentences by uuids`() {
    val firstCharge = DpsDataCreator.dpsCreateCharge(sentence = dpsCreateSentence())
    val secondCharge = DpsDataCreator.dpsCreateCharge(sentence = dpsCreateSentence())
    val appearance = DpsDataCreator.dpsCreateCourtAppearance(charges = listOf(firstCharge, secondCharge))
    val (_, courtCase) = createCourtCase(DpsDataCreator.dpsCreateCourtCase(appearances = listOf(appearance)))
    val sentenceOne = courtCase.appearances.first().charges.first().sentence!!
    val sentenceTwo = courtCase.appearances.first().charges[1].sentence!!

    webTestClient
      .get()
      .uri {
        it.path("/sentence")
          .queryParam("sentenceUuids", listOf(sentenceOne.sentenceUuid, sentenceTwo.sentenceUuid))
          .build()
      }
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING"))
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.[0]sentenceUuid")
      .isEqualTo(sentenceOne.sentenceUuid)
      .jsonPath("$.[1]sentenceUuid")
      .isEqualTo(sentenceTwo.sentenceUuid)
  }

  @Test
  fun `no sentence exist for uuid results in not found`() {
    webTestClient
      .get()
      .uri("/sentence/${UUID.randomUUID()}")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING"))
      }
      .exchange()
      .expectStatus()
      .isNotFound
  }

  @Test
  fun `no token results in unauthorized`() {
    val createdSentence = createCourtCase().second.appearances.first().charges.first().sentence!!
    webTestClient
      .get()
      .uri("/sentence/${createdSentence.sentenceUuid!!}")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `token with incorrect role is forbidden`() {
    val createdSentence = createCourtCase().second.appearances.first().charges.first().sentence!!
    webTestClient
      .get()
      .uri("/sentence/${createdSentence.sentenceUuid!!}")
      .headers {
        it.authToken(roles = listOf("ROLE_OTHER_FUNCTION"))
      }
      .exchange()
      .expectStatus()
      .isForbidden
  }
}
