package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.courtappearance

import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.text.MatchesPattern
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CourtAppearance
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateCourtAppearanceResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.Sentence
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.event.EventSource.DPS
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.DpsDataCreator
import java.util.UUID

class CreateCourtAppearanceTests : IntegrationTestBase() {

  @Test
  fun `create appearance in existing court case and link document`() {
    val courtCase = createCourtCase()

    val (uploadedDocument) = uploadDocument()

    val createCourtAppearance = DpsDataCreator.dpsCreateCourtAppearance(courtCaseUuid = courtCase.first, documents = listOf(uploadedDocument))
    webTestClient
      .post()
      .uri("/court-appearance")
      .bodyValue(createCourtAppearance)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isCreated
      .expectBody()
      .jsonPath("$.appearanceUuid")
      .value(MatchesPattern.matchesPattern("([a-f0-9]{8}(-[a-f0-9]{4}){4}[a-f0-9]{8})"))

    val historyRecords = courtAppearanceHistoryRepository.findAll().filter { it.appearanceUuid == createCourtAppearance.appearanceUuid }
    Assertions.assertThat(historyRecords).hasSize(1)
    val historyRecord = historyRecords[0]
    Assertions.assertThat(historyRecord.nextCourtAppearanceId).isNotNull
    assertThat(historyRecord.source).isEqualTo(DPS)

    val linkedDocument = uploadedDocumentRepository.findByDocumentUuid(uploadedDocument.documentUUID)
    assertThat(linkedDocument).isNotNull
    assertThat(linkedDocument!!.appearance?.appearanceUuid).isEqualTo(createCourtAppearance.appearanceUuid)
  }

  @Test
  fun `create appearance with consecutive to sentences`() {
    val courtCase = createCourtCase()
    val forthWithSentence = DpsDataCreator.dpsCreateSentence()
    val forthWithCharge = DpsDataCreator.dpsCreateCharge(sentence = forthWithSentence)
    val concurrentSentence = DpsDataCreator.dpsCreateSentence(chargeNumber = "2", sentenceServeType = "CONCURRENT", sentenceUuid = uuid(0))
    val concurrentCharge = DpsDataCreator.dpsCreateCharge(sentence = concurrentSentence)
    val consecutiveToSentence = DpsDataCreator.dpsCreateSentence(chargeNumber = "3", sentenceServeType = "CONSECUTIVE_TO", sentenceUuid = uuid(1), consecutiveToSentenceUuid = uuid(0))
    val consecutiveToCharge = DpsDataCreator.dpsCreateCharge(sentence = consecutiveToSentence)
    val appearance = DpsDataCreator.dpsCreateCourtAppearance(courtCaseUuid = courtCase.first, charges = listOf(consecutiveToCharge, concurrentCharge, forthWithCharge), overallSentenceLength = DpsDataCreator.dpsCreatePeriodLength(years = 6))
    webTestClient
      .post()
      .uri("/court-appearance")
      .bodyValue(appearance)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isCreated
      .expectBody()
      .jsonPath("$.appearanceUuid")
      .value(MatchesPattern.matchesPattern("([a-f0-9]{8}(-[a-f0-9]{4}){4}[a-f0-9]{8})"))
  }

  @Test
  fun `create charge with different outcome in second appearance keeps the first appearance outcome the same`() {
    val (courtCaseUuid, courtCase) = createCourtCase()
    val appearance = courtCase.appearances.first()
    val charge = appearance.charges.first()
    val oldOutcome = charge.outcomeUuid!! // f17328cf-ceaa-43c2-930a-26cf74480e18
    val newOutcome = UUID.fromString("315280e5-d53e-43b3-8ba6-44da25676ce2")
    val chargeWithNewOutcome = charge.copy(outcomeUuid = newOutcome)
    val newAppearance = DpsDataCreator.dpsCreateCourtAppearance(courtCaseUuid = courtCaseUuid, charges = listOf(chargeWithNewOutcome))
    val newAppearanceResponse = webTestClient
      .post()
      .uri("/court-appearance")
      .bodyValue(newAppearance)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isCreated
      .returnResult(CreateCourtAppearanceResponse::class.java)
      .responseBody.blockFirst()!!

    webTestClient
      .get()
      .uri("/court-appearance/${appearance.appearanceUuid}")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.charges[0].outcome.outcomeUuid")
      .isEqualTo(oldOutcome)

    webTestClient
      .get()
      .uri("/court-appearance/${newAppearanceResponse.appearanceUuid}")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.charges[0].outcome.outcomeUuid")
      .isEqualTo(newOutcome)
  }

  @Test
  fun `update charge with different offence code in second appearance results in charge created + update sentence period length`() {
    val (courtCaseUuid, courtCase) = createCourtCase()
    val appearance = courtCase.appearances.first()
    val charge = appearance.charges.first()
    val sentence = charge.sentence
    val periodLength = sentence?.periodLengths?.first()
    val sentenceWithUpdatedPeriodLength = sentence?.copy(periodLengths = listOf(periodLength!!.copy(days = 997)))
    val chargeWithOffenceCodeAndUpdatedPeriodLength = charge.copy(offenceCode = "OFF634624", sentence = sentenceWithUpdatedPeriodLength)
    val newAppearance = DpsDataCreator.dpsCreateCourtAppearance(courtCaseUuid = courtCaseUuid, charges = listOf(chargeWithOffenceCodeAndUpdatedPeriodLength))
    val newAppearanceResponse = webTestClient
      .post()
      .uri("/court-appearance")
      .bodyValue(newAppearance)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isCreated
      .returnResult(CreateCourtAppearanceResponse::class.java)
      .responseBody.blockFirst()!!

    val messages = getMessages(6)
    Assertions.assertThat(messages).hasSize(6).extracting<String> { it.eventType }.contains("court-appearance.inserted", "charge.inserted", "charge.updated", "sentence.period-length.updated", "sentence.updated")
    Assertions.assertThat(messages).extracting<String> { it.additionalInformation.get("source").asText() }.containsOnly("DPS")

    webTestClient
      .get()
      .uri("/court-appearance/${appearance.appearanceUuid}")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.charges[0].chargeUuid")
      .isEqualTo(charge.chargeUuid)

    webTestClient
      .get()
      .uri("/court-appearance/${newAppearanceResponse.appearanceUuid}")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.charges[?(@.chargeUuid == '${charge.chargeUuid}')].offenceCode")
      .isEqualTo(charge.offenceCode)
      .jsonPath("$.charges[?(@.chargeUuid == '${charge.chargeUuid}')].outcome.outcomeUuid")
      .isEqualTo("68e56c1f-b179-43da-9d00-1272805a7ad3") // replaced by another outcome
      .jsonPath("$.charges[?(@.chargeUuid != '${charge.chargeUuid}')].offenceCode")
      .isEqualTo(chargeWithOffenceCodeAndUpdatedPeriodLength.offenceCode)
  }

  @Test
  fun `Delete period length from a sentence and replace with a new one`() {
    val (courtCaseUuid, courtCase) = createCourtCase()
    val charge = courtCase.appearances.first().charges.first()
    val sentence = charge.sentence
    val sentenceWithNewPeriodLength = sentence?.copy(periodLengths = listOf(DpsDataCreator.dpsCreatePeriodLength()))
    val chargeWithNewPeriodLength = charge.copy(sentence = sentenceWithNewPeriodLength)
    val newAppearance = DpsDataCreator.dpsCreateCourtAppearance(courtCaseUuid = courtCaseUuid, charges = listOf(chargeWithNewPeriodLength))
    webTestClient
      .post()
      .uri("/court-appearance")
      .bodyValue(newAppearance)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isCreated
      .returnResult(CreateCourtAppearanceResponse::class.java)
      .responseBody.blockFirst()!!

    val messages = getMessages(4)
    Assertions.assertThat(messages).hasSize(4).extracting<String> { it.eventType }.contains("sentence.period-length.inserted", "sentence.period-length.deleted")
    Assertions.assertThat(messages).extracting<String> { it.additionalInformation.get("source").asText() }.containsOnly("DPS")
  }

  @Test
  fun `must not create appearance when no court case exists`() {
    val createCourtAppearance = DpsDataCreator.dpsCreateCourtAppearance(courtCaseUuid = UUID.randomUUID().toString())
    webTestClient
      .post()
      .uri("/court-appearance")
      .bodyValue(createCourtAppearance)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isNotFound
  }

  @Test
  fun `no token results in unauthorized`() {
    val createCourtAppearance = DpsDataCreator.dpsCreateCourtAppearance()
    webTestClient
      .post()
      .uri("/court-appearance")
      .bodyValue(createCourtAppearance)
      .headers {
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `token with incorrect role is forbidden`() {
    val createCourtAppearance = DpsDataCreator.dpsCreateCourtAppearance()
    webTestClient
      .post()
      .uri("/court-appearance")
      .bodyValue(createCourtAppearance)
      .headers {
        it.authToken(roles = listOf("ROLE_OTHER_FUNCTION"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `Create court appearance and ensure consecutive to relationships are correctly inserted`() {
    val s1 = DpsDataCreator.dpsCreateSentence(
      chargeNumber = "1",
      sentenceServeType = "FORTHWITH",
      sentenceUuid = uuid(0),
    )

    val s2 = DpsDataCreator.dpsCreateSentence(
      chargeNumber = "2",
      sentenceServeType = "CONSECUTIVE",
      sentenceUuid = uuid(1),
      consecutiveToSentenceUuid = uuid(0),
    )

    val s3 = DpsDataCreator.dpsCreateSentence(
      chargeNumber = "3",
      sentenceServeType = "CONSECUTIVE",
      sentenceUuid = uuid(2),
      consecutiveToSentenceUuid = uuid(1),
    )

    val s4 = DpsDataCreator.dpsCreateSentence(
      chargeNumber = "4",
      sentenceServeType = "CONSECUTIVE",
      sentenceUuid = uuid(3),
      consecutiveToSentenceUuid = uuid(2),
    )

    val c1 = DpsDataCreator.dpsCreateCharge(sentence = s1)
    val c2 = DpsDataCreator.dpsCreateCharge(sentence = s2)
    val c3 = DpsDataCreator.dpsCreateCharge(sentence = s3)
    val c4 = DpsDataCreator.dpsCreateCharge(sentence = s4)

    val appearance = DpsDataCreator.dpsCreateCourtAppearance(charges = listOf(c4, c3, c2, c1))

    val courtCase = createCourtCase(DpsDataCreator.dpsCreateCourtCase(appearances = listOf(appearance)))

    val createdCourtAppearance: CourtAppearance =
      webTestClient
        .get()
        .uri("/court-appearance/${courtCase.second.appearances[0].appearanceUuid}")
        .headers {
          it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
        }
        .exchange()
        .expectStatus().isOk
        .expectBody(CourtAppearance::class.java)
        .returnResult()
        .responseBody!!

    val sentences: List<Sentence> = createdCourtAppearance.charges.map { it.sentence!! }.sortedBy { it.chargeNumber!!.toInt() }

    assertThat(sentences[0].chargeNumber).isEqualTo("1")
    assertThat(sentences[1].chargeNumber).isEqualTo("2")
    assertThat(sentences[2].chargeNumber).isEqualTo("3")
    assertThat(sentences[3].chargeNumber).isEqualTo("4")

    assertThat(sentences[3].consecutiveToSentenceUuid).isEqualTo(sentences[2].sentenceUuid)
    assertThat(sentences[2].consecutiveToSentenceUuid).isEqualTo(sentences[1].sentenceUuid)
    assertThat(sentences[1].consecutiveToSentenceUuid).isEqualTo(sentences[0].sentenceUuid)
    assertThat(sentences[0].consecutiveToSentenceUuid).isNull()
  }
}
