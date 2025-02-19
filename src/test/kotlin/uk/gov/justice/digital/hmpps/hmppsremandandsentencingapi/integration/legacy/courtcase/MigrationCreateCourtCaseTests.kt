package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.courtcase

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.util.DataCreator
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.MigrationCreateCourtCaseResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.service.LegacySentenceService
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import java.util.regex.Pattern

class MigrationCreateCourtCaseTests : IntegrationTestBase() {

  @Test
  fun `create all entities and return ids against NOMIS ids`() {
    val migrationCourtCase = DataCreator.migrationCreateCourtCase()
    val response = webTestClient
      .post()
      .uri("/legacy/court-case/migration")
      .bodyValue(migrationCourtCase)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_COURT_CASE_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isCreated
      .returnResult(MigrationCreateCourtCaseResponse::class.java)
      .responseBody.blockFirst()!!

    Assertions.assertThat(response.courtCaseUuid).matches(Pattern.compile("([a-f0-9]{8}(-[a-f0-9]{4}){4}[a-f0-9]{8})"))
    Assertions.assertThat(response.appearances).hasSize(migrationCourtCase.appearances.size)
    val createdAppearance = response.appearances.first()
    Assertions.assertThat(createdAppearance.lifetimeUuid.toString()).matches(Pattern.compile("([a-f0-9]{8}(-[a-f0-9]{4}){4}[a-f0-9]{8})"))
    Assertions.assertThat(createdAppearance.eventId).isEqualTo(migrationCourtCase.appearances.first().legacyData.eventId!!)
    Assertions.assertThat(response.appearances).hasSize(migrationCourtCase.appearances.first().charges.size)
    val createdCharge = response.charges.first()
    Assertions.assertThat(createdCharge.lifetimeChargeUuid.toString()).matches(Pattern.compile("([a-f0-9]{8}(-[a-f0-9]{4}){4}[a-f0-9]{8})"))
    Assertions.assertThat(createdCharge.chargeNOMISId).isEqualTo(migrationCourtCase.appearances.first().charges.first().chargeNOMISId)
    val createdSentence = response.sentences.first()
    Assertions.assertThat(createdSentence.lifetimeSentenceUuid.toString()).matches(Pattern.compile("([a-f0-9]{8}(-[a-f0-9]{4}){4}[a-f0-9]{8})"))
    Assertions.assertThat(createdSentence.sentenceNOMISId).isEqualTo(migrationCourtCase.appearances.first().charges.first().sentence!!.sentenceId)
  }

  @Test
  fun `can create snapshots of charges in different appearances`() {
    val chargeNOMISId = "555"
    val firstSnapshot = DataCreator.migrationCreateCharge(chargeNOMISId = chargeNOMISId, legacyData = DataCreator.chargeLegacyData(nomisOutcomeCode = "99"))
    val secondSnapshot = DataCreator.migrationCreateCharge(chargeNOMISId = chargeNOMISId, legacyData = DataCreator.chargeLegacyData(nomisOutcomeCode = "66"))
    val firstAppearance = DataCreator.migrationCreateCourtAppearance(appearanceDate = LocalDate.now().minusDays(7), legacyData = DataCreator.courtAppearanceLegacyData(eventId = "1"), charges = listOf(firstSnapshot))
    val secondAppearance = DataCreator.migrationCreateCourtAppearance(appearanceDate = LocalDate.now().minusDays(2), legacyData = DataCreator.courtAppearanceLegacyData(eventId = "2"), charges = listOf(secondSnapshot))
    val migrationCourtCase = DataCreator.migrationCreateCourtCase(appearances = listOf(secondAppearance, firstAppearance))
    val response = webTestClient
      .post()
      .uri("/legacy/court-case/migration")
      .bodyValue(migrationCourtCase)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_COURT_CASE_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isCreated
      .returnResult(MigrationCreateCourtCaseResponse::class.java)
      .responseBody.blockFirst()!!
    Assertions.assertThat(response.charges).hasSize(1)
    val chargeLifetimeUuid = response.charges.first().lifetimeChargeUuid
    val firstAppearanceLifetimeUuid = response.appearances.first { appearanceResponse -> firstAppearance.legacyData.eventId == appearanceResponse.eventId }.lifetimeUuid
    checkChargeSnapshotOutcomeCode(firstAppearanceLifetimeUuid, chargeLifetimeUuid, firstSnapshot.legacyData.nomisOutcomeCode!!)
    val secondAppearanceLifetimeUuid = response.appearances.first { appearanceResponse -> secondAppearance.legacyData.eventId == appearanceResponse.eventId }.lifetimeUuid
    checkChargeSnapshotOutcomeCode(secondAppearanceLifetimeUuid, chargeLifetimeUuid, secondSnapshot.legacyData.nomisOutcomeCode!!)
  }

  @Test
  fun `creates DPS next court appearances when next court date and appearance date match`() {
    val futureAppearance = DataCreator.migrationCreateCourtAppearance(appearanceDate = LocalDate.now().plusDays(7), legacyData = DataCreator.courtAppearanceLegacyData(eventId = "567", nomisOutcomeCode = null, outcomeDescription = null, nextEventDateTime = null))
    val firstAppearance = DataCreator.migrationCreateCourtAppearance(legacyData = DataCreator.courtAppearanceLegacyData(nextEventDateTime = futureAppearance.appearanceDate.atTime(10, 0)))
    val migrationCourtCase = DataCreator.migrationCreateCourtCase(appearances = listOf(firstAppearance, futureAppearance))
    val response = webTestClient
      .post()
      .uri("/legacy/court-case/migration")
      .bodyValue(migrationCourtCase)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_COURT_CASE_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isCreated
      .returnResult(MigrationCreateCourtCaseResponse::class.java)
      .responseBody.blockFirst()!!

    val firstAppearanceLifetimeUuid = response.appearances.first { appearanceResponse -> firstAppearance.legacyData.eventId == appearanceResponse.eventId }.lifetimeUuid

    webTestClient
      .get()
      .uri("/court-case/${response.courtCaseUuid}")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING"))
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.appearances[?(@.lifetimeUuid == '$firstAppearanceLifetimeUuid')].nextCourtAppearance.courtCode")
      .isEqualTo(futureAppearance.courtCode)
  }

  @Test
  fun `create DPS next court appearance for latest past appearance when no matching next court date or appearance date`() {
    val futureAppearance = DataCreator.migrationCreateCourtAppearance(appearanceDate = LocalDate.now().plusDays(10), legacyData = DataCreator.courtAppearanceLegacyData(eventId = "567", nomisOutcomeCode = null, outcomeDescription = null, nextEventDateTime = null))
    val firstAppearance = DataCreator.migrationCreateCourtAppearance(legacyData = DataCreator.courtAppearanceLegacyData(nextEventDateTime = LocalDateTime.now().plusDays(5)))
    val migrationCourtCase = DataCreator.migrationCreateCourtCase(appearances = listOf(firstAppearance, futureAppearance))
    val response = webTestClient
      .post()
      .uri("/legacy/court-case/migration")
      .bodyValue(migrationCourtCase)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_COURT_CASE_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isCreated
      .returnResult(MigrationCreateCourtCaseResponse::class.java)
      .responseBody.blockFirst()!!

    val firstAppearanceLifetimeUuid = response.appearances.first { appearanceResponse -> firstAppearance.legacyData.eventId == appearanceResponse.eventId }.lifetimeUuid

    webTestClient
      .get()
      .uri("/court-case/${response.courtCaseUuid}")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING"))
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.appearances[?(@.lifetimeUuid == '$firstAppearanceLifetimeUuid')].nextCourtAppearance.courtCode")
      .isEqualTo(futureAppearance.courtCode)
  }

  @Test
  fun `can create sentence when consecutive to another in the same court case`() {
    val firstSentence = DataCreator.migrationCreateSentence(sentenceId = DataCreator.migrationSentenceId(1, 1), legacyData = DataCreator.sentenceLegacyData(sentenceCalcType = "FTR_ORA"))
    val consecutiveToSentence = DataCreator.migrationCreateSentence(sentenceId = DataCreator.migrationSentenceId(1, 5), consecutiveToSentenceId = firstSentence.sentenceId)
    val charge = DataCreator.migrationCreateCharge(chargeNOMISId = "11", sentence = firstSentence)
    val consecutiveToCharge = DataCreator.migrationCreateCharge(chargeNOMISId = "22", sentence = consecutiveToSentence)
    val appearance = DataCreator.migrationCreateCourtAppearance(charges = listOf(consecutiveToCharge, charge))
    val migrationCourtCase = DataCreator.migrationCreateCourtCase(appearances = listOf(appearance))
    val response = webTestClient
      .post()
      .uri("/legacy/court-case/migration")
      .bodyValue(migrationCourtCase)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_COURT_CASE_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isCreated
      .returnResult(MigrationCreateCourtCaseResponse::class.java)
      .responseBody.blockFirst()!!

    val consecutiveToSentenceLifetimeUuid = response.sentences.first { sentenceResponse -> sentenceResponse.sentenceNOMISId == consecutiveToSentence.sentenceId }.lifetimeSentenceUuid
    val firstSentenceLifetimeUuid = response.sentences.first { sentenceResponse -> sentenceResponse.sentenceNOMISId == firstSentence.sentenceId }.lifetimeSentenceUuid
    webTestClient
      .get()
      .uri("/court-case/${response.courtCaseUuid}")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING"))
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.appearances[*].charges[*].sentence[?(@.sentenceLifetimeUuid == '$consecutiveToSentenceLifetimeUuid')].consecutiveToChargeNumber")
      .isEqualTo(firstSentence.chargeNumber!!)
      .jsonPath("$.appearances[*].charges[*].sentence[?(@.sentenceLifetimeUuid == '$firstSentenceLifetimeUuid')].sentenceType.sentenceTypeUuid")
      .isEqualTo(LegacySentenceService.recallSentenceTypeBucketUuid.toString())
  }

  @Test
  fun `create source court case for a linked case`() {
    val sourceResponse = createSourceMergedCourtCase()
    val sourceCourtCaseUuid = sourceResponse.courtCaseUuid
    val sourceChargeUuid = sourceResponse.charges.first().lifetimeChargeUuid.toString()
    webTestClient
      .get()
      .uri("/court-case/$sourceCourtCaseUuid")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING"))
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.status")
      .isEqualTo("MERGED")
      .jsonPath("$.appearances[*].charges[?(@.lifetimeUuid == '$sourceChargeUuid')]")
      .exists()
  }

  @Test
  fun `create target court case for a linked case`() {
    val sourceResponse = createSourceMergedCourtCase()
    val sourceCourtCaseUuid = sourceResponse.courtCaseUuid
    val sourceChargeUuid = sourceResponse.charges.first().lifetimeChargeUuid
    val targetCharge = DataCreator.migrationCreateCharge(sentence = null, mergedFromCourtCaseUuid = sourceCourtCaseUuid, mergedChargeLifetimeUuid = sourceChargeUuid)
    val targetAppearance = DataCreator.migrationCreateCourtAppearance(charges = listOf(targetCharge))
    val targetCourtCase = DataCreator.migrationCreateCourtCase(appearances = listOf(targetAppearance))
    val targetResponse = webTestClient
      .post()
      .uri("/legacy/court-case/migration")
      .bodyValue(targetCourtCase)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_COURT_CASE_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isCreated
      .returnResult(MigrationCreateCourtCaseResponse::class.java)
      .responseBody.blockFirst()!!

    webTestClient
      .get()
      .uri("/court-case/${targetResponse.courtCaseUuid}")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING"))
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.appearances[*].charges[?(@.lifetimeUuid == '$sourceChargeUuid')]")
      .exists()
  }

  private fun createSourceMergedCourtCase(): MigrationCreateCourtCaseResponse {
    val charge = DataCreator.migrationCreateCharge(sentence = null, merged = true)
    val appearance = DataCreator.migrationCreateCourtAppearance(charges = listOf(charge))
    val sourceCourtCase = DataCreator.migrationCreateCourtCase(appearances = listOf(appearance), merged = true)
    return webTestClient
      .post()
      .uri("/legacy/court-case/migration")
      .bodyValue(sourceCourtCase)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_COURT_CASE_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isCreated
      .returnResult(MigrationCreateCourtCaseResponse::class.java)
      .responseBody.blockFirst()!!
  }

  private fun checkChargeSnapshotOutcomeCode(appearanceLifetimeUuid: UUID, chargeLifetimeUuid: UUID, expectedOutcomeCode: String) {
    webTestClient
      .get()
      .uri("/legacy/court-appearance/$appearanceLifetimeUuid/charge/$chargeLifetimeUuid")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_APPEARANCE_RO"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.nomisOutcomeCode")
      .isEqualTo(expectedOutcomeCode)
  }

  @Test
  fun `no token results in unauthorized`() {
    val migrationCourtCase = DataCreator.migrationCreateCourtCase()
    webTestClient
      .post()
      .uri("/legacy/court-case/migration")
      .bodyValue(migrationCourtCase)
      .headers {
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `token with incorrect role is forbidden`() {
    val migrationCourtCase = DataCreator.migrationCreateCourtCase()
    webTestClient
      .post()
      .uri("/legacy/court-case/migration")
      .bodyValue(migrationCourtCase)
      .headers {
        it.authToken(roles = listOf("ROLE_OTHER_FUNCTION"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isForbidden
  }
}
