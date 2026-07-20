package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.courtappearance

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.util.DataCreator
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.DpsDataCreator
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

class LegacyGetCourtAppearanceTests : IntegrationTestBase() {

  @Test
  fun `get appearance by lifetime uuid`() {
    val (lifetimeUuid, createdAppearance) = createLegacyCourtAppearance()
    val updateCourtSchedules = updateCourtSchedules(lifetimeUuid, DpsDataCreator.updateCourtAppearanceSchedule(courtCode = createdAppearance.courtCode, reasonCode = createdAppearance.legacyData.nomisAppearanceTypeCode!!, start = createdAppearance.appearanceDate.atTime(createdAppearance.legacyData.appearanceTime!!)))

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
      .jsonPath("$.appearanceTypeUuid")
      .isEqualTo("63e8fce0-033c-46ad-9edf-391b802d547a")
      .jsonPath("$.comments")
      .isEqualTo(updateCourtSchedules.comments!!)
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
  fun `get nomis appearance type code of subtype when it exists`() {
    val (_, createdCourtCase) = createCourtCase(purgeQueues = false)
    val courtAppearance = createdCourtCase.appearances.first()
    val courtAppearanceMessages = getMessages(7).filter { message -> message.eventType == "court-appearance.inserted" }
    val futureAppearanceUuid = courtAppearanceMessages.map { message -> message.additionalInformation.get("courtAppearanceId").asText() }.first { it != courtAppearance.appearanceUuid.toString() }
    val courtAppearanceSubType = courtAppearanceSubtypeRepository.findByAppearanceSubtypeUuid(courtAppearance.nextCourtAppearance!!.courtAppearanceSubtypeUuid!!)
    webTestClient
      .get()
      .uri("/legacy/court-appearance/$futureAppearanceUuid")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_APPEARANCE_RO"))
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.nomisAppearanceTypeCode")
      .isEqualTo(courtAppearanceSubType!!.nomisCode)
  }

  @Test
  fun `return merged charges`() {
    val sourceAppearance = DataCreator.migrationCreateCourtAppearance(eventId = 6)
    val sourceCourtCase = DataCreator.migrationCreateCourtCase(appearances = listOf(sourceAppearance), merged = true)
    val targetCharge = DataCreator.migrationCreateCharge(mergedFromCaseId = sourceCourtCase.caseId, mergedFromDate = LocalDate.now().minusDays(10L))
    val targetAppearance = DataCreator.migrationCreateCourtAppearance(eventId = 5, charges = listOf(targetCharge))
    val targetCourtCase = DataCreator.migrationCreateCourtCase(caseId = 2, appearances = listOf(targetAppearance))
    val courtCases = DataCreator.migrationCreateCourtCases(courtCases = listOf(sourceCourtCase, targetCourtCase))
    val response = migrateCases(courtCases)
    val appearanceUuid = response.appearances.first { it.eventId == sourceAppearance.eventId }.appearanceUuid
    val chargeUuid = response.charges.first { it.chargeNOMISId == sourceAppearance.charges.first().chargeNOMISId }.chargeUuid
    webTestClient
      .get()
      .uri("/legacy/court-appearance/$appearanceUuid")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_APPEARANCE_RO"))
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.charges[?(@.lifetimeUuid == '$chargeUuid')]")
      .exists()
  }

  @Test
  fun `no appearance exist for uuid results in not found`() {
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
}
