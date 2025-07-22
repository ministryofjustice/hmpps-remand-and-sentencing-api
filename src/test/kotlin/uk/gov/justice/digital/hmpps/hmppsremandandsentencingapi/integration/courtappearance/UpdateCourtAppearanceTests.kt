package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.courtappearance

import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers.everyItem
import org.hamcrest.core.IsNull
import org.hamcrest.text.MatchesPattern
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateCourtAppearanceResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.UploadedDocument
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.event.EventSource
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.event.EventSource.DPS
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.UploadedDocumentEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.DpsDataCreator
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

class UpdateCourtAppearanceTests : IntegrationTestBase() {

  @Test
  fun `update appearance in existing court case`() {
    val courtCase = createCourtCase()
    val createdAppearance = courtCase.second.appearances.first()
    val oldDocumentUuid = UUID.randomUUID()
    val oldDocumentEntity = UploadedDocumentEntity(
      documentUuid = oldDocumentUuid,
      appearance = courtAppearanceRepository.findByAppearanceUuid(createdAppearance.appearanceUuid),
      documentType = "REMAND_WARRANT",
      createdBy = "test-user",
      fileName = "old-document.pdf",
    )
    uploadedDocumentRepository.save(oldDocumentEntity)
    val newDocumentUuid = UUID.randomUUID()
    val newDocumentEntity = UploadedDocumentEntity(
      documentUuid = newDocumentUuid,
      appearance = null,
      documentType = "REMAND_WARRANT",
      createdBy = "test-user",
      fileName = "new-document.pdf",
    )
    uploadedDocumentRepository.save(newDocumentEntity)
    val newUploadedDocument = UploadedDocument(newDocumentUuid, "REMAND_WARRANT", "new-document.pdf")
    val appearanceId = courtAppearanceRepository.findByAppearanceUuid(createdAppearance.appearanceUuid)!!.id
    val appearanceChargeHistoryBefore = appearanceChargeHistoryRepository.findAll().toList().filter { it.appearanceId == appearanceId }
    val updateCourtAppearance = DpsDataCreator.dpsCreateCourtAppearance(courtCaseUuid = courtCase.first, appearanceUUID = createdAppearance.appearanceUuid, courtCaseReference = "ADIFFERENTCOURTCASEREFERENCE", documents = listOf(newUploadedDocument))

    webTestClient
      .put()
      .uri("/court-appearance/${createdAppearance.appearanceUuid}")
      .bodyValue(updateCourtAppearance)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.appearanceUuid")
      .value(MatchesPattern.matchesPattern("([a-f0-9]{8}(-[a-f0-9]{4}){4}[a-f0-9]{8})"))
    val messages = getMessages(7)
    assertThat(messages).hasSize(7).extracting<String> { it.eventType }.contains("court-appearance.updated")
    assertThat(messages).extracting<String> { it.eventType }.contains("sentence.inserted")
    assertThat(messages).extracting<String> { it.eventType }.contains("sentence.period-length.inserted")

    val historyRecords = courtAppearanceHistoryRepository.findAll().filter { it.appearanceUuid == updateCourtAppearance.appearanceUuid }
    assertThat(historyRecords).extracting<String> { it.courtCaseReference!! }.containsExactlyInAnyOrder(createdAppearance.courtCaseReference, updateCourtAppearance.courtCaseReference)
    assertThat(historyRecords).extracting<EventSource> { it.source }.containsOnly(DPS)

    val appearanceChargeHistoryAfter = appearanceChargeHistoryRepository.findAll().toList().filter { it.appearanceId == appearanceId }
    val beforeIds = appearanceChargeHistoryBefore.map { it.id }.toSet()
    val newEntries = appearanceChargeHistoryAfter.filter { it.id !in beforeIds }
    assertEquals(2, newEntries.size)
    assertThat(newEntries).extracting<String> { it.removedBy }.containsExactlyInAnyOrder(null, "SOME_USER")
    assertThat(newEntries).extracting<String> { it.createdBy }.containsExactly("SOME_USER", "SOME_USER")
    assertThat(newEntries).extracting<String> { it.createdPrison }.containsExactlyInAnyOrder("PRISON1", "PRISON1")
    assertThat(newEntries).extracting<String> { it.removedPrison }.containsExactlyInAnyOrder(null, "PRISON1")
    // Assert old document is unlinked
    val oldDoc = uploadedDocumentRepository.findByDocumentUuid(oldDocumentUuid)
    assertThat(oldDoc).isNotNull
    assertThat(oldDoc!!.appearance).isNull()

    // Assert new document is linked
    val newDoc = uploadedDocumentRepository.findByDocumentUuid(newDocumentUuid)
    assertThat(newDoc).isNotNull
    assertThat(newDoc!!.appearance?.appearanceUuid).isEqualTo(createdAppearance.appearanceUuid)
  }

  @Test
  fun `updating only a court appearance keeps the next court appearance`() {
    val courtCase = createCourtCase()
    val createdAppearance = courtCase.second.appearances.first()
    val updateCourtAppearance = DpsDataCreator.dpsCreateCourtAppearance(courtCaseUuid = courtCase.first, appearanceUUID = createdAppearance.appearanceUuid, courtCaseReference = "ADIFFERENTCOURTCASEREFERENCE")
    val response = webTestClient
      .put()
      .uri("/court-appearance/${createdAppearance.appearanceUuid}")
      .bodyValue(updateCourtAppearance)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isOk
      .returnResult(CreateCourtAppearanceResponse::class.java)
      .responseBody.blockFirst()!!
    webTestClient
      .get()
      .uri("/court-appearance/${response.appearanceUuid}")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.nextCourtAppearance")
      .exists()
  }

  @Test
  fun `updating the appearance date results in sentence updated events`() {
    val appearanceDate = LocalDate.now()
    val sentencedAppearance = DpsDataCreator.dpsCreateCourtAppearance(
      outcomeUuid = UUID.fromString("62412083-9892-48c9-bf01-7864af4a8b3c"),
      warrantType = "SENTENCING",
      appearanceDate = appearanceDate,
    )
    val (courtCaseUuid, createdCourtCase) = createCourtCase(DpsDataCreator.dpsCreateCourtCase(appearances = listOf(sentencedAppearance)))
    val createdAppearance = createdCourtCase.appearances.first().copy(courtCaseUuid = courtCaseUuid, appearanceDate = appearanceDate.minusDays(10))
    webTestClient
      .put()
      .uri("/court-appearance/${createdAppearance.appearanceUuid}")
      .bodyValue(createdAppearance)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isOk

    val messages = getMessages(2)
    assertThat(messages).hasSize(2).extracting<String> { it.eventType }.contains("sentence.updated")
  }

  @Test
  fun `updating the next appearance time stores the updated value`() {
    val courtCase = createCourtCase()
    val createdAppearance = courtCase.second.appearances.first()
    val createdNextAppearance = createdAppearance.nextCourtAppearance!!
    val updateNextAppearance = createdNextAppearance.copy(appearanceTime = createdNextAppearance.appearanceTime!!.plusHours(2).withSecond(0).withNano(0))
    val updateCourtAppearance = createdAppearance.copy(courtCaseUuid = courtCase.first, appearanceUuid = createdAppearance.appearanceUuid, nextCourtAppearance = updateNextAppearance)
    val response = webTestClient
      .put()
      .uri("/court-appearance/${createdAppearance.appearanceUuid}")
      .bodyValue(updateCourtAppearance)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isOk
      .returnResult(CreateCourtAppearanceResponse::class.java)
      .responseBody.blockFirst()!!

    webTestClient
      .get()
      .uri("/court-appearance/${response.appearanceUuid}")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.nextCourtAppearance.appearanceTime")
      .isEqualTo(updateNextAppearance.appearanceTime!!.format(DateTimeFormatter.ISO_LOCAL_TIME))
  }

  @Test
  fun `update appearance to edit charge`() {
    val courtCase = createCourtCase()
    val charge = courtCase.second.appearances.first().charges.first().copy(offenceCode = "OFF634624")
    val appearance = courtCase.second.appearances.first().copy(charges = listOf(charge), courtCaseUuid = courtCase.first)
    webTestClient
      .put()
      .uri("/court-appearance/${appearance.appearanceUuid}")
      .bodyValue(appearance)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isOk

    webTestClient
      .get()
      .uri("/court-appearance/${appearance.appearanceUuid}")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING"))
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.charges.[?(@.chargeUuid == '${charge.chargeUuid}')].outcome.outcomeUuid")
      .isEqualTo("68e56c1f-b179-43da-9d00-1272805a7ad3") // replaced with another outcome
      .jsonPath("$.charges.[?(@.chargeUuid == '${charge.chargeUuid}')].sentence")
      .value(everyItem(IsNull.nullValue()))
      .jsonPath("$.charges.[?(@.chargeUuid != '${charge.chargeUuid}')].offenceCode")
      .isEqualTo(charge.offenceCode)
      .jsonPath("$.charges.[?(@.chargeUuid != '${charge.chargeUuid}')].sentence")
      .exists()
  }

  @Test
  fun `update appearance to delete charge`() {
    val courtCase = createCourtCase()
    val charge = DpsDataCreator.dpsCreateCharge()
    val secondCharge = DpsDataCreator.dpsCreateCharge(offenceCode = "OFF567")
    val appearance = courtCase.second.appearances.first().copy(charges = listOf(charge, secondCharge), courtCaseUuid = courtCase.first)
    webTestClient
      .put()
      .uri("/court-appearance/${appearance.appearanceUuid}")
      .bodyValue(appearance)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isOk
    purgeQueues()
    val appearanceWithoutSecondCharge = appearance.copy(charges = listOf(charge))
    webTestClient
      .put()
      .uri("/court-appearance/${appearance.appearanceUuid}")
      .bodyValue(appearanceWithoutSecondCharge)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isOk

    val messages = getMessages(3)
    assertThat(messages).hasSize(3).extracting<String> { it.eventType }.contains("court-appearance.updated")

    webTestClient
      .get()
      .uri("/court-appearance/${appearance.appearanceUuid}")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING"))
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.charges.[?(@.chargeUuid == '${secondCharge.chargeUuid}')]")
      .doesNotExist()
      .jsonPath("$.charges.[?(@.chargeUuid == '${charge.chargeUuid}')]")
      .exists()
  }

  @Test
  fun `updating the appearance changing sentence type`() {
    val sdsUuid = UUID.fromString("1104e683-5467-4340-b961-ff53672c4f39")
    val edsUuid = UUID.fromString("18d5af6d-2fa7-4166-a4c9-8381a1e3c7e0")
    val sentence = DpsDataCreator.dpsCreateSentence(sentenceTypeId = sdsUuid)
    val charge = DpsDataCreator.dpsCreateCharge(sentence = sentence)
    val appearance = DpsDataCreator.dpsCreateCourtAppearance(charges = listOf(charge), overallSentenceLength = DpsDataCreator.dpsCreatePeriodLength(years = 6))
    val (courtCaseUuid, createdCourtCase) = createCourtCase(DpsDataCreator.dpsCreateCourtCase(appearances = listOf(appearance)))

    val createdAppearance = createdCourtCase.appearances.first()
    // Update from SDS sentence to EDS sentence
    val updateAppearance = createdAppearance.copy(courtCaseUuid = courtCaseUuid, charges = createdAppearance.charges.map { it.copy(sentence = it.sentence?.copy(sentenceTypeId = edsUuid)) })
    webTestClient
      .put()
      .uri("/court-appearance/${createdAppearance.appearanceUuid}")
      .bodyValue(updateAppearance)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isOk

    webTestClient
      .get()
      .uri("/court-appearance/${appearance.appearanceUuid}")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING"))
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.charges.[?(@.chargeUuid == '${charge.chargeUuid}')].sentence.sentenceType.sentenceTypeUuid")
      .isEqualTo(edsUuid.toString())
  }

  @Test
  fun `must not update appearance when no court case exists`() {
    val updateCourtAppearance = DpsDataCreator.dpsCreateCourtAppearance(courtCaseUuid = UUID.randomUUID().toString())
    webTestClient
      .put()
      .uri("/court-appearance/${updateCourtAppearance.appearanceUuid}")
      .bodyValue(updateCourtAppearance)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isNotFound
  }

  @Test
  fun `no token results in unauthorized`() {
    val updateCourtAppearance = DpsDataCreator.dpsCreateCourtAppearance()
    webTestClient
      .put()
      .uri("/court-appearance/${updateCourtAppearance.appearanceUuid}")
      .bodyValue(updateCourtAppearance)
      .headers {
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `token with incorrect role is forbidden`() {
    val updateCourtAppearance = DpsDataCreator.dpsCreateCourtAppearance()
    webTestClient
      .put()
      .uri("/court-appearance/${updateCourtAppearance.appearanceUuid}")
      .bodyValue(updateCourtAppearance)
      .headers {
        it.authToken(roles = listOf("ROLE_OTHER_FUNCTION"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isForbidden
  }
}
