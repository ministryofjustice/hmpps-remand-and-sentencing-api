package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.person

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.util.DataCreator

class GetCourtCaseCountForPersonTests : IntegrationTestBase() {

  @Test
  fun `get court case count for a person`() {
    val bookingId = 1L
    val courtCaseInBooking = DataCreator.migrationCreateCourtCase(courtCaseLegacyData = DataCreator.courtCaseLegacyData(bookingId = bookingId))
    val courtCaseInOtherBooking = DataCreator.migrationCreateCourtCase(caseId = 2, courtCaseLegacyData = DataCreator.courtCaseLegacyData(bookingId = bookingId + 1))
    val migrateCourtCases = DataCreator.migrationCreateCourtCases(courtCases = listOf(courtCaseInBooking, courtCaseInOtherBooking))
    migrateCases(migrateCourtCases)
    webTestClient.get()
      .uri {
        it.path("/person/${migrateCourtCases.prisonerId}/court-case-count")
          .queryParam("bookingId", bookingId)
          .build()
      }
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.suppliedBookingCount")
      .isEqualTo(1)
      .jsonPath("$.otherBookingCount")
      .isEqualTo(1)
  }

  @Test
  fun `get court case count for a person with no bookingId`() {
    val bookingId = 1L
    val courtCaseInBooking = DataCreator.migrationCreateCourtCase(courtCaseLegacyData = DataCreator.courtCaseLegacyData(bookingId = bookingId))
    val courtCaseInOtherBooking = DataCreator.migrationCreateCourtCase(caseId = 2, courtCaseLegacyData = DataCreator.courtCaseLegacyData(bookingId = bookingId + 1))
    val migrateCourtCases = DataCreator.migrationCreateCourtCases(courtCases = listOf(courtCaseInBooking, courtCaseInOtherBooking))
    migrateCases(migrateCourtCases)
    webTestClient.get()
      .uri("/person/${migrateCourtCases.prisonerId}/court-case-count")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING__REMAND_AND_SENTENCING_UI"))
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.suppliedBookingCount")
      .isEqualTo(0)
      .jsonPath("$.otherBookingCount")
      .isEqualTo(2)
  }
}
