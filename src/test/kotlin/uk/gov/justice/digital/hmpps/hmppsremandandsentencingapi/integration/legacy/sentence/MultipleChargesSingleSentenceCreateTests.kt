package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.sentence

import org.assertj.core.api.Assertions
import org.hamcrest.Matchers.everyItem
import org.hamcrest.core.IsNull
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.util.DataCreator
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacySentenceCreatedResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.DpsDataCreator
import java.util.UUID

class MultipleChargesSingleSentenceCreateTests : IntegrationTestBase() {

  @Test
  fun `create sentence with multiple charges`() {
    val firstCharge = DpsDataCreator.dpsCreateCharge(sentence = null)
    val secondCharge = DpsDataCreator.dpsCreateCharge(sentence = null)
    val appearance = DpsDataCreator.dpsCreateCourtAppearance(charges = listOf(firstCharge, secondCharge))
    val courtCase = DpsDataCreator.dpsCreateCourtCase(appearances = listOf(appearance))
    val (courtCaseUuid) = createCourtCase(courtCase)
    val legacySentence = DataCreator.legacyCreateSentence(chargeUuids = listOf(firstCharge.chargeUuid, secondCharge.chargeUuid), appearanceUuid = appearance.appearanceUuid)
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
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING"), user = "SOME_OTHER_USER")
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.appearances[*].charges[*].sentence.sentenceUuid")
      .value<List<String>> { result ->
        Assertions.assertThat(result).contains(response.lifetimeUuid.toString())
        val counts = result.groupingBy { it }.eachCount()
        Assertions.assertThat(counts.values).allMatch { it == 1 }
        result.forEach {
          Assertions.assertThat(sentenceRepository.findFirstBySentenceUuidOrderByUpdatedAtDesc(UUID.fromString(it))?.updatedBy).isEqualTo("SOME_OTHER_USER")
        }
      }
      .jsonPath("$.appearances[*].charges[?(@.chargeUuid == '${firstCharge.chargeUuid}')].sentence.sentenceUuid")
      .exists()
      .jsonPath("$.appearances[*].charges[?(@.chargeUuid == '${secondCharge.chargeUuid}')].sentence.sentenceUuid")
      .exists()
      .jsonPath("$.appearances[*].charges[*].sentence[?(@.sentenceUuid == '${response.lifetimeUuid}')].legacyData.nomisLineReference")
      .isEqualTo(legacySentence.legacyData.nomisLineReference!!)
      .jsonPath("$.appearances[*].charges[*].sentence[?(@.sentenceUuid != '${response.lifetimeUuid}')].legacyData.nomisLineReference")
      .value(everyItem(IsNull.nullValue()))
  }
}
