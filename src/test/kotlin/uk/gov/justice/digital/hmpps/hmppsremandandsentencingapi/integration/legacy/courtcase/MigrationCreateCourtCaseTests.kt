package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.courtcase

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.util.DataCreator
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.MigrationCreateCourtCaseResponse
import java.time.LocalDate
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
