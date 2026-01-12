package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.courtcase

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.DpsDataCreator
import java.time.format.DateTimeFormatter
import java.util.UUID

class LegacyGetCourtCaseTests : IntegrationTestBase() {

  @Test
  fun `get legacy court case by uuid`() {
    val createdCase = createCourtCase()
    webTestClient
      .get()
      .uri("/legacy/court-case/${createdCase.first}")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_COURT_CASE_RO"))
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.courtCaseUuid")
      .isEqualTo(createdCase.first)
      .jsonPath("$.prisonerId")
      .isEqualTo(createdCase.second.prisonerId)
      .jsonPath("$.active")
      .isEqualTo(true)
  }

  @Test
  fun `get immigration detention court case`() {
    val createImmigrationDetention = DpsDataCreator.dpsCreateImmigrationDetention()
    createImmigrationDetention(createImmigrationDetention)
    val courtCase = courtCaseRepository.findAllByPrisonerId(createImmigrationDetention.prisonerId).first()
    webTestClient
      .get()
      .uri("/legacy/court-case/${courtCase.caseUniqueIdentifier}")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_COURT_CASE_RO"))
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.startDate")
      .isEqualTo(createImmigrationDetention.recordDate.format(DateTimeFormatter.ISO_DATE))
      .jsonPath("$.courtId")
      .isEqualTo("IMM")
  }

  @Test
  fun `no court case exist for uuid results in not found`() {
    webTestClient
      .get()
      .uri("/legacy/court-case/${UUID.randomUUID()}")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_COURT_CASE_RO"))
      }
      .exchange()
      .expectStatus()
      .isNotFound
  }

  @Test
  fun `no token results in unauthorized`() {
    val createdCase = createCourtCase()
    webTestClient
      .get()
      .uri("/legacy/court-case/${createdCase.first}")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `token with incorrect role is forbidden`() {
    val createdCase = createCourtCase()
    webTestClient
      .get()
      .uri("/legacy/court-case/${createdCase.first}")
      .headers {
        it.authToken(roles = listOf("ROLE_OTHER_FUNCTION"))
      }
      .exchange()
      .expectStatus()
      .isForbidden
  }
}
