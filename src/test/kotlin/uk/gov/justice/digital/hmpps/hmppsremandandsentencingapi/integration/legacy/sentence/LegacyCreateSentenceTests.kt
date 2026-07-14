package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.sentence

import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers.everyItem
import org.hamcrest.core.IsNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.AggravatingFactor
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.event.EventSource
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.courtappearance.ChargeAggravatingFactorHelper
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.util.DataCreator
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.wiremock.AdjustmentsApiExtension.Companion.adjustmentsApi
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.RecallType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacySentenceCreatedResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.MigrationCreateCourtCasesResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.DpsDataCreator
import java.time.LocalDate

class LegacyCreateSentenceTests : IntegrationTestBase() {

  @Autowired
  private lateinit var jdbcTemplate: NamedParameterJdbcTemplate
  private val aggravatingFactors by lazy { ChargeAggravatingFactorHelper(jdbcTemplate) }

  @Test
  fun `create sentence in existing court case`() {
    val (chargeLifetimeUuid, createdCharge) = createLegacyCharge()
    val legacySentence = DataCreator.legacyCreateSentence(chargeUuids = listOf(chargeLifetimeUuid), appearanceUuid = createdCharge.appearanceLifetimeUuid)

    webTestClient
      .post()
      .uri("/legacy/sentence")
      .bodyValue(legacySentence)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_SENTENCE_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isCreated
      .expectBody()
      .jsonPath("$.lifetimeUuid")
      .value<String> {
        assertThat(it).matches("([a-f0-9]{8}(-[a-f0-9]{4}){4}[a-f0-9]{8})")
      }
    val message = getMessages(1)[0]
    assertThat(message.eventType).isEqualTo("sentence.inserted")
    assertThat(message.additionalInformation.get("source").asText()).isEqualTo("NOMIS")
  }

  @Test
  fun `creating a sentence with recall sentence type uses the DPS recall sentence type bucket`() {
    val (_, courtCaseCreated) = createCourtCase(
      DpsDataCreator.dpsCreateCourtCase(
        appearances = listOf(
          DpsDataCreator.dpsCreateCourtAppearance(charges = listOf(DpsDataCreator.dpsCreateCharge(sentence = null))),
        ),
      ),
    )
    val appearance = courtCaseCreated.appearances.first()
    val charge = appearance.charges.first()
    val legacySentence = DataCreator.legacyCreateSentence(chargeUuids = listOf(charge.chargeUuid), appearanceUuid = appearance.appearanceUuid, sentenceLegacyData = DataCreator.sentenceLegacyData(sentenceCalcType = "FTR_ORA", sentenceCategory = "2020"), returnToCustodyDate = LocalDate.of(2024, 1, 1))

    purgeQueues()

    webTestClient
      .post()
      .uri("/legacy/sentence")
      .bodyValue(legacySentence)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_SENTENCE_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isCreated

    webTestClient
      .get()
      .uri("/charge/${charge.chargeUuid}")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING"))
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.sentence.sentenceType.sentenceTypeUuid")
      .isEqualTo("f9a1551e-86b1-425b-96f7-23465a0f05fc")

    adjustmentsApi.stubGetAdjustmentsDefaultToNone()
    val recalls = getRecallsByPrisonerId(DpsDataCreator.DEFAULT_PRISONER_ID)
    assertThat(recalls).hasSize(1)
    assertThat(recalls[0].recallType).isEqualTo(RecallType.FTR_28)
    assertThat(recalls[0].courtCases[0].sentences).hasSize(1)
    assertThat(recalls[0].returnToCustodyDate).isEqualTo(LocalDate.of(2024, 1, 1))
    assertThat(recalls[0].source).isEqualTo(EventSource.NOMIS)

    val messages = getMessages(2)
    assertThat(messages).extracting<String> { it.eventType }
      .contains("sentence.inserted", "recall.inserted")

    val sentenceInserted = messages.first { it.eventType == "sentence.inserted" }
    assertThat(sentenceInserted.additionalInformation.get("source").asText()).isEqualTo("NOMIS")

    val recallInserted = messages.first { it.eventType == "recall.inserted" }
    assertThat(recallInserted.additionalInformation.get("source").asText()).isEqualTo("NOMIS")
  }

  @Test
  fun `creating a sentence with FTR 56 recall sentence type uses the DPS recall sentence type bucket`() {
    val (_, courtCaseCreated) = createCourtCase(
      DpsDataCreator.dpsCreateCourtCase(
        appearances = listOf(
          DpsDataCreator.dpsCreateCourtAppearance(charges = listOf(DpsDataCreator.dpsCreateCharge(sentence = null))),
        ),
      ),
    )
    val appearance = courtCaseCreated.appearances.first()
    val charge = appearance.charges.first()
    val legacySentence = DataCreator.legacyCreateSentence(chargeUuids = listOf(charge.chargeUuid), appearanceUuid = appearance.appearanceUuid, sentenceLegacyData = DataCreator.sentenceLegacyData(sentenceCalcType = "FTR_56ORA", sentenceCategory = "2020"), returnToCustodyDate = LocalDate.of(2024, 1, 1))
    webTestClient
      .post()
      .uri("/legacy/sentence")
      .bodyValue(legacySentence)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_SENTENCE_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isCreated

    webTestClient
      .get()
      .uri("/charge/${charge.chargeUuid}")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING"))
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.sentence.sentenceType.sentenceTypeUuid")
      .isEqualTo("f9a1551e-86b1-425b-96f7-23465a0f05fc")

    adjustmentsApi.stubGetAdjustmentsDefaultToNone()
    val recalls = getRecallsByPrisonerId(DpsDataCreator.DEFAULT_PRISONER_ID)
    assertThat(recalls).hasSize(1)
    assertThat(recalls[0].recallType).isEqualTo(RecallType.FTR_56)
    assertThat(recalls[0].courtCases[0].sentences).hasSize(1)
    assertThat(recalls[0].returnToCustodyDate).isEqualTo(LocalDate.of(2024, 1, 1))
    assertThat(recalls[0].source).isEqualTo(EventSource.NOMIS)
  }

  @ParameterizedTest
  @ValueSource(
    strings = [
      "ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI",
      "ROLE_REMAND_AND_SENTENCING__CCRD__RO",
      "ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI,ROLE_REMAND_AND_SENTENCING__CCRD__RO",
    ],
  )
  fun `inactive sentences are returned`(roleCsv: String) {
    val roles = roleCsv.split(",")
    val (chargeLifetimeUuid, toCreateCharge) = createLegacyCharge()
    val legacySentence = DataCreator.legacyCreateSentence(chargeUuids = listOf(chargeLifetimeUuid), appearanceUuid = toCreateCharge.appearanceLifetimeUuid, active = false)
    val response = webTestClient
      .post()
      .uri("/legacy/sentence")
      .bodyValue(legacySentence)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_SENTENCE_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isCreated.returnResult(LegacySentenceCreatedResponse::class.java)
      .responseBody.blockFirst()!!

    val appearanceUuid = toCreateCharge.appearanceLifetimeUuid.toString()

    webTestClient
      .get()
      .uri("/court-appearance/$appearanceUuid")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.charges[0].sentence.sentenceUuid")
      .isEqualTo(response.lifetimeUuid.toString())

    webTestClient
      .get()
      .uri { uriBuilder ->
        uriBuilder.path("/sentence/consecutive-to-details")
          .queryParam("sentenceUuids", response.lifetimeUuid.toString())
          .build()
      }
      .headers {
        it.authToken(roles = roles)
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.sentences[0].sentenceUuid")
      .isEqualTo(response.lifetimeUuid.toString())
  }

  @Test
  fun `must be able to add a sentence to a charge which is associated to a recall appearance`() {
    val (chargeLifetimeUuid, toCreateCharge) = createLegacyCharge(
      legacyCreateCourtAppearance = DataCreator.legacyCreateCourtAppearance(legacyData = DataCreator.courtAppearanceLegacyData(nomisOutcomeCode = "1501")),
    )

    val legacySentence = DataCreator.legacyCreateSentence(chargeUuids = listOf(chargeLifetimeUuid), appearanceUuid = toCreateCharge.appearanceLifetimeUuid)

    webTestClient
      .post()
      .uri("/legacy/sentence")
      .bodyValue(legacySentence)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_SENTENCE_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isCreated
  }

  @Test
  fun `must not be able to create a sentence on a already sentenced charge`() {
    val (_, sentence) = createLegacySentence()
    val legacySentence = DataCreator.legacyCreateSentence(chargeUuids = listOf(sentence.chargeUuids.first()), appearanceUuid = sentence.appearanceUuid)
    webTestClient
      .post()
      .uri("/legacy/sentence")
      .bodyValue(legacySentence)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_SENTENCE_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT)
  }

  @Test
  fun `must be able to create a consecutive to sentence`() {
    val (lifetimeUuid) = createLegacySentence()
    val (chargeLifetimeUuid, toCreateCharge) = createLegacyCharge(
      legacyCreateCourtAppearance = DataCreator.legacyCreateCourtAppearance(
        legacyData = DataCreator.courtAppearanceLegacyData(
          outcomeConvictionFlag = true,
          outcomeDispositionCode = "F",
        ),
      ),
    )
    val legacySentence = DataCreator.legacyCreateSentence(chargeUuids = listOf(chargeLifetimeUuid), appearanceUuid = toCreateCharge.appearanceLifetimeUuid, consecutiveToLifetimeUuid = lifetimeUuid)
    val response = webTestClient
      .post()
      .uri("/legacy/sentence")
      .bodyValue(legacySentence)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_SENTENCE_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isCreated.returnResult(LegacySentenceCreatedResponse::class.java)
      .responseBody.blockFirst()!!

    webTestClient
      .get()
      .uri("/legacy/sentence/${response.lifetimeUuid}")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_SENTENCE_RO"))
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.consecutiveToLifetimeUuid")
      .isEqualTo(lifetimeUuid.toString())
  }

  @Test
  fun `able to create sentence in specific appearance`() {
    val chargeNOMISId = 555L
    val charge = DataCreator.migrationCreateCharge(chargeNOMISId = chargeNOMISId, legacyData = DataCreator.chargeLegacyData(nomisOutcomeCode = "99"), offenceEndDate = LocalDate.now().plusDays(5), sentence = null)
    val firstAppearance = DataCreator.migrationCreateCourtAppearance(eventId = 1, appearanceDate = LocalDate.now().minusDays(7), legacyData = DataCreator.courtAppearanceLegacyData(), charges = listOf(charge))
    val secondAppearance = DataCreator.migrationCreateCourtAppearance(eventId = 2, appearanceDate = LocalDate.now().minusDays(2), legacyData = DataCreator.courtAppearanceLegacyData(), charges = listOf(charge))
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
    val courtCaseUuid = response.courtCases.first().courtCaseUuid
    val chargeUuid = response.charges.first().chargeUuid
    val firstAppearanceUuid = response.appearances.first { appearanceResponse -> firstAppearance.eventId == appearanceResponse.eventId }.appearanceUuid
    val secondAppearanceUuid = response.appearances.first { appearanceResponse -> secondAppearance.eventId == appearanceResponse.eventId }.appearanceUuid

    val legacySentence = DataCreator.legacyCreateSentence(
      chargeUuids = listOf(chargeUuid),
      appearanceUuid = secondAppearanceUuid,
    )
    webTestClient
      .post()
      .uri("/legacy/sentence")
      .bodyValue(legacySentence)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_SENTENCE_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isCreated

    webTestClient
      .get()
      .uri("/court-case/$courtCaseUuid")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.appearances[?(@.appearanceUuid == '$firstAppearanceUuid')].charges[?(@.chargeUuid == '$chargeUuid')].sentence")
      .value(everyItem(IsNull.nullValue()))
      .jsonPath("$.appearances[?(@.appearanceUuid == '$secondAppearanceUuid')].charges[?(@.chargeUuid == '$chargeUuid')].sentence")
      .value(everyItem(IsNull.notNullValue()))
  }

  @Test
  fun `must not sentence when no charge exists`() {
    val legacySentence = DataCreator.legacyCreateSentence()
    webTestClient
      .post()
      .uri("/legacy/sentence")
      .bodyValue(legacySentence)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_SENTENCE_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isNotFound
  }

  @Test
  fun `no token results in unauthorized`() {
    val legacySentence = DataCreator.legacyCreateSentence()
    webTestClient
      .post()
      .uri("/legacy/sentence")
      .bodyValue(legacySentence)
      .headers {
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `token with incorrect role is forbidden`() {
    val legacySentence = DataCreator.legacyCreateSentence()
    webTestClient
      .post()
      .uri("/legacy/sentence")
      .bodyValue(legacySentence)
      .headers {
        it.authToken(roles = listOf("ROLE_OTHER_FUNCTION"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `should preserve aggravating factors on new charge record when creating sentence on charge in multiple appearances`() {
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

    val legacySentence = DataCreator.legacyCreateSentence(
      chargeUuids = listOf(dpsCharge.chargeUuid),
      appearanceUuid = secondAppearance.appearanceUuid,
    )
    webTestClient
      .post()
      .uri("/legacy/sentence")
      .bodyValue(legacySentence)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_SENTENCE_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isCreated

    // Act
    webTestClient
      .get()
      .uri("/charge/${dpsCharge.chargeUuid}")
      .headers { it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING")) }
      .exchange()
      .expectStatus().isOk

    assertThat(aggravatingFactors.countAggravatingFactorForLatestCharge("OATC")).isEqualTo(1)
    assertThat(aggravatingFactors.countAggravatingFactorForLatestCharge("OAFPC")).isEqualTo(1)
  }
}
