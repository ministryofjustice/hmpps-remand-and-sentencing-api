package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.person

import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.returnResult
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.admin.FixSingleSentenceMultipleChargesPeople
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.util.DataCreator
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.MigrationCreateCourtCasesResponse

class ManyChargesToSentencePrisonersTests : IntegrationTestBase() {

  @Test
  fun `When calling many-charges endpoint for prisoners the many-charges fix is applied`() {
    val firstPrisonerId = "PRI1"
    val firstCreatedCourtCases = createManyChargesSingleSentenceCourtCase(firstPrisonerId)
    val firstSentenceUuid = firstCreatedCourtCases.sentences[0].sentenceUuid
    val firstSentencesForUuid = sentenceRepository.findBySentenceUuid(firstSentenceUuid)
    assertThat(firstSentencesForUuid).hasSize(2)
    val secondPrisonerId = "PRI2"
    val secondCreatedCourtCases = createManyChargesSingleSentenceCourtCase(secondPrisonerId)
    val secondSentenceUuid = secondCreatedCourtCases.sentences[0].sentenceUuid
    val secondSentencesForUuid = sentenceRepository.findBySentenceUuid(secondSentenceUuid)
    assertThat(secondSentencesForUuid).hasSize(2)

    val fixSingleSentenceMultipleChargesPeople = FixSingleSentenceMultipleChargesPeople(listOf(firstPrisonerId, secondPrisonerId))
    webTestClient
      .post()
      .uri("/person-admin/fix-many-charges-to-sentence")
      .bodyValue(fixSingleSentenceMultipleChargesPeople)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isAccepted
    val messages = getMessages(6)
    Assertions.assertThat(messages).extracting<String> { it.eventType }.containsExactlyInAnyOrder("sentence.fix-single-charge.inserted", "sentence.updated", "sentence.period-length.inserted", "sentence.fix-single-charge.inserted", "sentence.updated", "sentence.period-length.inserted")
    val firstSentencesAfter = sentenceRepository.findBySentenceUuid(firstSentenceUuid)
    assertThat(firstSentencesAfter).hasSize(1)
    val secondSentencesAfter = sentenceRepository.findBySentenceUuid(secondSentenceUuid)
    assertThat(secondSentencesAfter).hasSize(1)
  }

  private fun createManyChargesSingleSentenceCourtCase(prisonerId: String): MigrationCreateCourtCasesResponse {
    val sentence = DataCreator.migrationCreateSentence()
    val firstCharge = DataCreator.migrationCreateCharge(chargeNOMISId = 1, sentence = sentence)
    val secondCharge = DataCreator.migrationCreateCharge(chargeNOMISId = 2, sentence = sentence)
    val appearance = DataCreator.migrationCreateCourtAppearance(charges = listOf(firstCharge, secondCharge))
    val courtCase = DataCreator.migrationCreateCourtCase(appearances = listOf(appearance))
    val courtCases = DataCreator.migrationCreateCourtCases(courtCases = listOf(courtCase), prisonerId = prisonerId)
    return webTestClient
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
  }
}
