package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.person

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.returnResult
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.util.DataCreator
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.MigrationCreateCourtCasesResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.DpsDataCreator.Factory.DEFAULT_PRISONER_ID

class ManyChargesToSentenceTests : IntegrationTestBase() {

  @Test
  fun `When calling many-charges endpoint for a prisoner the many-charges fix is applied`() {
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
      .returnResult<MigrationCreateCourtCasesResponse>()
      .responseBody.blockFirst()!!

    val sentenceUuid = response.sentences[0].sentenceUuid
    val sentencesForUuid = sentenceRepository.findBySentenceUuid(sentenceUuid)
    assertThat(sentencesForUuid).hasSize(2)

    webTestClient
      .put()
      .uri("/person/${DEFAULT_PRISONER_ID}/fix-many-charges-to-sentence")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_SENTENCING__RECORD_RECALL_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isNoContent

    val sentencesAfter = sentenceRepository.findBySentenceUuid(sentenceUuid)
    assertThat(sentencesAfter).hasSize(1)
  }
}
