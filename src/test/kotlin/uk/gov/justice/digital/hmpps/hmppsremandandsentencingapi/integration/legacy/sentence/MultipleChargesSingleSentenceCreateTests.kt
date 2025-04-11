package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.sentence

import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.util.DataCreator
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacySentenceCreatedResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.DpsDataCreator

class MultipleChargesSingleSentenceCreateTests : IntegrationTestBase() {

  @Test
  fun `create sentence with multiple charges`() {
    val firstCharge = DpsDataCreator.dpsCreateCharge(sentence = null)
    val secondCharge = DpsDataCreator.dpsCreateCharge(sentence = null)
    val appearance = DpsDataCreator.dpsCreateCourtAppearance(charges = listOf(firstCharge, secondCharge))
    val courtCase = DpsDataCreator.dpsCreateCourtCase(appearances = listOf(appearance))
    val (courtCaseUuid) = createCourtCase(courtCase)
    val legacySentence = DataCreator.legacyCreateSentence(chargeUuids = listOf(firstCharge.chargeUuid, secondCharge.chargeUuid))
    val response = webTestClient
      .post()
      .uri("/legacy/sentence")
      .bodyValue(legacySentence)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_SENTENCE_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isCreated.returnResult(LegacySentenceCreatedResponse::class.java)
      .responseBody.blockFirst()!!

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
      .jsonPath("$.appearances[*].charges[?(@.chargeUuid == '${firstCharge.chargeUuid}')].sentence.sentenceUuid")
      .isEqualTo(response.lifetimeUuid.toString())
      .jsonPath("$.appearances[*].charges[?(@.chargeUuid == '${secondCharge.chargeUuid}')].sentence.sentenceUuid")
      .isEqualTo(response.lifetimeUuid.toString())
  }
}
