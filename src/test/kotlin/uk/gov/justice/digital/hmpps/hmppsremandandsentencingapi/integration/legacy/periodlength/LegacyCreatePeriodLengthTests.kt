package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.periodlength

import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.text.MatchesPattern
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.util.DataCreator

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
