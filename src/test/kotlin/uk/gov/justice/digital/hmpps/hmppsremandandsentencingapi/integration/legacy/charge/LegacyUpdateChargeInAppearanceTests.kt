package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.charge

import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.AggravatingFactor
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.courtappearance.ChargeAggravatingFactorHelper
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.util.DataCreator
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacyCourtAppearanceCreatedResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.MigrationCreateCourtCasesResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.DpsDataCreator
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

class LegacyUpdateChargeInAppearanceTests : IntegrationTestBase() {

  @Autowired
  private lateinit var jdbcTemplate: NamedParameterJdbcTemplate
  private val aggravatingFactors by lazy { ChargeAggravatingFactorHelper(jdbcTemplate) }

  @Test
  fun `update charge in existing court appearance`() {
    val (_, createdCourtCase) = createCourtCase()
    val appearance = createdCourtCase.appearances.first()
    val charge = appearance.charges.first()
    val toUpdate = DataCreator.legacyUpdateCharge(offenceCode = "ADIFFERENTCODE")
    webTestClient
      .put()
      .uri("/legacy/charge/${charge.chargeUuid}/appearance/${appearance.appearanceUuid}")
      .bodyValue(toUpdate)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_CHARGE_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isNoContent
    val message = getMessages(1)[0]
    Assertions.assertThat(message.eventType).isEqualTo("charge.updated")
    Assertions.assertThat(message.additionalInformation.get("source").asText()).isEqualTo("NOMIS")
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
      .jsonPath("$.charges[0].chargeUuid")
      .isEqualTo(charge.chargeUuid)
      .jsonPath("$.charges[0].offenceCode")
      .isEqualTo(toUpdate.offenceCode!!)
  }

  @Test
  fun `update charge in appearance when its linked to multiple appearances`() {
    val charge = DpsDataCreator.dpsCreateCharge()
    val firstAppearance = DpsDataCreator.dpsCreateCourtAppearance(charges = listOf(charge))
    val secondAppearance = DpsDataCreator.dpsCreateCourtAppearance(charges = listOf(charge))
    createCourtCase(DpsDataCreator.dpsCreateCourtCase(appearances = listOf(firstAppearance, secondAppearance)))
    val legacyUpdateCharge = DataCreator.legacyUpdateCharge()
    webTestClient
      .put()
      .uri("/legacy/charge/${charge.chargeUuid}/appearance/${secondAppearance.appearanceUuid}")
      .bodyValue(legacyUpdateCharge)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_CHARGE_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isNoContent

    webTestClient
      .get()
      .uri("/court-appearance/${firstAppearance.appearanceUuid}")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.charges[0].chargeUuid")
      .isEqualTo(charge.chargeUuid)
      .jsonPath("$.charges[0].outcome.outcomeUuid")
      .isEqualTo(charge.outcomeUuid.toString())

    webTestClient
      .get()
      .uri("/court-appearance/${secondAppearance.appearanceUuid}")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.charges[0].chargeUuid")
      .isEqualTo(charge.chargeUuid)
      .jsonPath("$.charges[0].legacyData.nomisOutcomeCode")
      .isEqualTo(legacyUpdateCharge.legacyData.nomisOutcomeCode!!)
  }

  @Test
  fun `update charge in old appearance`() {
    val charge = DataCreator.migrationCreateCharge(offenceStartDate = LocalDate.of(2024, 12, 1), offenceEndDate = LocalDate.of(2025, 8, 1), legacyData = DataCreator.chargeLegacyData(nomisOutcomeCode = "4565"))
    val firstAppearance = DataCreator.migrationCreateCourtAppearance(charges = listOf(charge), appearanceDate = LocalDate.of(2025, 8, 20), legacyData = DataCreator.courtAppearanceLegacyData(nextEventDateTime = LocalDateTime.of(2025, 10, 1, 0, 0)))
    val secondAppearance = DataCreator.migrationCreateCourtAppearance(eventId = 2, charges = listOf(charge), appearanceDate = firstAppearance.legacyData.nextEventDateTime!!.toLocalDate())
    val courtCase = DataCreator.migrationCreateCourtCase(appearances = listOf(firstAppearance, secondAppearance))
    val response = webTestClient
      .post()
      .uri("/legacy/court-case/migration")
      .bodyValue(DataCreator.migrationCreateCourtCases(courtCases = listOf(courtCase)))
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_COURT_CASE_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isCreated
      .returnResult(MigrationCreateCourtCasesResponse::class.java)
      .responseBody.blockFirst()!!

    val courtCaseUuid = response.courtCases.first().courtCaseUuid
    val secondAppearanceUuid = response.appearances.first { it.eventId == secondAppearance.eventId }.appearanceUuid
    val chargeUuid = response.charges.first().chargeUuid

    val thirdAppearance = DataCreator.legacyCreateCourtAppearance(courtCaseUuid = courtCaseUuid, appearanceDate = LocalDate.of(2025, 11, 26))
    val courtAppearanceCreatedResponse = webTestClient
      .post()
      .uri("/legacy/court-appearance")
      .bodyValue(thirdAppearance)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_APPEARANCE_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isCreated
      .returnResult(LegacyCourtAppearanceCreatedResponse::class.java)
      .responseBody.blockFirst()!!

    val legacyUpdateCharge = DataCreator.legacyUpdateCharge(
      offenceStartDate = charge.offenceStartDate,
      offenceEndDate = LocalDate.of(2025, 6, 30),
      legacyData = charge.legacyData,
    )

    webTestClient
      .put()
      .uri("/legacy/court-appearance/${courtAppearanceCreatedResponse.lifetimeUuid}/charge/$chargeUuid")
      .bodyValue(legacyUpdateCharge)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_APPEARANCE_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isNoContent

    webTestClient
      .put()
      .uri("/legacy/charge/$chargeUuid/appearance/$secondAppearanceUuid")
      .bodyValue(legacyUpdateCharge)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_CHARGE_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isNoContent

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
      .jsonPath("$.charges[?(@.chargeUuid == '$chargeUuid')].offenceEndDate")
      .isEqualTo(legacyUpdateCharge.offenceEndDate!!.format(DateTimeFormatter.ISO_DATE))
  }

  @Test
  fun `must not update appearance when no court appearance exists`() {
    val toUpdate = DataCreator.legacyUpdateCharge()
    webTestClient
      .put()
      .uri("/legacy/charge/${UUID.randomUUID()}/appearance/${UUID.randomUUID()}")
      .bodyValue(toUpdate)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_CHARGE_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isNotFound
  }

  @Test
  fun `no token results in unauthorized`() {
    val toUpdate = DataCreator.legacyUpdateCharge()
    webTestClient
      .put()
      .uri("/legacy/charge/${UUID.randomUUID()}/appearance/${UUID.randomUUID()}")
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
    val toUpdate = DataCreator.legacyUpdateCharge()
    webTestClient
      .put()
      .uri("/legacy/charge/${UUID.randomUUID()}/appearance/${UUID.randomUUID()}")
      .bodyValue(toUpdate)
      .headers {
        it.authToken(roles = listOf("ROLE_OTHER_FUNCTION"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `should preserve OATC aggravating factor on single appearance charge`() {
    val dpsCharge = DpsDataCreator.dpsCreateCharge(
      aggravatingFactors = listOf(
        AggravatingFactor(
          code = "OATC",
          title = "Offence Aggravated by Terrorist Connection",
          description = "Offence Aggravated by Terrorist Connection",
          displayOrder = 10,
        ),
      ),
      sentence = null,
    )
    val appearance = DpsDataCreator.dpsCreateCourtAppearance(charges = listOf(dpsCharge))
    createCourtCase(DpsDataCreator.dpsCreateCourtCase(appearances = listOf(appearance)))

    val legacyUpdate = DataCreator.legacyUpdateCharge(offenceStartDate = LocalDate.now().minusDays(5))
    webTestClient.put()
      .uri("/legacy/charge/${dpsCharge.chargeUuid}/appearance/${appearance.appearanceUuid}")
      .bodyValue(legacyUpdate)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_CHARGE_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus().isNoContent

    assertThat(aggravatingFactors.countAggravatingFactor(dpsCharge.chargeUuid, "OATC")).isEqualTo(1)
    assertThat(aggravatingFactors.countAggravatingFactor(dpsCharge.chargeUuid, "OAFPC")).isEqualTo(0)
  }

  @Test
  fun `should preserve OAFPC aggravating factor on single appearance charge`() {
    val dpsCharge = DpsDataCreator.dpsCreateCharge(
      aggravatingFactors = listOf(
        AggravatingFactor(
          code = "OAFPC",
          title = "Offence Aggravated by Foreign Power",
          description = "Offence Aggravated by Foreign Power",
          displayOrder = 10,
        ),
      ),
      sentence = null,
    )
    val appearance = DpsDataCreator.dpsCreateCourtAppearance(charges = listOf(dpsCharge))
    createCourtCase(DpsDataCreator.dpsCreateCourtCase(appearances = listOf(appearance)))

    val legacyUpdate = DataCreator.legacyUpdateCharge(offenceStartDate = LocalDate.now().minusDays(5))
    webTestClient.put()
      .uri("/legacy/charge/${dpsCharge.chargeUuid}/appearance/${appearance.appearanceUuid}")
      .bodyValue(legacyUpdate)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_CHARGE_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus().isNoContent

    assertThat(aggravatingFactors.countAggravatingFactor(dpsCharge.chargeUuid, "OATC")).isEqualTo(0)
    assertThat(aggravatingFactors.countAggravatingFactor(dpsCharge.chargeUuid, "OAFPC")).isEqualTo(1)
  }

  @Test
  fun `should preserve on charge in multiple appearances aggravating factors on new charge record`() {
    val dpsCharge = DpsDataCreator.dpsCreateCharge(
      aggravatingFactors = listOf(
        AggravatingFactor(
          code = "OATC",
          title = "Offence Aggravated by Terrorist Connection",
          description = "Offence Aggravated by Terrorist Connection",
          displayOrder = 10,
        ),
        AggravatingFactor(
          code = "OAFPC",
          title = "Offence Aggravated by Foreign Power",
          description = "Offence Aggravated by Foreign Power",
          displayOrder = 10,
        ),
      ),
      sentence = null,
    )
    val firstAppearance = DpsDataCreator.dpsCreateCourtAppearance(charges = listOf(dpsCharge))
    val secondAppearance = DpsDataCreator.dpsCreateCourtAppearance(charges = listOf(dpsCharge))
    createCourtCase(DpsDataCreator.dpsCreateCourtCase(appearances = listOf(firstAppearance, secondAppearance)))

    val legacyUpdate = DataCreator.legacyUpdateCharge(offenceStartDate = LocalDate.now().minusDays(5))
    webTestClient.put()
      .uri("/legacy/charge/${dpsCharge.chargeUuid}/appearance/${secondAppearance.appearanceUuid}")
      .bodyValue(legacyUpdate)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_CHARGE_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus().isNoContent

    assertThat(aggravatingFactors.countAggravatingFactorForLatestCharge("OATC")).isEqualTo(1)
    assertThat(aggravatingFactors.countAggravatingFactorForLatestCharge("OAFPC")).isEqualTo(1)
  }
}
