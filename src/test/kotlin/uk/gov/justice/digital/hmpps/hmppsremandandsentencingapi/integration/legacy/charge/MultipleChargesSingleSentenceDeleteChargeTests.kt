package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.charge

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateCourtCase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.util.DataCreator
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.SentenceEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.audit.SentenceHistoryRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacyCreateSentence
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacySentenceCreatedResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.DpsDataCreator
import java.time.LocalDateTime
import java.time.ZoneId

class MultipleChargesSingleSentenceUpdateTests : IntegrationTestBase() {

  @Autowired
  private lateinit var sentenceHistoryRepository: SentenceHistoryRepository

  @Test
  fun `sentence with multiple charges delete charge`() {
    val sentenceWithMultipleCharges = createSentenceWithMultipleCharges()
    val chargeUuid = sentenceWithMultipleCharges.courtCase.appearances.first().charges.first().chargeUuid
    webTestClient
      .delete()
      .uri("/legacy/charge/$chargeUuid")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_CHARGE_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isNoContent

    val latestSentenceHistoryRecord = sentenceHistoryRepository.findAll().filter { it.sentenceUuid == sentenceWithMultipleCharges.legacySentenceResponse.lifetimeUuid }.maxBy {
      it.updatedAt ?: LocalDateTime.MIN.atZone(
        ZoneId.systemDefault(),
      )
    }

    Assertions.assertEquals(SentenceEntityStatus.ACTIVE, latestSentenceHistoryRecord.statusId)
  }

  fun createSentenceWithMultipleCharges(): TestData {
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
