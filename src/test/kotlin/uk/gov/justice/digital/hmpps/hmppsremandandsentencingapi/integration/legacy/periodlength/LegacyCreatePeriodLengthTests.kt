package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.periodlength

import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.text.MatchesPattern
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.util.DataCreator
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.PeriodLengthEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacyPeriodLengthCreatedResponse

class LegacyCreatePeriodLengthTests : IntegrationTestBase() {

  @Test
  fun `create period length`() {
    val sentenceLifetimeUuid = createLegacySentence().first
    val legacyCreatePeriodLength = DataCreator.legacyCreatePeriodLength(sentenceUUID = sentenceLifetimeUuid)
    webTestClient
      .post()
      .uri("/legacy/period-length")
      .bodyValue(legacyCreatePeriodLength)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_PERIOD_LENGTH_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isCreated
      .expectBody()
      .jsonPath("$.periodLengthUuid")
      .value(MatchesPattern.matchesPattern("([a-f0-9]{8}(-[a-f0-9]{4}){4}[a-f0-9]{8})"))
    val message = getMessages(1)[0]
    assertThat(message.eventType).isEqualTo("sentence.period-length.inserted")
    val periodLengthsAfterCreate = periodLengthRepository.findAllBySentenceEntitySentenceUuidAndStatusIdNot(sentenceLifetimeUuid)
    assertThat(periodLengthsAfterCreate).hasSize(1)
    assertThat(periodLengthsAfterCreate[0].statusId).isEqualTo(PeriodLengthEntityStatus.ACTIVE)
  }

  @Test
  fun `create period length when the sentence has a 'many charges' status then update the sentence to change the status`() {
    val (sentenceUuid, createdSentence) = createLegacySentenceWithManyCharges()
    val periodLength = DataCreator.legacyCreatePeriodLength(sentenceUUID = sentenceUuid)

    legacyCreatePeriodLength(periodLength)

    val periodLengths = periodLengthRepository.findAllBySentenceEntitySentenceUuidAndStatusIdNot(sentenceUuid)
    assertThat(periodLengths).hasSize(2)
    assertThat(periodLengths.map { it.statusId }).containsExactlyElementsOf(listOf(PeriodLengthEntityStatus.MANY_CHARGES_DATA_FIX, PeriodLengthEntityStatus.MANY_CHARGES_DATA_FIX))

    val singleChargeSentence = createdSentence.copy(chargeUuids = listOf(createdSentence.chargeUuids[0]))
    legacyUpdateSentence(sentenceUuid, singleChargeSentence)

    val periodLengthsAfter = periodLengthRepository.findAllBySentenceEntitySentenceUuidAndStatusIdNot(sentenceUuid)
    assertThat(periodLengthsAfter).hasSize(1)
    assertThat(periodLengthsAfter.map { it.statusId }).containsExactlyElementsOf(listOf(PeriodLengthEntityStatus.ACTIVE))
  }

  @Test
  fun `inactive period lengths are returned`() {
    val (sentenceUuid, toCreateSentence) = createLegacySentence(legacySentence = DataCreator.legacyCreateSentence(active = false))
    val periodLength = DataCreator.legacyCreatePeriodLength(sentenceUUID = sentenceUuid)
    val response = webTestClient
      .post()
      .uri("/legacy/period-length")
      .bodyValue(periodLength)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_PERIOD_LENGTH_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isCreated
      .returnResult(LegacyPeriodLengthCreatedResponse::class.java)
      .responseBody.blockFirst()!!
    val appearanceUuid = toCreateSentence.appearanceUuid
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
      .jsonPath("$.charges[0].sentence.periodLengths.[0].periodLengthUuid")
      .isEqualTo(response.periodLengthUuid.toString())
  }

  @Test
  fun `fails to create period length if no sentence exists`() {
    val legacyCreatePeriodLength = DataCreator.legacyCreatePeriodLength()
    webTestClient
      .post()
      .uri("/legacy/period-length")
      .bodyValue(legacyCreatePeriodLength)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_PERIOD_LENGTH_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isNotFound
  }

  @Test
  fun `no token results in unauthorized`() {
    val legacyCreatePeriodLength = DataCreator.legacyCreatePeriodLength()
    webTestClient
      .post()
      .uri("/legacy/period-length")
      .bodyValue(legacyCreatePeriodLength)
      .headers {
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `token with incorrect role is forbidden`() {
    val legacyCreatePeriodLength = DataCreator.legacyCreatePeriodLength()
    webTestClient
      .post()
      .uri("/legacy/period-length")
      .bodyValue(legacyCreatePeriodLength)
      .headers {
        it.authToken(roles = listOf("ROLE_OTHER_FUNCTION"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isForbidden
  }
}
