package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.courtappearance

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.util.DataCreator
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacyCourtAppearanceCreatedResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.DpsDataCreator
import java.time.format.DateTimeFormatter
import java.util.UUID

class LegacyUpdateCourtAppearanceTests : IntegrationTestBase() {

  @Test
  fun `update appearance in existing court case`() {
    val createdCourtAppearance = createLegacyCourtAppearance()
    val toUpdate = DataCreator.legacyCreateCourtAppearance(courtCode = "ANOTHERCOURT")
    webTestClient
      .put()
      .uri("/legacy/court-appearance/${createdCourtAppearance.first}")
      .bodyValue(toUpdate)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_APPEARANCE_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isNoContent
    val message = getMessages(1)[0]
    Assertions.assertThat(message.eventType).isEqualTo("court-appearance.updated")
    Assertions.assertThat(message.additionalInformation.get("source").asText()).isEqualTo("NOMIS")
  }

  @Test
  fun `update future dated appearance`() {
    val (appearanceUuid, legacyCourtAppearance) = createLegacyCourtAppearance()
    val futureCourtAppearance = DataCreator.legacyCreateCourtAppearance(courtCaseUuid = legacyCourtAppearance.courtCaseUuid, appearanceDate = legacyCourtAppearance.legacyData.nextEventDateTime!!.toLocalDate(), legacyData = DataCreator.courtAppearanceLegacyData(nextEventDateTime = null))
    val response = webTestClient
      .post()
      .uri("/legacy/court-appearance")
      .bodyValue(futureCourtAppearance)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_APPEARANCE_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isCreated.returnResult(LegacyCourtAppearanceCreatedResponse::class.java)
      .responseBody.blockFirst()!!

    val editedFutureCourtAppearance = futureCourtAppearance.copy(courtCode = "ANOTHERCOURTCODE")
    webTestClient
      .put()
      .uri("/legacy/court-appearance/${response.lifetimeUuid}")
      .bodyValue(editedFutureCourtAppearance)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_APPEARANCE_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isNoContent

    webTestClient
      .get()
      .uri("/court-case/${legacyCourtAppearance.courtCaseUuid}")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.appearances[?(@.appearanceUuid == '$appearanceUuid')].nextCourtAppearance.courtCode")
      .isEqualTo(editedFutureCourtAppearance.courtCode)
  }

  @Test
  fun `updating future appearance type results in associated next court appearances being updated`() {
    val (appearanceUuid, legacyCourtAppearance) = createLegacyCourtAppearance()
    val futureCourtAppearance = DataCreator.legacyCreateCourtAppearance(courtCaseUuid = legacyCourtAppearance.courtCaseUuid, appearanceDate = legacyCourtAppearance.legacyData.nextEventDateTime!!.toLocalDate(), legacyData = DataCreator.courtAppearanceLegacyData(nextEventDateTime = null))
    val response = webTestClient
      .post()
      .uri("/legacy/court-appearance")
      .bodyValue(futureCourtAppearance)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_APPEARANCE_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isCreated.returnResult(LegacyCourtAppearanceCreatedResponse::class.java)
      .responseBody.blockFirst()!!

    val editedFutureCourtAppearance = futureCourtAppearance.copy(appearanceTypeUuid = UUID.fromString("1da09b6e-55cb-4838-a157-ee6944f2094c"))
    webTestClient
      .put()
      .uri("/legacy/court-appearance/${response.lifetimeUuid}")
      .bodyValue(editedFutureCourtAppearance)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_APPEARANCE_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isNoContent

    webTestClient
      .get()
      .uri("/court-case/${legacyCourtAppearance.courtCaseUuid}")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.appearances[?(@.appearanceUuid == '$appearanceUuid')].nextCourtAppearance.appearanceType.appearanceTypeUuid")
      .isEqualTo(editedFutureCourtAppearance.appearanceTypeUuid.toString())

    Assertions.assertThat(nextCourtAppearanceRepository.count()).isEqualTo(1)
  }

  @Test
  fun `updating immigration created court appearance results in an update to immigration detention record`() {
    val createImmigrationDetention = DpsDataCreator.dpsCreateImmigrationDetention()
    val createdImmigrationDetentionResponse = createImmigrationDetention(createImmigrationDetention)
    val toUpdate = DataCreator.legacyCreateCourtAppearance(courtCode = "IMM", appearanceDate = createImmigrationDetention.recordDate.plusDays(7), legacyData = DataCreator.courtAppearanceLegacyData(nomisOutcomeCode = "5500", nextEventDateTime = null))
    webTestClient
      .put()
      .uri("/legacy/court-appearance/${createdImmigrationDetentionResponse.courtAppearanceUuid}")
      .bodyValue(toUpdate)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_APPEARANCE_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isNoContent

    webTestClient
      .get()
      .uri("/immigration-detention/${createdImmigrationDetentionResponse.immigrationDetentionUuid}")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_SENTENCING__IMMIGRATION_DETENTION_RW"))
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.recordDate")
      .isEqualTo(toUpdate.appearanceDate.format(DateTimeFormatter.ISO_DATE))
  }

  @Test
  fun `must not update appearance when no court appearance exists`() {
    val toUpdate = DataCreator.legacyCreateCourtAppearance()
    webTestClient
      .put()
      .uri("/legacy/court-appearance/${UUID.randomUUID()}")
      .bodyValue(toUpdate)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_APPEARANCE_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isNotFound
  }

  @Test
  fun `no token results in unauthorized`() {
    val toUpdate = DataCreator.legacyCreateCourtAppearance()
    webTestClient
      .put()
      .uri("/legacy/court-appearance/${UUID.randomUUID()}")
      .bodyValue(toUpdate)
      .headers {
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `token with incorrect role is forbidden`() {
    val toUpdate = DataCreator.legacyCreateCourtAppearance()
    webTestClient
      .put()
      .uri("/legacy/court-appearance/${UUID.randomUUID()}")
      .bodyValue(toUpdate)
      .headers {
        it.authToken(roles = listOf("ROLE_OTHER_FUNCTION"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isForbidden
  }
}
