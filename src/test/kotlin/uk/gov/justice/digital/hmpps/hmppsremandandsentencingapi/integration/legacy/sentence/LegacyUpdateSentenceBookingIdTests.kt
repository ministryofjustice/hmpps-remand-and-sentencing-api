package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.sentence

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.util.DataCreator
import java.util.*

class LegacyUpdateSentenceBookingIdTests : IntegrationTestBase() {

  @Test
  fun `update booking id`() {
    val (lifetimeUuid) = createLegacySentence()
    val toUpdate = DataCreator.legacyUpdateSentenceBookingId()
    webTestClient
      .put()
      .uri("/legacy/sentence/$lifetimeUuid/booking-id")
      .bodyValue(toUpdate)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_SENTENCE_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isNoContent
    val sentenceRecords = sentenceRepository.findBySentenceUuid(lifetimeUuid)
    Assertions.assertThat(sentenceRecords).allMatch {
      it.legacyData!!.bookingId == toUpdate.bookingId
    }
  }

  @Test
  fun `no token results in unauthorized`() {
    val toUpdate = DataCreator.legacyUpdateSentenceBookingId()
    webTestClient
      .put()
      .uri("/legacy/sentence/${UUID.randomUUID()}/booking-id")
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
    val toUpdate = DataCreator.legacyUpdateSentenceBookingId()
    webTestClient
      .put()
      .uri("/legacy/sentence/${UUID.randomUUID()}/booking-id")
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
