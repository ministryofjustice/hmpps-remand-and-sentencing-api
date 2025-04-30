package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.periodlength

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.DpsDataCreator
import java.util.UUID

class LegacyGetPeriodLengthTests : IntegrationTestBase() {

  @Test
  fun `get period length by lifetime uuid`() {
    val sentencedAppearance = DpsDataCreator.dpsCreateCourtAppearance()
    val (_, createdCourtCase) = createCourtCase(DpsDataCreator.dpsCreateCourtCase(appearances = listOf(sentencedAppearance)))
    val periodLengthUuid = createdCourtCase.appearances.first().charges.first().sentence?.periodLengths?.first()?.periodLengthUuid

    webTestClient
      .get()
      .uri("/legacy/period-length/$periodLengthUuid")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_PERIOD_LENGTH_RO"))
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.periodLengthUuid")
      .isEqualTo(periodLengthUuid.toString())
  }

  @Test
  fun `no period-length exists (with a sentence associated) for uuid results in not found`() {
    val sentencedAppearance = DpsDataCreator.dpsCreateCourtAppearance()
    val periodLengthUuidWithNoSentence = sentencedAppearance.overallSentenceLength?.periodLengthUuid

    webTestClient
      .get()
      .uri("/legacy/period-length/$periodLengthUuidWithNoSentence")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_PERIOD_LENGTH_RO"))
      }
      .exchange()
      .expectStatus()
      .isNotFound
  }

  @Test
  fun `no period-length exists for uuid results in not found`() {
    webTestClient
      .get()
      .uri("/legacy/period-length/${UUID.randomUUID()}")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_PERIOD_LENGTH_RO"))
      }
      .exchange()
      .expectStatus()
      .isNotFound
  }

  @Test
  fun `no token results in unauthorized`() {
    val (_, createdCourtCase) = createCourtCase()
    val periodLengthUuid = createdCourtCase.appearances.first().charges.first().sentence?.periodLengths?.first()?.periodLengthUuid

    webTestClient
      .get()
      .uri("/legacy/period-length/$periodLengthUuid")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `token with incorrect role is forbidden`() {
    val (_, createdCourtCase) = createCourtCase()
    val periodLengthUuid = createdCourtCase.appearances.first().charges.first().sentence?.periodLengths?.first()?.periodLengthUuid

    webTestClient
      .get()
      .uri("/legacy/period-length/$periodLengthUuid")
      .headers {
        it.authToken(roles = listOf("ROLE_OTHER_FUNCTION"))
      }
      .exchange()
      .expectStatus()
      .isForbidden
  }
}
