package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.sentence

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateCourtCase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.util.DataCreator
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacyCreateSentence
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacySentenceCreatedResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.DpsDataCreator

class MultipleChargesSingleSentenceUpdateTests : IntegrationTestBase() {

  @Test
  fun `update sentence with multiple charges`() {
    val sentenceWithMultipleCharges = createSentenceWithMultipleCharges()
    val updatedLegacyData = sentenceWithMultipleCharges.legacySentence.legacyData.copy(sentenceCalcType = "ADIMP", sentenceCategory = "2020") // DPS SDS sentence type
    val updatedSentence = sentenceWithMultipleCharges.legacySentence.copy(legacyData = updatedLegacyData, fine = null)
    webTestClient
      .put()
      .uri("/legacy/sentence/${sentenceWithMultipleCharges.legacySentenceResponse.lifetimeUuid}")
      .bodyValue(updatedSentence)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_SENTENCE_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isNoContent

    val courtCaseUuid = sentenceWithMultipleCharges.courtCaseUuid
    val (firstCharge, secondCharge, thirdCharge) = sentenceWithMultipleCharges.courtCase.appearances.first().charges

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
      .jsonPath("$.appearances[*].charges[?(@.chargeUuid == '${firstCharge.chargeUuid}')].sentence.sentenceType.sentenceTypeUuid")
      .isEqualTo("02fe3513-40a6-47e9-a72d-9dafdd936a0e")
      .jsonPath("$.appearances[*].charges[?(@.chargeUuid == '${secondCharge.chargeUuid}')].sentence.sentenceType.sentenceTypeUuid")
      .isEqualTo("02fe3513-40a6-47e9-a72d-9dafdd936a0e")
      .jsonPath("$.appearances[*].charges[?(@.chargeUuid == '${thirdCharge.chargeUuid}')].sentence")
      .isEqualTo(null)
  }

  @Test
  fun `update sentence adding another charge to link to`() {
    val sentenceWithMultipleCharges = createSentenceWithMultipleCharges()
    val chargeUuids = sentenceWithMultipleCharges.courtCase.appearances.first().charges.map { it.chargeUuid }
    val updatedSentence = sentenceWithMultipleCharges.legacySentence.copy(chargeUuids = chargeUuids)
    webTestClient
      .put()
      .uri("/legacy/sentence/${sentenceWithMultipleCharges.legacySentenceResponse.lifetimeUuid}")
      .bodyValue(updatedSentence)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_SENTENCE_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isNoContent
    val courtCaseUuid = sentenceWithMultipleCharges.courtCaseUuid
    val (firstCharge, secondCharge, thirdCharge) = sentenceWithMultipleCharges.courtCase.appearances.first().charges
    val sentenceUuid = sentenceWithMultipleCharges.legacySentenceResponse.lifetimeUuid.toString()
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
      .jsonPath("$.appearances[*].charges[*].sentence.sentenceUuid")
      .value<List<String>> { result ->
        Assertions.assertThat(result).contains(sentenceUuid.toString())
        val counts = result.groupingBy { it }.eachCount()
        Assertions.assertThat(counts.values).allMatch { it == 1 }
      }
      .jsonPath("$.appearances[*].charges[?(@.chargeUuid == '${firstCharge.chargeUuid}')].sentence.sentenceUuid")
      .exists()
      .jsonPath("$.appearances[*].charges[?(@.chargeUuid == '${secondCharge.chargeUuid}')].sentence.sentenceUuid")
      .exists()
      .jsonPath("$.appearances[*].charges[?(@.chargeUuid == '${thirdCharge.chargeUuid}')].sentence.sentenceUuid")
      .exists()
  }

  fun createSentenceWithMultipleCharges(): TestData {
    val firstCharge = DpsDataCreator.dpsCreateCharge(sentence = null)
    val secondCharge = DpsDataCreator.dpsCreateCharge(sentence = null)
    val thirdCharge = DpsDataCreator.dpsCreateCharge(sentence = null)
    val appearance = DpsDataCreator.dpsCreateCourtAppearance(charges = listOf(firstCharge, secondCharge, thirdCharge))
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
    return TestData(
      courtCase,
      courtCaseUuid,
      legacySentence,
      response,
    )
  }
}

data class TestData(
  val courtCase: CreateCourtCase,
  val courtCaseUuid: String,
  val legacySentence: LegacyCreateSentence,
  val legacySentenceResponse: LegacySentenceCreatedResponse,
)
