package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.sentence

import org.assertj.core.api.Assertions
import org.hamcrest.text.MatchesPattern
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.util.DataCreator
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacySentenceCreatedResponse
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.DpsDataCreator

class LegacyCreateSentenceTests : IntegrationTestBase() {

  @Test
  fun `create sentence in existing court case`() {
    val (chargeLifetimeUuid) = createLegacyCharge()
    val legacySentence = DataCreator.legacyCreateSentence(chargeLifetimeUuid = chargeLifetimeUuid)

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
      .value(MatchesPattern.matchesPattern("([a-f0-9]{8}(-[a-f0-9]{4}){4}[a-f0-9]{8})"))
    val message = getMessages(1)[0]
    Assertions.assertThat(message.eventType).isEqualTo("sentence.inserted")
    Assertions.assertThat(message.additionalInformation.get("source").asText()).isEqualTo("NOMIS")
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
    val charge = courtCaseCreated.appearances.first().charges.first()
    val legacySentence = DataCreator.legacyCreateSentence(chargeLifetimeUuid = charge.chargeUuid, sentenceLegacyData = DataCreator.sentenceLegacyData(sentenceCalcType = "FTR_ORA"))
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
  }

  @Test
  fun `must not be able to create a sentence on a already sentenced charge`() {
    val (_, sentence) = createLegacySentence()
    val legacySentence = DataCreator.legacyCreateSentence(chargeLifetimeUuid = sentence.chargeLifetimeUuid)
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
      .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY)
  }

  @Test
  fun `must be able to create a consecutive to sentence`() {
    val (lifetimeUuid) = createLegacySentence()
    val (chargeLifetimeUuid) = createLegacyCharge(
      legacyCreateCourtAppearance = DataCreator.legacyCreateCourtAppearance(
        legacyData = DataCreator.courtAppearanceLegacyData(
          outcomeConvictionFlag = true,
          outcomeDispositionCode = "F",
        ),
      ),
    )
    val legacySentence = DataCreator.legacyCreateSentence(chargeLifetimeUuid = chargeLifetimeUuid, consecutiveToLifetimeUuid = lifetimeUuid)
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
}
