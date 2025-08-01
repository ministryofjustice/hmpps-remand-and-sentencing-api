package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.courtappearance

import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.text.MatchesPattern
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateCourtAppearanceResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.event.EventSource.DPS
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.DpsDataCreator
import java.util.UUID

class CreateCourtAppearanceTests : IntegrationTestBase() {

  @Test
  fun `create appearance in existing court case and link document`() {
    val courtCase = createCourtCase()
    val documentUuid = UUID.randomUUID()

    val uploadedDocument = DpsDataCreator.dpsCreateUploadedDocument(
      documentUuid = documentUuid,
      documentType = "REMAND_WARRANT",
      documentName = "court-appearance-document.pdf",
    )
    uploadDocument(uploadedDocument)

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

    val linkedDocument = uploadedDocumentRepository.findByDocumentUuid(documentUuid)
    assertThat(linkedDocument).isNotNull
    assertThat(linkedDocument!!.appearance?.appearanceUuid).isEqualTo(createCourtAppearance.appearanceUuid)
  }

  @Test
  fun `create appearance with consecutive to sentences`() {
    val courtCase = createCourtCase()
    val forthWithSentence = DpsDataCreator.dpsCreateSentence()
    val forthWithCharge = DpsDataCreator.dpsCreateCharge(sentence = forthWithSentence)
    val concurrentSentence = DpsDataCreator.dpsCreateSentence(chargeNumber = "2", sentenceServeType = "CONCURRENT", sentenceReference = "0")
    val concurrentCharge = DpsDataCreator.dpsCreateCharge(sentence = concurrentSentence)
    val consecutiveToSentence = DpsDataCreator.dpsCreateSentence(chargeNumber = "3", sentenceServeType = "CONSECUTIVE_TO", sentenceReference = "1", consecutiveToSentenceReference = "0")
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
}
