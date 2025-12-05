package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.sync

import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.util.DataCreator
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.MigrationCreateCourtCasesResponse

class ManyChargesConsecutiveToTests : IntegrationTestBase() {

  @Test
  fun `going from many charges to one charge on sentence then creating a sentence consecutive to`() {
    val sentence = DataCreator.migrationCreateSentence()
    val firstCharge = DataCreator.migrationCreateCharge(chargeNOMISId = 1, sentence = sentence)
    val secondCharge = DataCreator.migrationCreateCharge(chargeNOMISId = 2, sentence = sentence)
    val appearance = DataCreator.migrationCreateCourtAppearance(charges = listOf(firstCharge, secondCharge))
    val courtCase = DataCreator.migrationCreateCourtCase(appearances = listOf(appearance))
    val courtCases = DataCreator.migrationCreateCourtCases(courtCases = listOf(courtCase))
    val response = webTestClient
      .post()
      .uri("/legacy/court-case/migration")
      .bodyValue(courtCases)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_COURT_CASE_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isCreated
      .returnResult(MigrationCreateCourtCasesResponse::class.java)
      .responseBody.blockFirst()!!

    val sentenceUuid = response.sentences.first().sentenceUuid
    val firstChargeUuid = response.charges.first { it.chargeNOMISId == firstCharge.chargeNOMISId }.chargeUuid
    val secondChargeUuid = response.charges.first { it.chargeNOMISId == secondCharge.chargeNOMISId }.chargeUuid
    val appearanceUuid = response.appearances.first().appearanceUuid

    val updateSentence = DataCreator.legacyCreateSentence(chargeUuids = listOf(firstChargeUuid), appearanceUuid = appearanceUuid)
    webTestClient
      .put()
      .uri("/legacy/sentence/$sentenceUuid")
      .bodyValue(updateSentence)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_SENTENCE_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isNoContent

    val newSentence = DataCreator.legacyCreateSentence(chargeUuids = listOf(secondChargeUuid), appearanceUuid = appearanceUuid, consecutiveToLifetimeUuid = sentenceUuid)
    webTestClient
      .post()
      .uri("/legacy/sentence")
      .bodyValue(newSentence)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_SENTENCE_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isCreated
  }
}
