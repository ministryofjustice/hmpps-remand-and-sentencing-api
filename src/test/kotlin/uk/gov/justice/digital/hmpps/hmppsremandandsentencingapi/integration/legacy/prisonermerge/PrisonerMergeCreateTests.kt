package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.prisonermerge

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.util.DataCreator
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.util.PrisonerMergeDataCreator
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.wiremock.AdjustmentsApiExtension.Companion.adjustmentsApi
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.CourtCaseEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.SentenceEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.merge.MergeCreateCourtCasesResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.DpsDataCreator
import java.time.LocalDate
import java.util.*
import java.util.regex.Pattern

class PrisonerMergeCreateTests : IntegrationTestBase() {

  @Test
  fun `moved all records to new prisoner number`() {
    val migratedRecords = migrateCases(DataCreator.migrationCreateSentenceCourtCases())
    val retainedPrisonerNumber = "PRI999"
    val mergePerson = PrisonerMergeDataCreator.mergePerson(casesCreated = listOf())

    webTestClient
      .post()
      .uri("/legacy/court-case/merge/person/$retainedPrisonerNumber")
      .bodyValue(mergePerson)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_COURT_CASE_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isCreated

    val courtCaseUuid = migratedRecords.courtCases.first().courtCaseUuid
    webTestClient.get()
      .uri {
        it.path("/court-case/paged/search")
          .queryParam("prisonerId", retainedPrisonerNumber)
          .build()
      }
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.content.[0].courtCaseUuid")
      .isEqualTo(courtCaseUuid)
  }

  @Test
  fun `deactivate court case and sentence on merge`() {
    val migratedRecords = migrateCases(DataCreator.migrationCreateSentenceCourtCases())
    val retainedPrisonerNumber = "PRI999"
    val retainedPrisonerRecords = migrateCases(DataCreator.migrationCreateSentenceCourtCases(prisonerId = retainedPrisonerNumber))
    val courtCaseUuid = migratedRecords.courtCases.first().courtCaseUuid
    val sentenceUuid = migratedRecords.sentences.first().sentenceUuid
    val retainedCourtCaseUuid = retainedPrisonerRecords.courtCases.first().courtCaseUuid
    val retainedSentenceUuid = retainedPrisonerRecords.sentences.first().sentenceUuid
    val mergePerson = PrisonerMergeDataCreator.mergePerson(
      casesDeactivated = listOf(PrisonerMergeDataCreator.deactivatedCourtCase(courtCaseUuid), PrisonerMergeDataCreator.deactivatedCourtCase(retainedCourtCaseUuid)),
      sentencesDeactivated = listOf(PrisonerMergeDataCreator.deactivatedSentence(sentenceUuid), PrisonerMergeDataCreator.deactivatedSentence(retainedSentenceUuid)),
    )

    webTestClient
      .post()
      .uri("/legacy/court-case/merge/person/$retainedPrisonerNumber")
      .bodyValue(mergePerson)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_COURT_CASE_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isCreated

    webTestClient.get()
      .uri {
        it.path("/court-case/paged/search")
          .queryParam("prisonerId", retainedPrisonerNumber)
          .build()
      }
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.content.[?(@.courtCaseUuid == '$courtCaseUuid')].courtCaseStatus")
      .isEqualTo(CourtCaseEntityStatus.INACTIVE.toString())
      .jsonPath("$.content.[?(@.courtCaseUuid == '$retainedCourtCaseUuid')].courtCaseStatus")
      .isEqualTo(CourtCaseEntityStatus.INACTIVE.toString())

    val sentence = sentenceRepository.findBySentenceUuid(sentenceUuid).first()
    assertThat(sentence.statusId).isEqualTo(SentenceEntityStatus.INACTIVE)

    val retainedSentence = sentenceRepository.findBySentenceUuid(retainedSentenceUuid).first()
    assertThat(retainedSentence.statusId).isEqualTo(SentenceEntityStatus.INACTIVE)
  }

  @Test
  fun `create all entities and return ids against NOMIS ids`() {
    migrateCases(DataCreator.migrationCreateSentenceCourtCases())
    val retainedPrisonerNumber = "PRI999"
    val mergePerson = PrisonerMergeDataCreator.mergePerson()

    val response = webTestClient
      .post()
      .uri("/legacy/court-case/merge/person/$retainedPrisonerNumber")
      .bodyValue(mergePerson)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_COURT_CASE_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isCreated
      .returnResult(MergeCreateCourtCasesResponse::class.java)
      .responseBody.blockFirst()!!

    assertThat(response.courtCases).hasSize(mergePerson.casesCreated.size)
    val courtCaseResponse = response.courtCases.first()

    assertThat(courtCaseResponse.courtCaseUuid).matches(Pattern.compile("([a-f0-9]{8}(-[a-f0-9]{4}){4}[a-f0-9]{8})"))
    assertThat(response.appearances).hasSize(mergePerson.casesCreated.flatMap { it.appearances }.size)
    val createdAppearance = response.appearances.first()
    assertThat(createdAppearance.eventId).isEqualTo(mergePerson.casesCreated.first().appearances.first().eventId)
    assertThat(response.charges).hasSize(mergePerson.casesCreated.flatMap { it.appearances.flatMap { it.charges } }.size)
    val createdCharge = response.charges.first()
    assertThat(createdCharge.chargeNOMISId).isEqualTo(mergePerson.casesCreated.first().appearances.first().charges.first().chargeNOMISId)
    val createdSentence = response.sentences.first()

    assertThat(createdSentence.sentenceNOMISId).isEqualTo(mergePerson.casesCreated.first().appearances.first().charges.first().sentence!!.sentenceId)

    val messagesOnQueue = getMessages(6)
    assertThat(messagesOnQueue).extracting<String> { it.eventType }.containsExactlyInAnyOrder(
      "court-case.inserted",
      "court-appearance.inserted",
      "charge.inserted",
      "sentence.inserted",
      "sentence.period-length.inserted",
      "court-case.updated",
    )
    assertThat(messagesOnQueue).extracting<String> { it.additionalInformation.get("source").asText() }
      .allMatch { it.equals("NOMIS") }
  }

  @Test
  fun `should merge recalls`() {
    adjustmentsApi.stubAllowCreateAdjustments()
    val removedPrisonerNumber = "RCLMER1"
    val retainedPrisonerNumber = "RCLMER2"

    val recallIdForPrisonerOne = createRecallForPrisoner(removedPrisonerNumber, LocalDate.of(2024, 2, 3))
    val recallIdForPrisonerTwo = createRecallForPrisoner(retainedPrisonerNumber, LocalDate.of(2024, 1, 1))

    val mergePerson = PrisonerMergeDataCreator.mergePerson(removedPrisonerNumber = removedPrisonerNumber)

    val mergeResponse = webTestClient
      .post()
      .uri("/legacy/court-case/merge/person/$retainedPrisonerNumber")
      .bodyValue(mergePerson)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_COURT_CASE_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isCreated
      .returnResult(MergeCreateCourtCasesResponse::class.java)
      .responseBody.blockFirst()!!

    assertThat(mergeResponse).isNotNull

    adjustmentsApi.stubGetAdjustmentsDefaultToNone()
    val recallsForRetainedPrisoner = getRecallsByPrisonerId(retainedPrisonerNumber)
    assertThat(recallsForRetainedPrisoner).hasSize(2)
    assertThat(recallsForRetainedPrisoner)
      .extracting<UUID> { it -> it.recallUuid }
      .containsExactlyInAnyOrder(recallIdForPrisonerOne, recallIdForPrisonerTwo)

    val recallsForRemovedPrisoner = getRecallsByPrisonerId(removedPrisonerNumber)
    assertThat(recallsForRemovedPrisoner).isEmpty()
  }

  private fun createRecallForPrisoner(prisonerNumber: String, returnToCustodyDate: LocalDate): UUID {
    val (sentenceOne, sentenceTwo) = createCourtCaseTwoSentences(prisonerNumber)
    val recall = DpsDataCreator.dpsCreateRecall(
      prisonerId = prisonerNumber,
      createdByUsername = "username1",
      returnToCustodyDate = returnToCustodyDate,
      sentenceIds = listOf(
        sentenceOne.sentenceUuid,
        sentenceTwo.sentenceUuid,
      ),
    )

    return createRecall(recall).recallUuid
  }
}
