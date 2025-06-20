package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.sentencetype

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class GetIsSentenceTypeStillValidTests : IntegrationTestBase() {

  @Test
  fun `return true when sentence type is still valid for all parameters`() {
    val age = 25
    val convictionDate = LocalDate.parse("2020-12-15")
    val offenceDate = LocalDate.parse("2020-12-15")
    webTestClient.get()
      .uri(
        "/sentence-type/e138374d-810f-4718-a81a-1c9d4745031e/is-still-valid?age=$age&convictionDate=${convictionDate.format(DateTimeFormatter.ISO_DATE)}&offenceDate=${offenceDate.format(
          DateTimeFormatter.ISO_DATE,
        )}",
      )
      .headers { it.authToken() }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.isStillValid")
      .isEqualTo(true)
  }

  @Test
  fun `return false when sentence type is not still valid for all parameters`() {
    val age = 17
    val convictionDate = LocalDate.parse("2020-12-15")
    val offenceDate = LocalDate.parse("2020-12-15")
    webTestClient.get()
      .uri(
        "/sentence-type/e138374d-810f-4718-a81a-1c9d4745031e/is-still-valid?age=$age&convictionDate=${convictionDate.format(DateTimeFormatter.ISO_DATE)}&offenceDate=${offenceDate.format(
          DateTimeFormatter.ISO_DATE,
        )}",
      )
      .headers { it.authToken() }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.isStillValid")
      .isEqualTo(false)
  }
}
