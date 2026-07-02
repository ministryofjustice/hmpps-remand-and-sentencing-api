package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.charge

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.AggravatingFactor
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.courtappearance.ChargeAggravatingFactorHelper
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.DpsDataCreator
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.DpsDataCreator.Factory.dpsCreateCourtAppearance
import java.time.LocalDate
import java.util.UUID

class UpdateChargeTests : IntegrationTestBase() {

  @Autowired
  private lateinit var jdbcTemplate: NamedParameterJdbcTemplate
  private val aggravatingFactors by lazy { ChargeAggravatingFactorHelper(jdbcTemplate) }

  @Test
  fun `update charge in existing appearance`() {
    val createCharge = DpsDataCreator.dpsCreateCharge()
    val createAppearance = dpsCreateCourtAppearance(charges = listOf(createCharge))
    createCourtCase(DpsDataCreator.dpsCreateCourtCase(appearances = listOf(createAppearance)))
    val updateCharge = createCharge.copy(offenceStartDate = LocalDate.now().minusDays(10), appearanceUuid = createAppearance.appearanceUuid)
    webTestClient
      .put()
      .uri("/charge/${createCharge.chargeUuid}")
      .bodyValue(updateCharge)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.chargeUuid")
      .value<String> {
        assertThat(it).matches("([a-f0-9]{8}(-[a-f0-9]{4}){4}[a-f0-9]{8})")
      }
    val messages = getMessages(1)
    assertThat(messages).hasSize(1).extracting<String> { it.eventType }.contains("charge.updated")
  }

  @Test
  fun `updating sentenced charge in specific appearance must keep sentence`() {
    val (courtCaseUuid, createdCourtCase) = createCourtCase()
    val createdAppearance = createdCourtCase.appearances.first()
    val createdSentencedCharge = createdAppearance.charges.first()

    val createRemandCharge = createdSentencedCharge.copy(outcomeUuid = UUID.fromString("315280e5-d53e-43b3-8ba6-44da25676ce2"), sentence = null)
    createCourtAppearance(dpsCreateCourtAppearance(courtCaseUuid = courtCaseUuid, charges = listOf(createRemandCharge)))

    val updateSentence = createdSentencedCharge.sentence!!.copy(convictionDate = createdSentencedCharge.sentence.convictionDate!!.minusDays(7))

    val updatedSentenceCharge = createdSentencedCharge.copy(appearanceUuid = createdAppearance.appearanceUuid, sentence = updateSentence)

    webTestClient
      .put()
      .uri("/charge/${updatedSentenceCharge.chargeUuid}")
      .bodyValue(updatedSentenceCharge)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isOk

    webTestClient
      .get()
      .uri("/court-appearance/${createdAppearance.appearanceUuid}")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.charges[?(@.chargeUuid == '${updatedSentenceCharge.chargeUuid}')].sentence.sentenceUuid")
      .isEqualTo(updateSentence.sentenceUuid.toString())
  }

  @Test
  fun `no token results in unauthorized`() {
    val updateCourtAppearance = dpsCreateCourtAppearance()
    val charge = updateCourtAppearance.charges.first()
    webTestClient
      .put()
      .uri("/charge/${charge.chargeUuid}")
      .bodyValue(charge)
      .headers {
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `token with incorrect role is forbidden`() {
    val updateCourtAppearance = dpsCreateCourtAppearance()
    val charge = updateCourtAppearance.charges.first()
    webTestClient
      .put()
      .uri("/charge/${charge.chargeUuid}")
      .bodyValue(charge)
      .headers {
        it.authToken(roles = listOf("ROLE_OTHER_FUNCTION"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `cannot update charge if appearance is deleted`() {
    val createCharge = DpsDataCreator.dpsCreateCharge()
    val createAppearance = dpsCreateCourtAppearance(charges = listOf(createCharge))
    createCourtCase(DpsDataCreator.dpsCreateCourtCase(appearances = listOf(createAppearance)))

    deleteCourtAppearance(createAppearance.appearanceUuid)

    val updateCharge = createCharge.copy(offenceStartDate = LocalDate.now().minusDays(10), appearanceUuid = createAppearance.appearanceUuid)
    webTestClient
      .put()
      .uri("/charge/${createCharge.chargeUuid}")
      .bodyValue(updateCharge)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isEqualTo(HttpStatus.CONFLICT)
  }

  @Test
  fun `should update charge when set terrorRelated adds OATC aggravating factor`() {
    val createCharge = DpsDataCreator.dpsCreateCharge()
    val createAppearance = dpsCreateCourtAppearance(charges = listOf(createCharge))
    createCourtCase(DpsDataCreator.dpsCreateCourtCase(appearances = listOf(createAppearance)))

    val updateCharge = createCharge.copy(
      aggravatingFactors = listOf(
        AggravatingFactor(
          "OATC",
          "Offence Aggravated by Terrorist Connection",
          description = "Offence Aggravated by Terrorist Connection",
          displayOrder = 10,
        ),
      ),
      offenceStartDate = LocalDate.now().minusDays(1),
      appearanceUuid = createAppearance.appearanceUuid,
    )
    webTestClient.put()
      .uri("/charge/${createCharge.chargeUuid}")
      .bodyValue(updateCharge)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus().isOk

    assertThat(aggravatingFactors.countAggravatingFactor(createCharge.chargeUuid, "OATC")).isEqualTo(1)
    assertThat(aggravatingFactors.countAggravatingFactor(createCharge.chargeUuid, "OAFPC")).isEqualTo(0)
  }

  @Test
  fun `should update charge when clear terrorRelated removes OATC aggravating factor`() {
    val createCharge = DpsDataCreator.dpsCreateCharge(
      aggravatingFactors = listOf(
        AggravatingFactor(
          "OATC",
          "Offence Aggravated by Terrorist Connection",
          description = "Offence Aggravated by Terrorist Connection",
          displayOrder = 10,
        ),
      ),
    )
    val createAppearance = dpsCreateCourtAppearance(charges = listOf(createCharge))
    createCourtCase(DpsDataCreator.dpsCreateCourtCase(appearances = listOf(createAppearance)))

    val updateCharge = createCharge.copy(
      aggravatingFactors = emptyList(),
      offenceStartDate = LocalDate.now().minusDays(1),
      appearanceUuid = createAppearance.appearanceUuid,
    )
    webTestClient.put()
      .uri("/charge/${createCharge.chargeUuid}")
      .bodyValue(updateCharge)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus().isOk

    assertThat(aggravatingFactors.countAggravatingFactor(createCharge.chargeUuid, "OATC")).isEqualTo(0)
  }

  @Test
  fun `should update charge when linked to multiple appearances preserves aggravating factors on new charge record`() {
    val charge = DpsDataCreator.dpsCreateCharge(
      aggravatingFactors = listOf(
        AggravatingFactor(
          "OATC",
          "Offence Aggravated by Terrorist Connection",
          description = "Offence Aggravated by Terrorist Connection",
          displayOrder = 10,
        ),
      ),
    )
    val firstAppearance = dpsCreateCourtAppearance(charges = listOf(charge))
    val secondAppearance = dpsCreateCourtAppearance(charges = listOf(charge))
    createCourtCase(DpsDataCreator.dpsCreateCourtCase(appearances = listOf(firstAppearance, secondAppearance)))

    val updateCharge = charge.copy(
      aggravatingFactors = listOf(
        AggravatingFactor(
          "OATC",
          "Offence Aggravated by Terrorist Connection",
          description = "Offence Aggravated by Terrorist Connection",
          displayOrder = 10,
        ),
        AggravatingFactor(
          "OAFPC",
          "Offence Aggravated by Foreign Power Connection",
          description = "Offence Aggravated by Foreign Power Connection",
          displayOrder = 20,
        ),
      ),
      offenceStartDate = LocalDate.now().minusDays(1),
      appearanceUuid = secondAppearance.appearanceUuid,
    )
    webTestClient.put()
      .uri("/charge/${charge.chargeUuid}")
      .bodyValue(updateCharge)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus().isOk

    assertThat(aggravatingFactors.countAggravatingFactorForLatestCharge("OATC")).isEqualTo(1)
    assertThat(aggravatingFactors.countAggravatingFactorForLatestCharge("OAFPC")).isEqualTo(1)
  }
}
