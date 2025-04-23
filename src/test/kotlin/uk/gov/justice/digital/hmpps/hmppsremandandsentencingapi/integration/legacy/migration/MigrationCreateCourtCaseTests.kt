package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.migration

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.util.DataCreator
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.MigrationCreateCourtCasesResponse
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import java.util.regex.Pattern

class MigrationCreateCourtCaseTests : IntegrationTestBase() {

  @Test
  fun `create all entities and return ids against NOMIS ids`() {
    val migrationCourtCases = DataCreator.migrationCreateCourtCases()
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
    Assertions.assertThat(response.courtCases).hasSize(migrationCourtCases.courtCases.size)
    val courtCaseResponse = response.courtCases.first()

    Assertions.assertThat(courtCaseResponse.courtCaseUuid).matches(Pattern.compile("([a-f0-9]{8}(-[a-f0-9]{4}){4}[a-f0-9]{8})"))
    Assertions.assertThat(response.appearances).hasSize(migrationCourtCases.courtCases.flatMap { it.appearances }.size)
    val createdAppearance = response.appearances.first()
    Assertions.assertThat(createdAppearance.eventId).isEqualTo(migrationCourtCases.courtCases.first().appearances.first().eventId)
    Assertions.assertThat(response.charges).hasSize(migrationCourtCases.courtCases.flatMap { it.appearances.flatMap { it.charges } }.size)
    val createdCharge = response.charges.first()
    Assertions.assertThat(createdCharge.chargeNOMISId).isEqualTo(migrationCourtCases.courtCases.first().appearances.first().charges.first().chargeNOMISId)
    val createdSentence = response.sentences.first()

    Assertions.assertThat(createdSentence.sentenceNOMISId).isEqualTo(migrationCourtCases.courtCases.first().appearances.first().charges.first().sentence!!.sentenceId)
  }

  @Test
  fun `can create snapshots of charges in different appearances`() {
    val chargeNOMISId = 555L
    val firstSnapshot = DataCreator.migrationCreateCharge(chargeNOMISId = chargeNOMISId, legacyData = DataCreator.chargeLegacyData(nomisOutcomeCode = "99"), offenceEndDate = LocalDate.now().plusDays(5), sentence = null)
    val secondSnapshot = DataCreator.migrationCreateCharge(chargeNOMISId = chargeNOMISId, legacyData = DataCreator.chargeLegacyData(nomisOutcomeCode = "66"), offenceStartDate = null, sentence = null)
    val firstAppearance = DataCreator.migrationCreateCourtAppearance(eventId = 1, appearanceDate = LocalDate.now().minusDays(7), legacyData = DataCreator.courtAppearanceLegacyData(), charges = listOf(firstSnapshot))
    val secondAppearance = DataCreator.migrationCreateCourtAppearance(eventId = 2, appearanceDate = LocalDate.now().minusDays(2), legacyData = DataCreator.courtAppearanceLegacyData(), charges = listOf(secondSnapshot))
    val migrationCourtCase = DataCreator.migrationCreateCourtCase(appearances = listOf(secondAppearance, firstAppearance))
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
    Assertions.assertThat(response.charges).hasSize(1)
    val chargeLifetimeUuid = response.charges.first().chargeUuid
    val firstAppearanceUuid = response.appearances.first { appearanceResponse -> firstAppearance.eventId == appearanceResponse.eventId }.appearanceUuid
    checkChargeSnapshotOutcomeCode(firstAppearanceUuid, chargeLifetimeUuid, firstSnapshot.legacyData.nomisOutcomeCode!!)
    val secondAppearanceUuid = response.appearances.first { appearanceResponse -> secondAppearance.eventId == appearanceResponse.eventId }.appearanceUuid
    checkChargeSnapshotOutcomeCode(secondAppearanceUuid, chargeLifetimeUuid, secondSnapshot.legacyData.nomisOutcomeCode!!)
  }

  @Test
  fun `creates DPS next court appearances when next court date and appearance date match`() {
    val futureAppearance = DataCreator.migrationCreateCourtAppearance(eventId = 567, appearanceDate = LocalDate.now().plusDays(7), legacyData = DataCreator.courtAppearanceLegacyData(nomisOutcomeCode = null, outcomeDescription = null, nextEventDateTime = null))
    val firstAppearance = DataCreator.migrationCreateCourtAppearance(legacyData = DataCreator.courtAppearanceLegacyData(nextEventDateTime = futureAppearance.appearanceDate.atTime(10, 0)))
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

    webTestClient
      .get()
      .uri("/court-case/${response.courtCases.first().courtCaseUuid}")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING"))
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.appearances[?(@.appearanceUuid == '$firstAppearanceUuid')].nextCourtAppearance.courtCode")
      .isEqualTo(futureAppearance.courtCode)
  }

  @Test
  fun `create DPS next court appearance for latest past appearance when no matching next court date or appearance date`() {
    val futureAppearance = DataCreator.migrationCreateCourtAppearance(eventId = 567, appearanceDate = LocalDate.now().plusDays(10), legacyData = DataCreator.courtAppearanceLegacyData(nomisOutcomeCode = null, outcomeDescription = null, nextEventDateTime = null))
    val firstAppearance = DataCreator.migrationCreateCourtAppearance(legacyData = DataCreator.courtAppearanceLegacyData(nextEventDateTime = LocalDateTime.now().plusDays(5)))
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

    webTestClient
      .get()
      .uri("/court-case/${response.courtCases.first().courtCaseUuid}")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING"))
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.appearances[?(@.appearanceUuid == '$firstAppearanceUuid')].nextCourtAppearance.courtCode")
      .isEqualTo(futureAppearance.courtCode)
  }

  @Test
  fun `many charges to a single sentence creates multiple sentence records`() {
    val sentence = DataCreator.migrationCreateSentence()
    val firstCharge = DataCreator.migrationCreateCharge(sentence = sentence)
    val secondCharge = DataCreator.migrationCreateCharge(chargeNOMISId = 1111, sentence = sentence)
    val appearance = DataCreator.migrationCreateCourtAppearance(charges = listOf(firstCharge, secondCharge))
    val courtCase = DataCreator.migrationCreateCourtCase(appearances = listOf(appearance))
    val courtCases = DataCreator.migrationCreateCourtCases(courtCases = listOf(courtCase))

    val response = webTestClient
      .post()
      .uri("/legacy/court-case/migration")
      .bodyValue(courtCases)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_COURT_CASE_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isCreated
      .returnResult(MigrationCreateCourtCasesResponse::class.java)
      .responseBody.blockFirst()!!

    val sentenceUuid = response.sentences.first { sentence.sentenceId == it.sentenceNOMISId }.sentenceUuid
    val firstChargeUuid = response.charges.first { firstCharge.chargeNOMISId == it.chargeNOMISId }.chargeUuid
    val secondChargeUuid = response.charges.first { secondCharge.chargeNOMISId == it.chargeNOMISId }.chargeUuid
    val periodLengthUuid = response.sentenceTerms.first { sentence.periodLengths.first().periodLengthId == it.sentenceTermNOMISId }.periodLengthUuid
    webTestClient
      .get()
      .uri("/court-case/${response.courtCases.first().courtCaseUuid}")
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
      .jsonPath("$.appearances[*].charges[*].sentence.periodLengths[*].periodLengthUuid")
      .value<List<String>> { result ->
        Assertions.assertThat(result).contains(periodLengthUuid.toString())
        val counts = result.groupingBy { it }.eachCount()
        Assertions.assertThat(counts.values).allMatch { it == 1 }
      }
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
    val migrationCourtCases = DataCreator.migrationCreateCourtCases()
    webTestClient
      .post()
      .uri("/legacy/court-case/migration")
      .bodyValue(migrationCourtCases)
      .headers {
        it.authToken(roles = listOf("ROLE_OTHER_FUNCTION"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isForbidden
  }
}
