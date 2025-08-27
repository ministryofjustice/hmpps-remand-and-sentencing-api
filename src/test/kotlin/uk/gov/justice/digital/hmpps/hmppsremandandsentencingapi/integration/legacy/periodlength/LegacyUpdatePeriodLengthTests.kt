package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.periodlength

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.repository.audit.PeriodLengthHistoryRepository

class LegacyUpdatePeriodLengthTests : IntegrationTestBase() {

  @Autowired
  private lateinit var periodLengthHistoryRepository: PeriodLengthHistoryRepository

  @Test
  fun `update period length`() {
    val (periodLengthUuid, periodLength) = createPeriodLength()
    val updatedPeriodLength = periodLength.copy(periodYears = 997)
    webTestClient
      .put()
      .uri("/legacy/period-length/$periodLengthUuid")
      .bodyValue(updatedPeriodLength)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_PERIOD_LENGTH_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isNoContent

    val message = getMessages(1)[0]
    assertThat(message.eventType).isEqualTo("sentence.period-length.updated")
    assertThat(message.additionalInformation.get("source").asText()).isEqualTo("NOMIS")
    val periodLengthAfterUpdate = periodLengthRepository.findFirstByPeriodLengthUuidOrderByUpdatedAtDesc(periodLengthUuid)
    assertThat(periodLengthAfterUpdate!!.years).isEqualTo(997)
    val periodLengthHistoryRecords = periodLengthHistoryRepository.findAll().filter { it.periodLengthUuid == periodLengthUuid }
    assertThat(periodLengthHistoryRecords).hasSize(2)
    assertThat(periodLengthHistoryRecords.map { it.years }).containsExactlyInAnyOrder(2, 997)
  }

  @Test
  fun `call to update period length when no changes are made`() {
    val (periodLengthUuid, periodLength) = createPeriodLength()
    webTestClient
      .put()
      .uri("/legacy/period-length/$periodLengthUuid")
      .bodyValue(periodLength)
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_PERIOD_LENGTH_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isNoContent

    getMessages(0)
    val periodLengthHistoryRecords = periodLengthHistoryRepository.findAll().filter { it.periodLengthUuid == periodLengthUuid }
    assertThat(periodLengthHistoryRecords).hasSize(1)
  }

  @Test
  fun `no token results in unauthorized`() {
    val (periodLengthUuid, periodLength) = createPeriodLength()
    webTestClient
      .put()
      .uri("/legacy/period-length/$periodLengthUuid")
      .bodyValue(periodLength)
      .headers {
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `token with incorrect role is forbidden`() {
    val (periodLengthUuid, periodLength) = createPeriodLength()
    webTestClient
      .put()
      .uri("/legacy/period-length/$periodLengthUuid")
      .bodyValue(periodLength)
      .headers {
        it.authToken(roles = listOf("ROLE_OTHER_FUNCTION"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isForbidden
  }
}
