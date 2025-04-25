package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.courtappearance

import org.junit.jupiter.api.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import java.time.format.DateTimeFormatter
import java.util.UUID

class LegacyGetCourtAppearanceTests : IntegrationTestBase() {

  @Test
  fun `get appearance by lifetime uuid`() {
    val (lifetimeUuid, createdAppearance) = createLegacyCourtAppearance()

    webTestClient
      .get()
      .uri("/legacy/court-appearance/$lifetimeUuid")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_APPEARANCE_RO"))
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.lifetimeUuid")
      .isEqualTo(lifetimeUuid.toString())
      .jsonPath("$.appearanceDate")
      .isEqualTo(createdAppearance.appearanceDate.format(DateTimeFormatter.ISO_DATE))
      .jsonPath("$.nomisOutcomeCode")
      .isEqualTo(createdAppearance.legacyData.nomisOutcomeCode!!)
  }

  @Test
  fun `can get appearance and future appearance when DPS next court appearance is set`() {
    createCourtCase(purgeQueues = false)
    val courtAppearanceMessages = getMessages(7).filter { message -> message.eventType == "court-appearance.inserted" }
    val courtAppearanceLifetimeUuid = courtAppearanceMessages.map { message -> message.additionalInformation.get("courtAppearanceId").asText() }
    courtAppearanceLifetimeUuid.forEach { lifetimeUuid ->
      webTestClient
        .get()
        .uri("/legacy/court-appearance/$lifetimeUuid")
        .headers {
          it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_APPEARANCE_RO"))
        }
        .exchange()
        .expectStatus()
        .isOk
    }
  }

  @Test
  fun `no appearance exist for uuid results in not found`() {
    log.info("1  MESSAGES >>>>>>>>>>>>>>>>> : ")
    webTestClient
      .get()
      .uri("/legacy/court-appearance/${UUID.randomUUID()}")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_APPEARANCE_RO"))
      }
      .exchange()
      .expectStatus()
      .isNotFound
  }

  @Test
  fun `no token results in unauthorized`() {
    val createdAppearance = createCourtCase().second.appearances.first()
    webTestClient
      .get()
      .uri("/legacy/court-appearance/${createdAppearance.appearanceUuid}")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `token with incorrect role is forbidden`() {
    val createdAppearance = createCourtCase().second.appearances.first()
    webTestClient
      .get()
      .uri("/legacy/court-appearance/${createdAppearance.appearanceUuid}")
      .headers {
        it.authToken(roles = listOf("ROLE_OTHER_FUNCTION"))
      }
      .exchange()
      .expectStatus()
      .isForbidden
  }

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }
}
