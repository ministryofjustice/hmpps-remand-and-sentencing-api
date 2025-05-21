package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.sentence

import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateCourtCase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.util.DataCreator
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.PeriodLengthRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.SentenceRepository
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacyCreateSentence
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacySentenceCreatedResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.DpsDataCreator

class MultipleChargesSingleSentenceUpdateTests : IntegrationTestBase() {

  @Autowired
  private lateinit var periodLengthRepository: PeriodLengthRepository

  @Autowired
  private lateinit var sentenceRepository: SentenceRepository

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

  @Test
  fun `Update sentence - add a charge to an existing sentence (so a many-charges situation), ensure period-lengths are copied to new charge and sentence combination`() {
    val firstCharge = DpsDataCreator.dpsCreateCharge(sentence = null, offenceCode = "OFFENCE1")
    val secondCharge = DpsDataCreator.dpsCreateCharge(sentence = null, offenceCode = "OFFENCE2")
    val appearance = DpsDataCreator.dpsCreateCourtAppearance(charges = listOf(firstCharge, secondCharge))
    val courtCase = DpsDataCreator.dpsCreateCourtCase(appearances = listOf(appearance))
    createCourtCase(courtCase)
    val legacySentence = DataCreator.legacyCreateSentence(chargeUuids = listOf(firstCharge.chargeUuid))

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
}

data class TestData(
  val courtCase: CreateCourtCase,
  val courtCaseUuid: String,
  val legacySentence: LegacyCreateSentence,
  val legacySentenceResponse: LegacySentenceCreatedResponse,
)
