package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.sentence

import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateCourtCase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.util.DataCreator
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacyCreateFine
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacyCreateSentence
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacyPeriodLengthCreatedResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacySentenceCreatedResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.DpsDataCreator
import java.math.BigDecimal

class MultipleChargesSingleSentenceUpdateTests : IntegrationTestBase() {

  @Test
  fun `update sentence with multiple charges`() {
    val sentenceWithMultipleCharges = createSentenceWithMultipleCharges()
    val updatedSentence = sentenceWithMultipleCharges.legacySentence.copy(fine = LegacyCreateFine(BigDecimal.TEN))
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
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.appearances[*].charges[?(@.chargeUuid == '${firstCharge.chargeUuid}')].sentence.fineAmount.fineAmount")
      .isEqualTo(10.0)
      .jsonPath("$.appearances[*].charges[?(@.chargeUuid == '${secondCharge.chargeUuid}')].sentence.fineAmount.fineAmount")
      .isEqualTo(10.0)
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
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.appearances[*].charges[*].sentence.sentenceUuid")
      .value<List<String>> { result ->
        Assertions.assertThat(result).contains(sentenceUuid)
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

  @Test
  fun `update removing a link to a charge`() {
    val sentenceWithMultipleCharges = createSentenceWithMultipleCharges()
    val legacySentenceChargeUuids = sentenceWithMultipleCharges.legacySentence.chargeUuids.toMutableList()
    val removedChargeUuid = legacySentenceChargeUuids.removeFirst()

    val legacySentenceWithRemovedCharge = sentenceWithMultipleCharges.legacySentence.copy(chargeUuids = legacySentenceChargeUuids)
    webTestClient
      .put()
      .uri("/legacy/sentence/${sentenceWithMultipleCharges.legacySentenceResponse.lifetimeUuid}")
      .bodyValue(legacySentenceWithRemovedCharge)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_SENTENCE_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isNoContent
    val courtCaseUuid = sentenceWithMultipleCharges.courtCaseUuid
    webTestClient
      .get()
      .uri("/court-case/$courtCaseUuid")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.appearances[*].charges[?(@.chargeUuid == '$removedChargeUuid')].sentence.sentenceUuid")
      .doesNotExist()
  }

  @Test
  fun `removing all charges and associating a new charge must keep the period length`() {
    val sentenceWithMultipleCharges = createSentenceWithMultipleCharges()
    val legacyCreatePeriodLength = DataCreator.legacyCreatePeriodLength(sentenceUUID = sentenceWithMultipleCharges.legacySentenceResponse.lifetimeUuid)
    val legacyPeriodLengthCreatedResponse = webTestClient
      .post()
      .uri("/legacy/period-length")
      .bodyValue(legacyCreatePeriodLength)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_PERIOD_LENGTH_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isCreated.returnResult(LegacyPeriodLengthCreatedResponse::class.java)
      .responseBody.blockFirst()!!
    val allChargeUuids = sentenceWithMultipleCharges.courtCase.appearances.first().charges.map { it.chargeUuid }
    val currentChargeUuidsOnSentence = sentenceWithMultipleCharges.legacySentence.chargeUuids
    val chargeUuidsNotOnSentence = allChargeUuids.filter { !currentChargeUuidsOnSentence.contains(it) }
    val legacySentenceWithNewCharge = sentenceWithMultipleCharges.legacySentence.copy(chargeUuids = chargeUuidsNotOnSentence)
    webTestClient
      .put()
      .uri("/legacy/sentence/${sentenceWithMultipleCharges.legacySentenceResponse.lifetimeUuid}")
      .bodyValue(legacySentenceWithNewCharge)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_SENTENCE_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isNoContent

    webTestClient
      .get()
      .uri("/court-case/${sentenceWithMultipleCharges.courtCaseUuid}")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.appearances[*].charges[?(@.chargeUuid == '${chargeUuidsNotOnSentence.first()}')].sentence.periodLengths[?(@.periodLengthUuid == '${legacyPeriodLengthCreatedResponse.periodLengthUuid}')]")
      .exists()
  }

  @Test
  fun `removing charges until sentence is active then updating a period length must be successful`() {
    val sentenceWithMultipleCharges = createSentenceWithMultipleCharges()
    val legacyCreatePeriodLength = DataCreator.legacyCreatePeriodLength(sentenceUUID = sentenceWithMultipleCharges.legacySentenceResponse.lifetimeUuid)
    val legacyPeriodLengthCreatedResponse = webTestClient
      .post()
      .uri("/legacy/period-length")
      .bodyValue(legacyCreatePeriodLength)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_PERIOD_LENGTH_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isCreated.returnResult(LegacyPeriodLengthCreatedResponse::class.java)
      .responseBody.blockFirst()!!
    val singleChargeUuid = sentenceWithMultipleCharges.legacySentence.chargeUuids.first()
    val otherChargeUuids = sentenceWithMultipleCharges.legacySentence.chargeUuids.filter { it != singleChargeUuid }

    val legacySentenceWithNewCharge = sentenceWithMultipleCharges.legacySentence.copy(chargeUuids = listOf(singleChargeUuid))
    webTestClient
      .put()
      .uri("/legacy/sentence/${sentenceWithMultipleCharges.legacySentenceResponse.lifetimeUuid}")
      .bodyValue(legacySentenceWithNewCharge)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_SENTENCE_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isNoContent

    otherChargeUuids.forEach { chargeUuid ->
      webTestClient
        .delete()
        .uri("/legacy/court-appearance/${sentenceWithMultipleCharges.courtCase.appearances.first().appearanceUuid}/charge/$chargeUuid")
        .headers {
          it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_APPEARANCE_RW"))
          it.contentType = MediaType.APPLICATION_JSON
        }
        .exchange()
        .expectStatus()
        .isOk
    }

    val updatedPeriodLength = legacyCreatePeriodLength.copy(periodDays = legacyCreatePeriodLength.periodDays?.plus(10))
    webTestClient
      .put()
      .uri("/legacy/period-length/${legacyPeriodLengthCreatedResponse.periodLengthUuid}")
      .bodyValue(updatedPeriodLength)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_PERIOD_LENGTH_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isNoContent
  }

  fun createSentenceWithMultipleCharges(): TestData {
    val firstCharge = DpsDataCreator.dpsCreateCharge(sentence = null)
    val secondCharge = DpsDataCreator.dpsCreateCharge(sentence = null)
    val thirdCharge = DpsDataCreator.dpsCreateCharge(sentence = null)
    val appearance = DpsDataCreator.dpsCreateCourtAppearance(charges = listOf(firstCharge, secondCharge, thirdCharge))
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

  @Test
  fun `Update sentence - add a charge to an existing sentence (so a many-charges situation), ensure period-lengths are copied to new charge and sentence combination`() {
    val firstCharge = DpsDataCreator.dpsCreateCharge(sentence = null, offenceCode = "OFFENCE1")
    val secondCharge = DpsDataCreator.dpsCreateCharge(sentence = null, offenceCode = "OFFENCE2")
    val appearance = DpsDataCreator.dpsCreateCourtAppearance(charges = listOf(firstCharge, secondCharge))
    val courtCase = DpsDataCreator.dpsCreateCourtCase(appearances = listOf(appearance))
    createCourtCase(courtCase)
    val legacySentence = DataCreator.legacyCreateSentence(chargeUuids = listOf(firstCharge.chargeUuid), appearanceUuid = appearance.appearanceUuid)

    val createdSentence = legacyCreateSentence(legacySentence)
    val sentenceUuid = createdSentence.lifetimeUuid
    val legacyCreatePeriodLength = DataCreator.legacyCreatePeriodLength(sentenceUUID = sentenceUuid, periodDays = 99)
    legacyCreatePeriodLength(legacyCreatePeriodLength)

    val sentencesBefore = sentenceRepository.findBySentenceUuid(sentenceUuid)
    assertThat(sentencesBefore).hasSize(1)
    assertThat(sentencesBefore[0].charge.offenceCode).isEqualTo("OFFENCE1")
    val periodLengthsBefore = periodLengthRepository.findAllBySentenceEntitySentenceUuidAndStatusIdNot(sentenceUuid)
    assertThat(periodLengthsBefore).hasSize(1)
    assertThat(periodLengthsBefore[0].days).isEqualTo(99)

    val updatedSentence = legacySentence.copy(chargeUuids = listOf(firstCharge.chargeUuid, secondCharge.chargeUuid))
    legacyUpdateSentence(sentenceUuid, updatedSentence)

    val sentencesAfter = sentenceRepository.findBySentenceUuid(sentenceUuid)
    assertThat(sentencesAfter).hasSize(2)
    assertThat(sentencesAfter.map { it.charge.offenceCode }).containsExactlyInAnyOrder("OFFENCE1", "OFFENCE2")
    val periodLengthsAfter = periodLengthRepository.findAllBySentenceEntitySentenceUuidAndStatusIdNot(sentenceUuid)
    assertThat(periodLengthsAfter).hasSize(2)
    assertThat(periodLengthsAfter.map { it.days }).containsExactly(99, 99)
    assertThat(periodLengthsAfter.map { it.sentenceEntity?.sentenceUuid }).containsExactly(sentenceUuid, sentenceUuid)
  }

  @Test
  fun `When additional charges are added to a single sentence then the conviction date is retained from the source sentence`() {
    val firstCharge = DpsDataCreator.dpsCreateCharge(sentence = DpsDataCreator.dpsCreateSentence(), offenceCode = "OFFENCE1")
    val secondCharge = DpsDataCreator.dpsCreateCharge(sentence = null, offenceCode = "OFFENCE2")
    val appearance = DpsDataCreator.dpsCreateCourtAppearance(charges = listOf(firstCharge, secondCharge))
    val courtCase = DpsDataCreator.dpsCreateCourtCase(appearances = listOf(appearance))
    val createdCase = createCourtCase(courtCase)
    val createdSentence = createdCase.second.appearances.first().charges.first().sentence!!
    val convictionDate = createdSentence.convictionDate!!

    val legacySentence = DataCreator.legacyCreateSentence(chargeUuids = listOf(firstCharge.chargeUuid, secondCharge.chargeUuid), appearanceUuid = appearance.appearanceUuid)
    val sentenceUuid = createdSentence.sentenceUuid!!

    val sentencesBefore = sentenceRepository.findBySentenceUuid(sentenceUuid)
    assertThat(sentencesBefore).hasSize(1)

    legacyUpdateSentence(sentenceUuid, legacySentence)

    val sentencesAfter = sentenceRepository.findBySentenceUuid(sentenceUuid)
    assertThat(sentencesAfter).hasSize(2)
    assertThat(sentencesAfter.map { it.convictionDate }).containsExactlyInAnyOrder(convictionDate, convictionDate)
  }
}

data class TestData(
  val courtCase: CreateCourtCase,
  val courtCaseUuid: String,
  val legacySentence: LegacyCreateSentence,
  val legacySentenceResponse: LegacySentenceCreatedResponse,
)
