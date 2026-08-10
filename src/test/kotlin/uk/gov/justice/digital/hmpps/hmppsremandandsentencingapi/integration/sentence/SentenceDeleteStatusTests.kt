package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.sentence

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.sentence.delete.DeleteSentenceStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.sentence.delete.DeleteSentenceStatusReason
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.PeriodLengthType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.DpsDataCreator
import java.time.LocalDate
import java.util.UUID

class SentenceDeleteStatusTests : IntegrationTestBase() {

  @Test
  fun `when sentences on other case is after return not supported`() {
    val sentence = DpsDataCreator.dpsCreateSentence()
    val charge = DpsDataCreator.dpsCreateCharge(sentence = sentence)
    val courtAppearance = DpsDataCreator.dpsCreateCourtAppearance(charges = listOf(charge))
    createCourtCase(DpsDataCreator.dpsCreateCourtCase(appearances = listOf(courtAppearance)))
    val otherCaseSentence = DpsDataCreator.dpsCreateSentence(consecutiveToSentenceUuid = sentence.sentenceUuid)
    val otherCaseCharge = DpsDataCreator.dpsCreateCharge(sentence = otherCaseSentence)
    val otherCaseAppearance = DpsDataCreator.dpsCreateCourtAppearance(charges = listOf(otherCaseCharge))
    createCourtCase(DpsDataCreator.dpsCreateCourtCase(appearances = listOf(otherCaseAppearance)))

    webTestClient
      .get()
      .uri("/sentence/${sentence.sentenceUuid}/delete-status")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.status")
      .isEqualTo(DeleteSentenceStatus.NOT_SUPPORTED.name)
      .jsonPath("$.reasons[*].reason")
      .isEqualTo(listOf(DeleteSentenceStatusReason.HAS_SENTENCES_AFTER_ON_OTHER_COURT_APPEARANCE.name))
  }

  @Test
  fun `when sentence on same case other court appearance is after return not supported`() {
    val sentence = DpsDataCreator.dpsCreateSentence()
    val charge = DpsDataCreator.dpsCreateCharge(sentence = sentence)
    val courtAppearance = DpsDataCreator.dpsCreateCourtAppearance(charges = listOf(charge))
    val otherCaseSentence = DpsDataCreator.dpsCreateSentence(consecutiveToSentenceUuid = sentence.sentenceUuid)
    val otherCaseCharge = DpsDataCreator.dpsCreateCharge(sentence = otherCaseSentence)
    val otherCaseAppearance = DpsDataCreator.dpsCreateCourtAppearance(charges = listOf(otherCaseCharge))
    createCourtCase(DpsDataCreator.dpsCreateCourtCase(appearances = listOf(courtAppearance, otherCaseAppearance)))
    webTestClient
      .get()
      .uri("/sentence/${sentence.sentenceUuid}/delete-status")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.status")
      .isEqualTo(DeleteSentenceStatus.NOT_SUPPORTED.name)
      .jsonPath("$.reasons[*].reason")
      .isEqualTo(listOf(DeleteSentenceStatusReason.HAS_SENTENCES_AFTER_ON_OTHER_COURT_APPEARANCE.name))
  }

  @Test
  fun `when sentence on same case same appearance is after return supported`() {
    val sentence = DpsDataCreator.dpsCreateSentence()
    val otherSentence = DpsDataCreator.dpsCreateSentence(consecutiveToSentenceUuid = sentence.sentenceUuid)
    val charge = DpsDataCreator.dpsCreateCharge(sentence = sentence)
    val otherCharge = DpsDataCreator.dpsCreateCharge(sentence = otherSentence)
    val courtAppearance = DpsDataCreator.dpsCreateCourtAppearance(charges = listOf(charge, otherCharge))
    createCourtCase(DpsDataCreator.dpsCreateCourtCase(appearances = listOf(courtAppearance)))
    webTestClient
      .get()
      .uri("/sentence/${sentence.sentenceUuid}/delete-status")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.status")
      .isEqualTo(DeleteSentenceStatus.SUPPORTED.name)
      .jsonPath("$.reasons")
      .isEmpty
  }

  @Test
  fun `when sentence has an appearance period length return not supported`() {
    val sentence = DpsDataCreator.dpsCreateSentence()
    val sentencedCharge = DpsDataCreator.dpsCreateCharge(sentence = sentence)
    val sentencingAppearance = DpsDataCreator.dpsCreateCourtAppearance(appearanceDate = LocalDate.now().minusDays(10), nextCourtAppearance = null, charges = listOf(sentencedCharge))
    val (courtCaseUuid) = createCourtCase(DpsDataCreator.dpsCreateCourtCase(appearances = listOf(sentencingAppearance)))
    val breachPeriodLength = DpsDataCreator.dpsCreatePeriodLength(type = PeriodLengthType.BREACH_OF_SUPERVISION_REQUIREMENTS, days = 41, years = null)
    val breachAppearance = DpsDataCreator.dpsCreateCourtAppearance(
      courtCaseUuid = courtCaseUuid,
      warrantType = "BREACH_OF_SUPERVISION_REQUIREMENTS",
      nextCourtAppearance = null,
      charges = listOf(
        sentencedCharge.copy(sentence = null),
      ),
      periodLengths = listOf(breachPeriodLength),
    )
    putCourtAppearance(breachAppearance.appearanceUuid, breachAppearance)
    webTestClient
      .get()
      .uri("/sentence/${sentence.sentenceUuid}/delete-status")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.status")
      .isEqualTo(DeleteSentenceStatus.NOT_SUPPORTED.name)
      .jsonPath("$.reasons[*].reason")
      .isEqualTo(listOf(DeleteSentenceStatusReason.HAS_APPEARANCE_PERIOD_LENGTH.name))
      .jsonPath("$.reasons[*].metadata.appearanceUuid")
      .isEqualTo(listOf(breachAppearance.appearanceUuid.toString()))
  }

  @Test
  fun `return default supported for sentences which do not exist`() {
    webTestClient
      .get()
      .uri("/sentence/${UUID.randomUUID()}/delete-status")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.status")
      .isEqualTo(DeleteSentenceStatus.SUPPORTED.name)
      .jsonPath("$.reasons")
      .isEmpty
  }

  @Test
  fun `no token results in unauthorized`() {
    val sentence = createCourtCase().second.appearances.first().charges.first().sentence!!
    webTestClient
      .get()
      .uri("/sentence/${sentence.sentenceUuid}/delete-status")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `token with incorrect role is forbidden`() {
    val sentence = createCourtCase().second.appearances.first().charges.first().sentence!!
    webTestClient
      .get()
      .uri("/sentence/${sentence.sentenceUuid}/delete-status")
      .headers {
        it.authToken(roles = listOf("ROLE_OTHER_FUNCTION"))
      }
      .exchange()
      .expectStatus()
      .isForbidden
  }
}
