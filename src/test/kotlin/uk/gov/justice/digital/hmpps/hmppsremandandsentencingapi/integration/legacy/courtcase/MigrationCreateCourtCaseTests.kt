package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.courtcase

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.util.DataCreator
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.MigrationCreateCourtCaseResponse
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
