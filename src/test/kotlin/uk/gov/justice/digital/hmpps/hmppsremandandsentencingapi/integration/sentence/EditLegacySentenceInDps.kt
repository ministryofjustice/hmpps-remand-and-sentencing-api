package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.sentence

import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.util.DataCreator
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacySentenceCreatedResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.DpsDataCreator

class EditLegacySentenceInDps : IntegrationTestBase() {

  @Test
  fun `can edit legacy sentence in DPS`() {
    val charge = DpsDataCreator.dpsCreateCharge(sentence = null)
    val appearance = DpsDataCreator.dpsCreateCourtAppearance(charges = listOf(charge))
    val (courtCaseUuid) = createCourtCase(DpsDataCreator.dpsCreateCourtCase(appearances = listOf(appearance)))
    val legacySentence = DataCreator.legacyCreateSentence(chargeUuids = listOf(charge.chargeUuid), appearanceUuid = appearance.appearanceUuid)
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

    val dpsEdit = DpsDataCreator.dpsCreateSentence(sentenceUuid = response.lifetimeUuid, sentenceTypeId = null)
    val sentencedCharge = charge.copy(sentence = dpsEdit)
    val sentencedAppearance = appearance.copy(charges = listOf(sentencedCharge), courtCaseUuid = courtCaseUuid)
    webTestClient
      .put()
      .uri("/court-appearance/${sentencedAppearance.appearanceUuid}")
      .bodyValue(sentencedAppearance)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isOk
  }
}
