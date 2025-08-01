package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.courtappearance

import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.text.MatchesPattern
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.event.EventSource
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.event.EventSource.NOMIS
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.util.DataCreator
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.CaseReferenceLegacyData
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.CourtCaseLegacyData
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacyCourtAppearanceCreatedResponse
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class LegacyCreateCourtAppearanceTests : IntegrationTestBase() {

  @Test
  fun `create appearance in existing court case`() {
    val legacyCourtCase = createLegacyCourtCase()
    val legacyCourtAppearance = DataCreator.legacyCreateCourtAppearance(courtCaseUuid = legacyCourtCase.first)

    webTestClient
      .post()
      .uri("/legacy/court-appearance")
      .bodyValue(legacyCourtAppearance)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_APPEARANCE_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isCreated
      .expectBody()
      .jsonPath("$.lifetimeUuid")
      .value(MatchesPattern.matchesPattern("([a-f0-9]{8}(-[a-f0-9]{4}){4}[a-f0-9]{8})"))
    val message = getMessages(1)[0]
    Assertions.assertThat(message.eventType).isEqualTo("court-appearance.inserted")
    Assertions.assertThat(message.additionalInformation.get("source").asText()).isEqualTo("NOMIS")
  }

  @Test
  fun `create future dated appearance in existing court case`() {
    val legacyCourtCase = createLegacyCourtCase()
    val futureCourtAppearance = DataCreator.legacyCreateCourtAppearance(courtCaseUuid = legacyCourtCase.first, appearanceDate = LocalDate.now().plusDays(10), legacyData = DataCreator.courtAppearanceLegacyData(nextEventDateTime = null))
    webTestClient
      .post()
      .uri("/legacy/court-appearance")
      .bodyValue(futureCourtAppearance)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_APPEARANCE_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isCreated
      .expectBody()
      .jsonPath("$.lifetimeUuid")
      .value(MatchesPattern.matchesPattern("([a-f0-9]{8}(-[a-f0-9]{4}){4}[a-f0-9]{8})"))
    val message = getMessages(1)[0]
    Assertions.assertThat(message.eventType).isEqualTo("court-appearance.inserted")
    Assertions.assertThat(message.additionalInformation.get("source").asText()).isEqualTo("NOMIS")
  }

  @Test
  fun `create future dated appearance with existing appearance`() {
    val (appearanceUuid, legacyCourtAppearance) = createLegacyCourtAppearance()
    val futureCourtAppearance = DataCreator.legacyCreateCourtAppearance(courtCaseUuid = legacyCourtAppearance.courtCaseUuid, appearanceDate = legacyCourtAppearance.legacyData.nextEventDateTime!!.toLocalDate(), legacyData = DataCreator.courtAppearanceLegacyData(nextEventDateTime = null))
    webTestClient
      .post()
      .uri("/legacy/court-appearance")
      .bodyValue(futureCourtAppearance)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_APPEARANCE_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isCreated
      .expectBody()
      .jsonPath("$.lifetimeUuid")
      .value(MatchesPattern.matchesPattern("([a-f0-9]{8}(-[a-f0-9]{4}){4}[a-f0-9]{8})"))

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
      .jsonPath("$.appearances[?(@.appearanceUuid == '$appearanceUuid')].nextCourtAppearance.appearanceDate")
      .isEqualTo(futureCourtAppearance.appearanceDate.format(DateTimeFormatter.ISO_DATE))
      .jsonPath("$.appearances[?(@.appearanceUuid == '$appearanceUuid')].nextCourtAppearance.appearanceTime")
      .isEqualTo(futureCourtAppearance.legacyData.appearanceTime!!.format(DateTimeFormatter.ISO_LOCAL_TIME))

    val appearanceHistories = courtAppearanceHistoryRepository.findAll()
    val existingAppearanceHistories = appearanceHistories.filter { it.appearanceUuid == appearanceUuid }.sortedBy { it.updatedAt ?: it.createdAt }
    Assertions.assertThat(existingAppearanceHistories[0].nextCourtAppearanceId).isNull()
    Assertions.assertThat(existingAppearanceHistories[1].nextCourtAppearanceId).isNotNull
    assertThat(existingAppearanceHistories).extracting<EventSource> { it.source }.containsOnly(NOMIS)
  }

  @Test
  fun `use latest court case reference in appearance when creating`() {
    val nomisCaseReference = "NEW_NOMIS_CASE_REFERENCE"
    val (courtCaseUuid) = createLegacyCourtCase()
    val legacyData = CourtCaseLegacyData(
      mutableListOf(
        CaseReferenceLegacyData(nomisCaseReference, LocalDateTime.now()),
      ),
      1L,
    )
    webTestClient
      .put()
      .uri("/court-case/$courtCaseUuid/case-references/refresh")
      .bodyValue(legacyData)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_COURT_CASE_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isNoContent

    val legacyCourtAppearance = DataCreator.legacyCreateCourtAppearance(courtCaseUuid = courtCaseUuid)

    val response = webTestClient
      .post()
      .uri("/legacy/court-appearance")
      .bodyValue(legacyCourtAppearance)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_APPEARANCE_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isCreated
      .returnResult(LegacyCourtAppearanceCreatedResponse::class.java)
      .responseBody.blockFirst()!!

    webTestClient
      .get()
      .uri("/court-appearance/${response.lifetimeUuid}")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.courtCaseReference")
      .isEqualTo(nomisCaseReference)
  }

  @Test
  fun `must not create appearance when no court case exists`() {
    val legacyCourtAppearance = DataCreator.legacyCreateCourtAppearance()
    webTestClient
      .post()
      .uri("/legacy/court-appearance")
      .bodyValue(legacyCourtAppearance)
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
    val legacyCourtAppearance = DataCreator.legacyCreateCourtAppearance()
    webTestClient
      .post()
      .uri("/legacy/court-appearance")
      .bodyValue(legacyCourtAppearance)
      .headers {
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `token with incorrect role is forbidden`() {
    val legacyCourtAppearance = DataCreator.legacyCreateCourtAppearance()
    webTestClient
      .post()
      .uri("/legacy/court-appearance")
      .bodyValue(legacyCourtAppearance)
      .headers {
        it.authToken(roles = listOf("ROLE_OTHER_FUNCTION"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isForbidden
  }
}
