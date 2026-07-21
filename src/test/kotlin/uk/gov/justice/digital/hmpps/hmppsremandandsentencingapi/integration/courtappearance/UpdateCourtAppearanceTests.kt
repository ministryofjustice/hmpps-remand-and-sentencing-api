package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.courtappearance

import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers.everyItem
import org.hamcrest.core.IsNull
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.AggravatingFactor
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CourtCase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateCourtAppearance
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateCourtAppearanceResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.CreateCourtCase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.event.EventSource
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.event.EventSource.DPS
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.event.HmppsPeriodLengthMessage
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.util.DataCreator
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.wiremock.DocumentManagementApiExtension.Companion.documentManagementApi
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.CourtAppearanceEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.PeriodLengthType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.SentenceEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.MigrationCreateCourtCasesResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.ChargeService
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.DpsDataCreator
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.DpsDataCreator.Factory.dpsCreateCourtAppearance
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

class UpdateCourtAppearanceTests : IntegrationTestBase() {

  @Autowired
  private lateinit var jdbcTemplate: NamedParameterJdbcTemplate
  private val aggravatingFactors by lazy { ChargeAggravatingFactorHelper(jdbcTemplate) }

  @Test
  fun `update appearance in existing court case`() {
    val (oldDocument) = uploadDocument()
    documentManagementApi.stubUpdateDocumentStatus(oldDocument.documentUUID.toString())

    val appearance = dpsCreateCourtAppearance(documents = listOf(oldDocument))
    val courtCase = createCourtCase(DpsDataCreator.dpsCreateCourtCase(appearances = listOf(appearance)))
    val createdAppearance = courtCase.second.appearances.first()
    getCourtCase(courtCase.first)

    val (newDocument) = uploadDocument()
    documentManagementApi.stubUpdateDocumentStatus(newDocument.documentUUID.toString())

    val appearanceId = courtAppearanceRepository.findByAppearanceUuid(createdAppearance.appearanceUuid)!!.id
    val appearanceChargeHistoryBefore =
      appearanceChargeHistoryRepository.findAll().toList().filter { it.appearanceId == appearanceId }
    val updateCourtAppearance = dpsCreateCourtAppearance(
      courtCaseUuid = courtCase.first,
      appearanceUUID = createdAppearance.appearanceUuid,
      courtCaseReference = "ADIFFERENTCOURTCASEREFERENCE",
      documents = listOf(newDocument),
    )

    putCourtAppearance(createdAppearance.appearanceUuid, updateCourtAppearance)

    val messages = getMessages(8)
    assertThat(messages).hasSize(8).extracting<String> { it.eventType }.contains("court-appearance.updated")
    assertThat(messages).extracting<String> { it.eventType }.contains("sentence.inserted")
    assertThat(messages).extracting<String> { it.eventType }.contains("sentence.period-length.inserted")

    val historyRecords =
      courtAppearanceHistoryRepository.findAll().filter { it.appearanceUuid == updateCourtAppearance.appearanceUuid }
    assertThat(historyRecords).extracting<String> { it.courtCaseReference!! }
      .containsExactlyInAnyOrder(createdAppearance.courtCaseReference, updateCourtAppearance.courtCaseReference)
    assertThat(historyRecords).extracting<EventSource> { it.source }.containsOnly(DPS)

    val appearanceChargeHistoryAfter =
      appearanceChargeHistoryRepository.findAll().toList().filter { it.appearanceId == appearanceId }
    val beforeIds = appearanceChargeHistoryBefore.map { it.id }.toSet()
    val newEntries = appearanceChargeHistoryAfter.filter { it.id !in beforeIds }
    assertEquals(2, newEntries.size)
    assertThat(newEntries).extracting<String> { it.removedBy }.containsExactlyInAnyOrder(null, "SOME_USER")
    assertThat(newEntries).extracting<String> { it.createdBy }.containsExactly("SOME_USER", "SOME_USER")
    assertThat(newEntries).extracting<String> { it.createdPrison }.containsExactlyInAnyOrder("PRISON1", "PRISON1")
    assertThat(newEntries).extracting<String> { it.removedPrison }.containsExactlyInAnyOrder(null, "PRISON1")

    val oldDoc = uploadedDocumentRepository.findByDocumentUuid(oldDocument.documentUUID)
    assertThat(oldDoc).isNotNull
    assertThat(oldDoc!!.appearance).isNull()

    verifyDocumentMetadataUpdated(oldDocument.documentUUID, "DELETED")

    val newDoc = uploadedDocumentRepository.findByDocumentUuid(newDocument.documentUUID)
    assertThat(newDoc).isNotNull
    assertThat(newDoc!!.appearance?.appearanceUuid).isEqualTo(createdAppearance.appearanceUuid)

    verifyDocumentMetadataUpdated(newDocument.documentUUID, "ACTIVE")
  }

  @Test
  fun `still update appearance in existing court case even if dm api fails`() {
    val (oldDocument) = uploadDocument()
    documentManagementApi.stubUpdateDocumentStatus(oldDocument.documentUUID.toString())

    val appearance = dpsCreateCourtAppearance(documents = listOf(oldDocument))
    val courtCase = createCourtCase(DpsDataCreator.dpsCreateCourtCase(appearances = listOf(appearance)))
    val createdAppearance = courtCase.second.appearances.first()
    getCourtCase(courtCase.first)

    val (newDocument) = uploadDocument()
    documentManagementApi.stubUpdateDocumentStatusToFail(newDocument.documentUUID.toString())

    val appearanceId = courtAppearanceRepository.findByAppearanceUuid(createdAppearance.appearanceUuid)!!.id
    val appearanceChargeHistoryBefore =
      appearanceChargeHistoryRepository.findAll().toList().filter { it.appearanceId == appearanceId }
    val updateCourtAppearance = dpsCreateCourtAppearance(
      courtCaseUuid = courtCase.first,
      appearanceUUID = createdAppearance.appearanceUuid,
      courtCaseReference = "ADIFFERENTCOURTCASEREFERENCE",
      documents = listOf(newDocument),
    )

    putCourtAppearance(createdAppearance.appearanceUuid, updateCourtAppearance)

    val messages = getMessages(8)
    assertThat(messages).hasSize(8).extracting<String> { it.eventType }.contains("court-appearance.updated")
    assertThat(messages).extracting<String> { it.eventType }.contains("sentence.inserted")
    assertThat(messages).extracting<String> { it.eventType }.contains("sentence.period-length.inserted")

    val historyRecords =
      courtAppearanceHistoryRepository.findAll().filter { it.appearanceUuid == updateCourtAppearance.appearanceUuid }
    assertThat(historyRecords).extracting<String> { it.courtCaseReference!! }
      .containsExactlyInAnyOrder(createdAppearance.courtCaseReference, updateCourtAppearance.courtCaseReference)
    assertThat(historyRecords).extracting<EventSource> { it.source }.containsOnly(DPS)

    val appearanceChargeHistoryAfter =
      appearanceChargeHistoryRepository.findAll().toList().filter { it.appearanceId == appearanceId }
    val beforeIds = appearanceChargeHistoryBefore.map { it.id }.toSet()
    val newEntries = appearanceChargeHistoryAfter.filter { it.id !in beforeIds }
    assertEquals(2, newEntries.size)
    assertThat(newEntries).extracting<String> { it.removedBy }.containsExactlyInAnyOrder(null, "SOME_USER")
    assertThat(newEntries).extracting<String> { it.createdBy }.containsExactly("SOME_USER", "SOME_USER")
    assertThat(newEntries).extracting<String> { it.createdPrison }.containsExactlyInAnyOrder("PRISON1", "PRISON1")
    assertThat(newEntries).extracting<String> { it.removedPrison }.containsExactlyInAnyOrder(null, "PRISON1")

    val oldDoc = uploadedDocumentRepository.findByDocumentUuid(oldDocument.documentUUID)
    assertThat(oldDoc).isNotNull
    assertThat(oldDoc!!.appearance).isNull()

    verifyDocumentMetadataUpdated(oldDocument.documentUUID, "DELETED")

    val newDoc = uploadedDocumentRepository.findByDocumentUuid(newDocument.documentUUID)
    assertThat(newDoc).isNotNull
    assertThat(newDoc!!.appearance?.appearanceUuid).isEqualTo(createdAppearance.appearanceUuid)

    verifyDocumentMetadataUpdated(newDocument.documentUUID, "ACTIVE")
  }

  @Test
  fun `update appearance with added and removed documents updates metadata correctly`() {
    val (docA) = uploadDocument()
    val (docB) = uploadDocument()
    documentManagementApi.stubUpdateDocumentStatus(docA.documentUUID.toString())
    documentManagementApi.stubUpdateDocumentStatus(docB.documentUUID.toString())

    val appearance = dpsCreateCourtAppearance(documents = listOf(docA, docB))
    val courtCase = createCourtCase(DpsDataCreator.dpsCreateCourtCase(appearances = listOf(appearance)))
    val createdAppearance = courtCase.second.appearances.first()
    getCourtCase(courtCase.first)

    val (docC) = uploadDocument()
    documentManagementApi.stubUpdateDocumentStatus(docC.documentUUID.toString())

    val updateCourtAppearance = dpsCreateCourtAppearance(
      courtCaseUuid = courtCase.first,
      appearanceUUID = createdAppearance.appearanceUuid,
      documents = listOf(docB, docC),
    )

    putCourtAppearance(createdAppearance.appearanceUuid, updateCourtAppearance)

    verifyDocumentMetadataUpdated(docA.documentUUID, "DELETED")
    verifyDocumentMetadataUpdated(docC.documentUUID, "ACTIVE")
  }

  @Test
  fun `copy over interim charges to future appearance`() {
    val remandCharge = DpsDataCreator.dpsCreateCharge(
      outcomeUuid = UUID.fromString("315280e5-d53e-43b3-8ba6-44da25676ce2"),
      sentence = null,
    )
    val noNextAppearance = dpsCreateCourtAppearance(
      nextCourtAppearance = null,
      outcomeUuid = UUID.fromString("2f585681-7b1a-44fb-a0cb-f9a4b1d9cda8"),
      warrantType = "NON_SENTENCING",
      charges = listOf(remandCharge),
    )
    val (courtCaseUuid) = createCourtCase(createCourtCase = DpsDataCreator.dpsCreateCourtCase(appearances = listOf(noNextAppearance)))

    val updateAppearanceToHaveNextCourtAppearance = noNextAppearance.copy(
      courtCaseUuid = courtCaseUuid,
      nextCourtAppearance = DpsDataCreator.dpsCreateNextCourtAppearance(),
    )

    putCourtAppearance(noNextAppearance.appearanceUuid, updateAppearanceToHaveNextCourtAppearance)

    val futureAppearance = courtAppearanceRepository.findByCourtCaseCaseUniqueIdentifierAndStatusId(
      courtCaseUuid,
      CourtAppearanceEntityStatus.FUTURE,
    )
    webTestClient
      .get()
      .uri("/court-appearance/${futureAppearance.appearanceUuid}")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.charges[0].chargeUuid")
      .isEqualTo(remandCharge.chargeUuid.toString())
      .jsonPath("$.charges[0].outcome")
      .doesNotExist()
  }

  @Test
  fun `do not copy over edited offence to future when updated in current court appearance`() {
    val remandCharge = DpsDataCreator.dpsCreateCharge(
      outcomeUuid = UUID.fromString("315280e5-d53e-43b3-8ba6-44da25676ce2"),
      sentence = null,
    )
    val courtAppearanceWithNextCourtAppearance = dpsCreateCourtAppearance(
      nextCourtAppearance = DpsDataCreator.dpsCreateNextCourtAppearance(),
      outcomeUuid = UUID.fromString("2f585681-7b1a-44fb-a0cb-f9a4b1d9cda8"),
      warrantType = "NON_SENTENCING",
      charges = listOf(remandCharge),
    )
    val (courtCaseUuid) = createCourtCase(createCourtCase = DpsDataCreator.dpsCreateCourtCase(appearances = listOf(courtAppearanceWithNextCourtAppearance)))

    val editedCharge = remandCharge.copy(
      offenceStartDate = remandCharge.offenceStartDate.minusDays(10),
    )
    val editAppearance = courtAppearanceWithNextCourtAppearance.copy(
      courtCaseUuid = courtCaseUuid,
      charges = listOf(editedCharge),
    )

    putCourtAppearance(courtAppearanceWithNextCourtAppearance.appearanceUuid, editAppearance)

    val futureAppearance = courtAppearanceRepository.findByCourtCaseCaseUniqueIdentifierAndStatusId(
      courtCaseUuid,
      CourtAppearanceEntityStatus.FUTURE,
    )
    webTestClient
      .get()
      .uri("/court-appearance/${futureAppearance.appearanceUuid}")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.charges[0].offenceStartDate")
      .isEqualTo(remandCharge.offenceStartDate.format(DateTimeFormatter.ISO_DATE))
  }

  @Test
  fun `updating only a court appearance keeps the next court appearance`() {
    val courtCase = createCourtCase()
    val createdAppearance = courtCase.second.appearances.first()
    val updateCourtAppearance = dpsCreateCourtAppearance(
      courtCaseUuid = courtCase.first,
      appearanceUUID = createdAppearance.appearanceUuid,
      courtCaseReference = "ADIFFERENTCOURTCASEREFERENCE",
    )
    val response = webTestClient
      .put()
      .uri("/court-appearance/${createdAppearance.appearanceUuid}")
      .bodyValue(updateCourtAppearance)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
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
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
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
    val sentencedAppearance = dpsCreateCourtAppearance(
      outcomeUuid = UUID.fromString("62412083-9892-48c9-bf01-7864af4a8b3c"),
      warrantType = "SENTENCING",
      appearanceDate = appearanceDate,
    )
    val (courtCaseUuid, createdCourtCase) = createCourtCase(
      DpsDataCreator.dpsCreateCourtCase(
        appearances = listOf(
          sentencedAppearance,
        ),
      ),
    )
    val createdAppearance = createdCourtCase.appearances.first()
      .copy(courtCaseUuid = courtCaseUuid, appearanceDate = appearanceDate.minusDays(10))

    putCourtAppearance(createdAppearance.appearanceUuid, createdAppearance)

    val messages = getMessages(2)
    assertThat(messages).hasSize(2).extracting<String> { it.eventType }.contains("sentence.updated")
  }

  @Test
  fun `updating the next appearance time stores the updated value`() {
    val courtCase = createCourtCase()
    val createdAppearance = courtCase.second.appearances.first()
    val createdNextAppearance = createdAppearance.nextCourtAppearance!!
    val updateNextAppearance = createdNextAppearance.copy(
      appearanceTime = createdNextAppearance.appearanceTime!!.plusHours(2).withSecond(0).withNano(0),
    )
    val updateCourtAppearance = createdAppearance.copy(
      courtCaseUuid = courtCase.first,
      appearanceUuid = createdAppearance.appearanceUuid,
      nextCourtAppearance = updateNextAppearance,
    )
    val response = webTestClient
      .put()
      .uri("/court-appearance/${createdAppearance.appearanceUuid}")
      .bodyValue(updateCourtAppearance)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
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
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.nextCourtAppearance.appearanceTime")
      .isEqualTo(updateNextAppearance.appearanceTime!!.format(DateTimeFormatter.ISO_LOCAL_TIME))
    Assertions.assertThat(nextCourtAppearanceRepository.count()).isEqualTo(1)
  }

  @Test
  fun `do not delete the future appearance if its turned to an active appearance when clearing the next court appearance`() {
    val (courtCaseUuid, courtCase) = createCourtCase()
    val createdAppearance = courtCase.appearances.first()
    val futureAppearance = courtAppearanceRepository.findByCourtCaseCaseUniqueIdentifierAndStatusId(
      courtCaseUuid,
      CourtAppearanceEntityStatus.FUTURE,
    )

    val futureAppearanceUpdate = dpsCreateCourtAppearance(
      courtCaseUuid = courtCaseUuid,
      appearanceUUID = futureAppearance.appearanceUuid,
      nextCourtAppearance = null,
    )

    putCourtAppearance(futureAppearance.appearanceUuid, futureAppearanceUpdate)

    val updateCourtAppearance = createdAppearance.copy(
      courtCaseUuid = courtCaseUuid,
      appearanceUuid = createdAppearance.appearanceUuid,
      nextCourtAppearance = null,
    )

    putCourtAppearance(createdAppearance.appearanceUuid, updateCourtAppearance)

    webTestClient
      .get()
      .uri("/court-appearance/${futureAppearance.appearanceUuid}")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isOk
  }

  @Test
  fun `update appearance to edit charge offence code`() {
    val courtCase = createCourtCase()
    val charge = courtCase.second.appearances.first().charges.first().copy(offenceCode = "OFF634624")
    val appearance =
      courtCase.second.appearances.first().copy(charges = listOf(charge), courtCaseUuid = courtCase.first)

    putCourtAppearance(appearance.appearanceUuid, appearance)

    webTestClient
      .get()
      .uri("/court-appearance/${appearance.appearanceUuid}")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
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
  fun `charge can have different values across appearances`() {
    val charge = DpsDataCreator.dpsCreateCharge(sentence = null)
    val firstAppearance = dpsCreateCourtAppearance(appearanceDate = LocalDate.now().minusDays(15), charges = listOf(charge))
    val secondAppearance = dpsCreateCourtAppearance(appearanceDate = LocalDate.now().minusDays(10), charges = listOf(charge))
    val (courtCaseUuid) = createCourtCase(DpsDataCreator.dpsCreateCourtCase(appearances = listOf(firstAppearance, secondAppearance)))

    val editedCharge = charge.copy(offenceStartDate = charge.offenceStartDate.minusDays(5))
    val editedAppearance = firstAppearance.copy(charges = listOf(editedCharge), courtCaseUuid = courtCaseUuid)

    putCourtAppearance(editedAppearance.appearanceUuid, editedAppearance)

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
      .jsonPath("$.appearances.[?(@.appearanceUuid == '${firstAppearance.appearanceUuid}')].charges.[?(@.chargeUuid == '${charge.chargeUuid}')].offenceStartDate")
      .isEqualTo(editedCharge.offenceStartDate.format(DateTimeFormatter.ISO_LOCAL_DATE))
      .jsonPath("$.appearances.[?(@.appearanceUuid == '${secondAppearance.appearanceUuid}')].charges.[?(@.chargeUuid == '${charge.chargeUuid}')].offenceStartDate")
      .isEqualTo(charge.offenceStartDate.format(DateTimeFormatter.ISO_LOCAL_DATE))
  }

  @Test
  fun `update appearance to delete charge`() {
    val courtCase = createCourtCase()
    val charge = DpsDataCreator.dpsCreateCharge()
    val secondCharge = DpsDataCreator.dpsCreateCharge(offenceCode = "OFF567")
    val appearance =
      courtCase.second.appearances.first().copy(charges = listOf(charge, secondCharge), courtCaseUuid = courtCase.first)

    putCourtAppearance(appearance.appearanceUuid, appearance)

    purgeQueues()
    val appearanceWithoutSecondCharge = appearance.copy(charges = listOf(charge))

    putCourtAppearance(appearance.appearanceUuid, appearanceWithoutSecondCharge)

    val messages = getMessages(4)
    assertThat(messages).hasSize(4).extracting<String> { it.eventType }.contains("court-appearance.updated")

    webTestClient
      .get()
      .uri("/court-appearance/${appearance.appearanceUuid}")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
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
  fun `update appearance does not emit delete period length events on deleted period lengths`() {
    val (courtCaseUuid, courtCase) = createCourtCase()
    val appearance = courtCase.appearances.first().copy(courtCaseUuid = courtCaseUuid)
    val charge = appearance.charges.first()
    val sentenceWithoutPeriodLength = charge.sentence!!.copy(periodLengths = listOf())
    val editedCharge = charge.copy(sentence = sentenceWithoutPeriodLength)
    val editedAppearance = appearance.copy(charges = listOf(editedCharge))

    putCourtAppearance(appearance.appearanceUuid, editedAppearance)

    purgeQueues()
    val appearanceWithSentenceEdit = editedAppearance.copy(charges = listOf(editedCharge.copy(sentence = sentenceWithoutPeriodLength.copy(convictionDate = sentenceWithoutPeriodLength.convictionDate!!.minusDays(2L)))))
    putCourtAppearance(appearance.appearanceUuid, appearanceWithSentenceEdit)

    val messages = getMessages(1)
    assertThat(messages).hasSize(1).extracting<String> { it.eventType }.doesNotContain("sentence.period-length.deleted")
  }

  @Test
  fun `updating the appearance changing sentence type`() {
    val sdsUuid = UUID.fromString("1104e683-5467-4340-b961-ff53672c4f39")
    val edsUuid = UUID.fromString("18d5af6d-2fa7-4166-a4c9-8381a1e3c7e0")
    val sentence = DpsDataCreator.dpsCreateSentence(sentenceTypeId = sdsUuid)
    val charge = DpsDataCreator.dpsCreateCharge(sentence = sentence)
    val appearance = dpsCreateCourtAppearance(
      charges = listOf(charge),
      periodLengths = listOf(DpsDataCreator.dpsCreatePeriodLength(years = 6)),
    )
    val (courtCaseUuid, createdCourtCase) = createCourtCase(
      DpsDataCreator.dpsCreateCourtCase(
        appearances = listOf(
          appearance,
        ),
      ),
    )

    val createdAppearance = createdCourtCase.appearances.first()
    // Update from SDS sentence to EDS sentence
    val updateAppearance = createdAppearance.copy(
      courtCaseUuid = courtCaseUuid,
      charges = createdAppearance.charges.map { it.copy(sentence = it.sentence?.copy(sentenceTypeId = edsUuid)) },
    )
    putCourtAppearance(createdAppearance.appearanceUuid, updateAppearance)

    webTestClient
      .get()
      .uri("/court-appearance/${appearance.appearanceUuid}")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.charges.[?(@.chargeUuid == '${charge.chargeUuid}')].sentence.sentenceType.sentenceTypeUuid")
      .isEqualTo(edsUuid.toString())
  }

  @Test
  fun `update court appearance only adding a next court appearance results in an appearance updated event`() {
    val appearance = dpsCreateCourtAppearance(
      nextCourtAppearance = null,
    )
    val (courtCaseUuid, createdCourtCase) = createCourtCase(
      DpsDataCreator.dpsCreateCourtCase(
        appearances = listOf(
          appearance,
        ),
      ),
    )
    val createdAppearance = createdCourtCase.appearances.first()
    val updateAppearance = createdAppearance.copy(
      courtCaseUuid = courtCaseUuid,
      nextCourtAppearance = DpsDataCreator.dpsCreateNextCourtAppearance(),
    )
    putCourtAppearance(createdAppearance.appearanceUuid, updateAppearance)

    val messages = getMessages(2)
    assertThat(messages).hasSize(2).extracting<String> { it.eventType }.containsExactlyInAnyOrder("court-appearance.updated", "court-appearance.inserted")
  }

  @Test
  fun `update court appearance only deleting a next court appearance results in an appearance updated event`() {
    val appearance = dpsCreateCourtAppearance(
      nextCourtAppearance = DpsDataCreator.dpsCreateNextCourtAppearance(),
    )
    val (courtCaseUuid, createdCourtCase) = createCourtCase(
      DpsDataCreator.dpsCreateCourtCase(
        appearances = listOf(
          appearance,
        ),
      ),
    )
    val createdAppearance = createdCourtCase.appearances.first()
    val updateAppearance = createdAppearance.copy(
      courtCaseUuid = courtCaseUuid,
      nextCourtAppearance = null,
    )
    putCourtAppearance(createdAppearance.appearanceUuid, updateAppearance)

    val messages = getMessages(2)
    assertThat(messages).hasSize(2).extracting<String> { it.eventType }.containsExactlyInAnyOrder("court-appearance.updated", "court-appearance.deleted")
  }

  @Test
  fun `updating offence code in new appearance must keep charge as is in old appearance`() {
    val charge = DpsDataCreator.dpsCreateCharge(
      outcomeUuid = UUID.fromString("315280e5-d53e-43b3-8ba6-44da25676ce2"),
      sentence = null,
    )
    val appearance = dpsCreateCourtAppearance(
      outcomeUuid = UUID.fromString("2f585681-7b1a-44fb-a0cb-f9a4b1d9cda8"),
      appearanceDate = LocalDate.now().minusDays(20),
      charges = listOf(charge),
      nextCourtAppearance = null,
    )
    val (courtCaseUuid) = createCourtCase(DpsDataCreator.dpsCreateCourtCase(appearances = listOf(appearance)))
    val updatedOffenceCodeCharge = charge.copy(appearanceUuid = appearance.appearanceUuid, offenceCode = "ADIFFERENTCODE")
    val newAppearance = dpsCreateCourtAppearance(
      courtCaseUuid = courtCaseUuid,
      outcomeUuid = UUID.fromString("2f585681-7b1a-44fb-a0cb-f9a4b1d9cda8"),
      charges = listOf(updatedOffenceCodeCharge),
      nextCourtAppearance = null,
    )

    putCourtAppearance(newAppearance.appearanceUuid, newAppearance)

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
      .jsonPath("$.appearances.[?(@.appearanceUuid == '${appearance.appearanceUuid}')].charges.[?(@.chargeUuid == '${charge.chargeUuid}')].outcome.outcomeUuid")
      .isEqualTo(charge.outcomeUuid.toString())
      .jsonPath("$.appearances.[?(@.appearanceUuid == '${newAppearance.appearanceUuid}')].charges.[?(@.chargeUuid == '${charge.chargeUuid}')].outcome.outcomeUuid")
      .isEqualTo(ChargeService.replacedWithAnotherOutcomeUuid.toString())
  }

  @Test
  fun `updating next court appearance type correctly returns nomisAppearanceTypeCode`() {
    val futureAppearance = DataCreator.migrationCreateCourtAppearance(eventId = 567, appearanceDate = LocalDate.now().plusDays(2), legacyData = DataCreator.courtAppearanceLegacyData(nomisOutcomeCode = null, outcomeDescription = null, nextEventDateTime = null, nomisAppearanceTypeCode = "PS"), charges = emptyList())
    val firstAppearance = DataCreator.migrationCreateCourtAppearance(legacyData = DataCreator.courtAppearanceLegacyData(nextEventDateTime = futureAppearance.appearanceDate.atTime(10, 0)), charges = emptyList())
    val migrationCourtCase = DataCreator.migrationCreateCourtCase(appearances = listOf(firstAppearance, futureAppearance))
    val migrationCourtCases = DataCreator.migrationCreateCourtCases(courtCases = listOf(migrationCourtCase))
    val response = webTestClient
      .post()
      .uri("/legacy/court-case/migration")
      .bodyValue(migrationCourtCases)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_COURT_CASE_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isCreated
      .returnResult(MigrationCreateCourtCasesResponse::class.java)
      .responseBody.blockFirst()!!

    val firstAppearanceUuid = response.appearances.first { appearanceResponse -> firstAppearance.eventId == appearanceResponse.eventId }.appearanceUuid
    val futureAppearanceUuid = response.appearances.first { appearanceResponse -> futureAppearance.eventId == appearanceResponse.eventId }.appearanceUuid
    val courtCaseUuid = response.courtCases.first().courtCaseUuid

    val updateCourtAppearance = DpsDataCreator.dpsCreateNonSentencedCourtAppearance(courtCaseUuid = courtCaseUuid, appearanceUUID = firstAppearanceUuid, appearanceDate = firstAppearance.appearanceDate, charges = emptyList(), documents = emptyList(), nextCourtAppearance = DpsDataCreator.dpsCreateNextCourtAppearance(appearanceTypeUuid = UUID.fromString("1da09b6e-55cb-4838-a157-ee6944f2094c"), courtAppearanceSubtypeUuid = null))
    putCourtAppearance(updateCourtAppearance.appearanceUuid, updateCourtAppearance)

    val appearanceType = appearanceTypeRepository.findByAppearanceTypeUuid(updateCourtAppearance.nextCourtAppearance!!.appearanceTypeUuid)!!

    webTestClient
      .get()
      .uri("/court-appearance/$futureAppearanceUuid")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.legacyData.nomisAppearanceTypeCode")
      .isEqualTo(appearanceType.dpsToNomisMappingCode)
  }

  @Test
  fun `must not update appearance when no court case exists`() {
    val updateCourtAppearance = dpsCreateCourtAppearance(courtCaseUuid = UUID.randomUUID().toString())
    webTestClient
      .put()
      .uri("/court-appearance/${updateCourtAppearance.appearanceUuid}")
      .bodyValue(updateCourtAppearance)
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
    val updateCourtAppearance = dpsCreateCourtAppearance()
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
    val updateCourtAppearance = dpsCreateCourtAppearance()
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

  @Test
  fun `updating a court appearance and changing the case reference removes the old case ref from legacy data`() {
    val createCourtCase: Pair<String, CreateCourtCase> = createCourtCase()
    val caseUuid = createCourtCase.first
    createCourtCase.second.appearances
    val oldRef = createCourtCase.second.appearances[0].courtCaseReference

    val courtCase = getCourtCase(caseUuid)
    assertThat(courtCase.legacyData!!.caseReferences.map { it.offenderCaseReference }).containsExactly(oldRef)

    val newRef = "UPDATED-CASE-REF"
    val appearanceWithWithNewRef = createCourtCase.second.appearances[0].copy(
      appearanceUuid = courtCase.appearances[0].appearanceUuid,
      courtCaseReference = newRef,
      courtCaseUuid = caseUuid,
    )
    putCourtAppearance(appearanceWithWithNewRef.appearanceUuid, appearanceWithWithNewRef)

    val courtCaseAfter = getCourtCase(caseUuid)
    assertThat(courtCaseAfter.legacyData!!.caseReferences.map { it.offenderCaseReference }).containsExactly(newRef)
  }

  @Test
  fun `cannot update an appearance that is deleted`() {
    val courtCase = createCourtCase()
    val appearance = courtCase.second.appearances.first()
    val appearanceUuid = appearance.appearanceUuid

    deleteCourtAppearance(appearanceUuid)

    val updateAppearance = appearance.copy(
      courtCaseUuid = courtCase.first,
      charges = listOf(DpsDataCreator.dpsCreateCharge()),
    )

    webTestClient
      .put()
      .uri("/court-appearance/$appearanceUuid")
      .bodyValue(updateAppearance)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .is4xxClientError
  }

  @Test
  fun `updating an inactive sentence does not change its status to active`() {
    val sentence = DpsDataCreator.dpsCreateSentence()
    val charge = DpsDataCreator.dpsCreateCharge(sentence = sentence)
    val appearance = dpsCreateCourtAppearance(charges = listOf(charge))
    val (courtCaseUuid, createdCourtCase) = createCourtCase(DpsDataCreator.dpsCreateCourtCase(appearances = listOf(appearance)))

    val createdSentence = sentenceRepository.findBySentenceUuid(sentence.sentenceUuid)[0]
    createdSentence.statusId = SentenceEntityStatus.INACTIVE
    sentenceRepository.save(createdSentence)

    val createdAppearance = createdCourtCase.appearances.first()
    val updatedSentence = createdAppearance.charges.first().sentence!!.copy(convictionDate = LocalDate.now().minusDays(5))
    val updatedCharge = createdAppearance.charges.first().copy(sentence = updatedSentence)
    val updateAppearance = createdAppearance.copy(
      courtCaseUuid = courtCaseUuid,
      charges = listOf(updatedCharge),
    )
    putCourtAppearance(createdAppearance.appearanceUuid, updateAppearance)

    val latestSentence = sentenceRepository.findBySentenceUuid(createdSentence.sentenceUuid)[0]
    assertThat(latestSentence).isNotNull
    assertThat(latestSentence.statusId).isEqualTo(SentenceEntityStatus.INACTIVE)
  }

  @Test
  fun `should update a charge that exists in two appearances replaces aggravating factors`() {
    val createCharge = DpsDataCreator.dpsCreateCharge(aggravatingFactors = listOf(AggravatingFactor(code = "OATC", title = "Offence Aggravated by Terrorist Connection", description = "Offence Aggravated by Terrorist Connection", displayOrder = 10)))
    val (courtCaseUuid, createdCourtCase) = createCourtCase(
      DpsDataCreator.dpsCreateCourtCase(
        appearances = listOf(dpsCreateCourtAppearance(charges = listOf(createCharge))),
      ),
    )
    val createdAppearance = createdCourtCase.appearances.first()
    createCourtAppearance(
      dpsCreateCourtAppearance(
        courtCaseUuid = courtCaseUuid,
        charges = listOf(createCharge.copy(outcomeUuid = UUID.fromString("315280e5-d53e-43b3-8ba6-44da25676ce2"))),
      ),
    )
    val updateCharge = createCharge.copy(
      aggravatingFactors = listOf(AggravatingFactor(code = "OAFPC", title = "Offence Aggravated by Foreign Power", description = "Offence Aggravated by Foreign Power", displayOrder = 10)),
      offenceStartDate = LocalDate.now().minusDays(1),
      appearanceUuid = createdAppearance.appearanceUuid,
    )

    // Act
    webTestClient.put()
      .uri("/charge/${createCharge.chargeUuid}")
      .bodyValue(updateCharge)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus().isOk

    assertThat(aggravatingFactors.countAggravatingFactorForLatestCharge("OATC")).isEqualTo(0)
    assertThat(aggravatingFactors.countAggravatingFactorForLatestCharge("OAFPC")).isEqualTo(1)
  }

  @Test
  fun `should update a charge with a changed offence code in two appearances replaces aggravating factors`() {
    val charge = DpsDataCreator.dpsCreateCharge(
      outcomeUuid = UUID.fromString("315280e5-d53e-43b3-8ba6-44da25676ce2"),
      sentence = null,
    )
    val appearance = dpsCreateCourtAppearance(
      outcomeUuid = UUID.fromString("2f585681-7b1a-44fb-a0cb-f9a4b1d9cda8"),
      appearanceDate = LocalDate.now().minusDays(20),
      charges = listOf(charge),
      nextCourtAppearance = null,
    )
    val (courtCaseUuid) = createCourtCase(DpsDataCreator.dpsCreateCourtCase(appearances = listOf(appearance)))
    val updatedOffenceCodeCharge = charge.copy(appearanceUuid = appearance.appearanceUuid, offenceCode = "ADIFFERENTCODE")
    val newAppearance = dpsCreateCourtAppearance(
      courtCaseUuid = courtCaseUuid,
      outcomeUuid = UUID.fromString("2f585681-7b1a-44fb-a0cb-f9a4b1d9cda8"),
      charges = listOf(updatedOffenceCodeCharge),
      nextCourtAppearance = null,
    )

    // Act
    putCourtAppearance(newAppearance.appearanceUuid, newAppearance)

    assertThat(aggravatingFactors.countAggravatingFactorForLatestCharge("OATC")).isEqualTo(1)
    assertThat(aggravatingFactors.countAggravatingFactorForLatestCharge("OAFPC")).isEqualTo(0)
  }

  @Test
  fun `should update charge when when an aggravating factors is added which is neither terror related nor foreign power related`() {
    val charge = DpsDataCreator.dpsCreateCharge()
    val appearance = dpsCreateCourtAppearance(charges = listOf(charge))
    val (courtCaseUuid) = createCourtCase(DpsDataCreator.dpsCreateCourtCase(appearances = listOf(appearance)))
    val updatedOffenceCodeCharge = charge.copy(
      appearanceUuid = appearance.appearanceUuid,
      offenceCode = "ADIFFERENTCODE",
      aggravatingFactors = listOf(
        AggravatingFactor(code = "DISV", title = "Disability of victim", description = "Disability of victim", displayOrder = 120),
      ),
    )
    val newAppearance = dpsCreateCourtAppearance(
      courtCaseUuid = courtCaseUuid,
      outcomeUuid = UUID.fromString("2f585681-7b1a-44fb-a0cb-f9a4b1d9cda8"),
      charges = listOf(updatedOffenceCodeCharge),
      nextCourtAppearance = null,
    )

    // Act
    putCourtAppearance(newAppearance.appearanceUuid, newAppearance)

    assertThat(aggravatingFactors.countAggravatingFactorForLatestCharge("DISV")).isEqualTo(1)
  }

  @Test
  fun `should not modify aggravating factors when legacy af flags are unchanged`() {
    val createCharge = DpsDataCreator.dpsCreateCharge(aggravatingFactors = listOf(AggravatingFactor(code = "OATC", title = "Offence Aggravated by Terrorist Connection", description = "Offence Aggravated by Terrorist Connection", displayOrder = 10)))
    val createAppearance = dpsCreateCourtAppearance(charges = listOf(createCharge), nextCourtAppearance = null)
    val (courtCaseUuid) = createCourtCase(DpsDataCreator.dpsCreateCourtCase(appearances = listOf(createAppearance)))
    val firstUpdate = createAppearance.copy(
      courtCaseUuid = courtCaseUuid,
      charges = listOf(
        createCharge.copy(
          offenceStartDate = createCharge.offenceStartDate.minusDays(1),
          appearanceUuid = createAppearance.appearanceUuid,
        ),
      ),
      nextCourtAppearance = null,
    )
    putCourtAppearance(createAppearance.appearanceUuid, firstUpdate)
    val secondUpdate = firstUpdate.copy(
      charges = listOf(
        createCharge.copy(
          offenceStartDate = createCharge.offenceStartDate.minusDays(2),
          appearanceUuid = createAppearance.appearanceUuid,
        ),
      ),
    )

    // Act
    putCourtAppearance(createAppearance.appearanceUuid, secondUpdate)

    assertThat(aggravatingFactors.countAggravatingFactor(createCharge.chargeUuid, "OATC")).isEqualTo(1)
    assertThat(aggravatingFactors.countAggravatingFactor(createCharge.chargeUuid, "OAFPC")).isEqualTo(0)
  }

  @Test
  fun `should persist aggravating factors when only charge aggravating factors change`() {
    val createCharge = DpsDataCreator.dpsCreateCharge(
      aggravatingFactors = listOf(
        AggravatingFactor(code = "OATC", title = "Offence Aggravated by Terrorist Connection", description = "Offence Aggravated by Terrorist Connection", displayOrder = 10),
      ),
    )
    val createAppearance = dpsCreateCourtAppearance(charges = listOf(createCharge), nextCourtAppearance = null)
    val (courtCaseUuid) = createCourtCase(DpsDataCreator.dpsCreateCourtCase(appearances = listOf(createAppearance)))

    val updateAppearance = createAppearance.copy(
      courtCaseUuid = courtCaseUuid,
      charges = listOf(
        createCharge.copy(
          appearanceUuid = createAppearance.appearanceUuid,
          aggravatingFactors = listOf(
            AggravatingFactor(code = "OATC", title = "Offence Aggravated by Terrorist Connection", description = "Offence Aggravated by Terrorist Connection", displayOrder = 10),
            AggravatingFactor(code = "OAFPC", title = "Offence Aggravated by Foreign Power", description = "Offence Aggravated by Foreign Power", displayOrder = 20),
          ),
        ),
      ),
      nextCourtAppearance = null,
    )

    // Act
    putCourtAppearance(createAppearance.appearanceUuid, updateAppearance)

    assertThat(aggravatingFactors.countAggravatingFactor(createCharge.chargeUuid, "OATC")).isEqualTo(1)
    assertThat(aggravatingFactors.countAggravatingFactor(createCharge.chargeUuid, "OAFPC")).isEqualTo(1)
  }

  @Test
  fun `keep legacy data after updating in DPS`() {
    val (courtAppearanceUuid, createdCourtAppearance) = createLegacyCourtAppearance(legacyCreateCourtAppearance = DataCreator.legacyCreateCourtAppearance(appearanceDate = LocalDate.now().plusDays(5)))
    val updateAppearance = dpsCreateCourtAppearance(courtCaseUuid = createdCourtAppearance.courtCaseUuid)
    putCourtAppearance(courtAppearanceUuid, updateAppearance)

    webTestClient
      .get()
      .uri("/court-appearance/$courtAppearanceUuid")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.legacyData.appearanceTime")
      .isEqualTo(createdCourtAppearance.legacyData.appearanceTime!!.format(DateTimeFormatter.ISO_LOCAL_TIME))
  }

  @Test
  fun `must remove original outcome from appearance when updating offence code`() {
    val migrationCharge = DataCreator.migrationCreateCharge(legacyData = DataCreator.chargeLegacyData(nomisOutcomeCode = "4560"))
    val firstAppearance = DataCreator.migrationCreateCourtAppearance(charges = listOf(migrationCharge))
    val secondAppearance = DataCreator.migrationCreateCourtAppearance(eventId = firstAppearance.eventId + 1, appearanceDate = firstAppearance.appearanceDate.minusDays(5), charges = listOf(migrationCharge), legacyData = DataCreator.courtAppearanceLegacyData(nomisOutcomeCode = "4560"))
    val courtCase = DataCreator.migrationCreateCourtCase(appearances = listOf(firstAppearance, secondAppearance))
    val response = migrateCases(DataCreator.migrationCreateCourtCases(courtCases = listOf(courtCase)))
    val courtCaseUuid = response.courtCases.first { it.caseId == courtCase.caseId }.courtCaseUuid
    val secondAppearanceUuid = response.appearances.first { it.eventId == secondAppearance.eventId }.appearanceUuid
    val chargeUuid = response.charges.first { it.chargeNOMISId == migrationCharge.chargeNOMISId }.chargeUuid
    val updatedCharge = DpsDataCreator.dpsCreateCharge(appearanceUuid = secondAppearanceUuid, chargeUuid = chargeUuid, offenceStartDate = migrationCharge.offenceStartDate!!, offenceCode = "ANOTHERCODE", outcomeUuid = UUID.fromString("dd912c55-ca0d-4a68-8b0d-ba0a5e73b471"))
    val appearance = dpsCreateCourtAppearance(courtCaseUuid = courtCaseUuid, appearanceUUID = secondAppearanceUuid, outcomeUuid = UUID.fromString("fb966699-7adf-4c58-8852-395951e77846"), courtCode = secondAppearance.courtCode, courtCaseReference = "NOMIS123", appearanceDate = secondAppearance.appearanceDate, warrantType = "NON_SENTENCING", overallSentenceLength = null, nextCourtAppearance = null, charges = listOf(updatedCharge), overallConvictionDate = null, documents = listOf())
    putCourtAppearance(secondAppearanceUuid, appearance)
    webTestClient
      .get()
      .uri("/court-appearance/$secondAppearanceUuid")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.charges[?(@.chargeUuid == '$chargeUuid')].outcome.outcomeUuid")
      .isEqualTo("68e56c1f-b179-43da-9d00-1272805a7ad3") // replaced with another
  }

  @Test
  fun `creating breach of supervision appearance period length attaches to a sentence`() {
    val sentencedCharge = DpsDataCreator.dpsCreateCharge()
    val sentencingAppearance = dpsCreateCourtAppearance(appearanceDate = LocalDate.now().minusDays(10), nextCourtAppearance = null, charges = listOf(sentencedCharge))
    val (courtCaseUuid) = createCourtCase(DpsDataCreator.dpsCreateCourtCase(appearances = listOf(sentencingAppearance)))
    val breachPeriodLength = DpsDataCreator.dpsCreatePeriodLength(type = PeriodLengthType.BREACH_OF_SUPERVISION_REQUIREMENTS, days = 41, years = null)
    val breachAppearance = dpsCreateCourtAppearance(
      courtCaseUuid = courtCaseUuid,
      warrantType = "BREACH_OF_SUPERVISION_REQUIREMENTS",
      overallSentenceLength = null,
      nextCourtAppearance = null,
      charges = listOf(
        sentencedCharge.copy(sentence = null),
      ),
      periodLengths = listOf(breachPeriodLength),
    )
    putCourtAppearance(breachAppearance.appearanceUuid, breachAppearance)
    val courtCase = getCourtCase(courtCaseUuid)
    val sentence = courtCase.appearances.first { sentencingAppearance.appearanceUuid == it.appearanceUuid }.charges.first { it.chargeUuid == sentencedCharge.chargeUuid }.sentence!!
    Assertions.assertThat(sentence.periodLengths).anyMatch { periodLength -> periodLength.periodLengthUuid == breachPeriodLength.periodLengthUuid && periodLength.days == breachPeriodLength.days }
    val events = getMessages(3)
    Assertions.assertThat(events).anyMatch { it.eventType == "sentence.period-length.inserted" }
    val periodLengthInsertedEvent = events.first { it.eventType == "sentence.period-length.inserted" }
    val additionalInformation = objectMapper.treeToValue(periodLengthInsertedEvent.additionalInformation, HmppsPeriodLengthMessage::class.java)
    Assertions.assertThat(additionalInformation.courtAppearanceId).isEqualTo(sentencingAppearance.appearanceUuid.toString())
    Assertions.assertThat(additionalInformation.courtChargeId).isEqualTo(sentencedCharge.chargeUuid.toString())
  }

  @Test
  fun `updating breach of supervision appearance period length results in sentence updated event`() {
    val sentencedCharge = DpsDataCreator.dpsCreateCharge()
    val sentencingAppearance = dpsCreateCourtAppearance(appearanceDate = LocalDate.now().minusDays(10), nextCourtAppearance = null, charges = listOf(sentencedCharge))
    val (courtCaseUuid) = createCourtCase(DpsDataCreator.dpsCreateCourtCase(appearances = listOf(sentencingAppearance)))
    val breachPeriodLength = DpsDataCreator.dpsCreatePeriodLength(type = PeriodLengthType.BREACH_OF_SUPERVISION_REQUIREMENTS, days = 41, years = null)
    val breachAppearance = dpsCreateCourtAppearance(
      courtCaseUuid = courtCaseUuid,
      warrantType = "BREACH_OF_SUPERVISION_REQUIREMENTS",
      overallSentenceLength = null,
      nextCourtAppearance = null,
      charges = listOf(
        sentencedCharge.copy(sentence = null),
      ),
      periodLengths = listOf(breachPeriodLength),
    )
    putCourtAppearance(breachAppearance.appearanceUuid, breachAppearance)
    purgeQueues()
    val updatedBreachPeriodLength = breachPeriodLength.copy(days = 50)
    putCourtAppearance(breachAppearance.appearanceUuid, breachAppearance.copy(periodLengths = listOf(updatedBreachPeriodLength)))
    val events = getMessages(1)
    val periodLengthUpdatedEvent = events.first { it.eventType == "sentence.period-length.updated" }
    val additionalInformation = objectMapper.treeToValue(periodLengthUpdatedEvent.additionalInformation, HmppsPeriodLengthMessage::class.java)
    Assertions.assertThat(additionalInformation.courtAppearanceId).isEqualTo(sentencingAppearance.appearanceUuid.toString())
    Assertions.assertThat(additionalInformation.courtChargeId).isEqualTo(sentencedCharge.chargeUuid.toString())
  }

  private fun getCourtCase(caseUuid: String): CourtCase = webTestClient
    .get()
    .uri("/court-case/$caseUuid")
    .headers {
      it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
    }
    .exchange()
    .expectStatus()
    .isOk
    .returnResult(CourtCase::class.java)
    .responseBody.blockFirst()!!

  private fun putCourtAppearance(appearanceUuid: UUID, appearance: CreateCourtAppearance) = webTestClient
    .put()
    .uri("/court-appearance/$appearanceUuid")
    .bodyValue(appearance)
    .headers {
      it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
      it.contentType = MediaType.APPLICATION_JSON
    }
    .exchange()
    .expectStatus()
    .isOk
}
