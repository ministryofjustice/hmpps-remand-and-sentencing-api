package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.immigrationdetention

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.ImmigrationDetention
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.event.EventSource
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ImmigrationDetentionRecordType.IS91
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.UUID

class GetImmigrationByCourtAppearanceTests : IntegrationTestBase() {

  @Test
  fun `get immigration by court appearance`() {
    val courtAppearanceUuid = createNomisImmigrationDetentionCourtCase(prisonerId = "B12345B", "5500")

    val response = webTestClient
      .get()
      .uri("/immigration-detention/court-appearance/$courtAppearanceUuid")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_SENTENCING__IMMIGRATION_DETENTION_RW"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody(ImmigrationDetention::class.java)
      .returnResult().responseBody!!

    assertThat(response)
      .usingRecursiveComparison()
      .ignoringFields("createdAt", "immigrationDetentionUuid")
      .isEqualTo(
        ImmigrationDetention(
          immigrationDetentionUuid = UUID.randomUUID(),
          courtAppearanceUuid = courtAppearanceUuid,
          prisonerId = "B12345B",
          immigrationDetentionRecordType = IS91,
          recordDate = LocalDate.now(),
          createdAt = ZonedDateTime.now(),
          source = EventSource.NOMIS,
        ),
      )
  }
}
