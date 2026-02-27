package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.admin

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.admin.RepublishEvents
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.EventMetadata
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.EventType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import java.util.*

class RepublishEventsTests : IntegrationTestBase() {

  @Test
  fun `republish failed events puts events on topic`() {
    val eventsToRepublish = RepublishEvents(
      eventsMetadata = listOf(
        EventMetadata(
          "PR123",
          UUID.randomUUID().toString(),
          null,
          null,
          null,
          null,
          EventType.COURT_CASE_INSERTED,
        ),
      ),
    )

    webTestClient.post()
      .uri("/event-admin/republish")
      .bodyValue(eventsToRepublish)
      .exchange()
      .expectStatus()
      .isNoContent
    val messages = getMessages(1)
    assertThat(messages).hasSize(1).extracting<String> { it.eventType }.contains("court-case.inserted")
  }
}
