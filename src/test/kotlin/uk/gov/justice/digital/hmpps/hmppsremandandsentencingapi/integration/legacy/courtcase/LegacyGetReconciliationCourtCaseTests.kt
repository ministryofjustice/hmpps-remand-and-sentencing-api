package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.courtcase

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.RecallType.LR
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.DpsDataCreator
import java.time.format.DateTimeFormatter
import java.util.UUID

class LegacyGetReconciliationCourtCaseTests : IntegrationTestBase() {

  @Test
  fun `get reconciliation court case by uuid`() {
    val (uuid, createdCase) = createCourtCase()
    val appearance = createdCase.appearances.first()
    val nextAppearance = appearance.nextCourtAppearance!!
    webTestClient
      .get()
      .uri("/legacy/court-case/$uuid/reconciliation")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_COURT_CASE_RO"))
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.courtCaseUuid")
      .isEqualTo(uuid)
      .jsonPath("$.prisonerId")
      .isEqualTo(createdCase.prisonerId)
      .jsonPath("$.active")
      .isEqualTo(true)
      .jsonPath("$.appearances[?(@.appearanceDate == '${appearance.appearanceDate.format(DateTimeFormatter.ISO_DATE)}')]")
      .exists()
      .jsonPath("$.appearances[?(@.appearanceDate == '${nextAppearance.appearanceDate.format(DateTimeFormatter.ISO_DATE)}')]")
      .exists()
  }

  @Test
  fun `get reconciliation court case with recall`() {
    val (uuid, createdCase) = createCourtCase()
    val appearance = createdCase.appearances.first()
    val sentence = appearance.charges[0].sentence!!
    val recall = createRecall(
      DpsDataCreator.dpsCreateRecall(
        recallTypeCode = LR,
        sentenceIds = listOf(
          sentence.sentenceUuid!!,
        ),
      ),
    )

    webTestClient
      .get()
      .uri("/legacy/court-case/$uuid/reconciliation")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_AND_SENTENCING_COURT_CASE_RO"))
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.courtCaseUuid")
      .isEqualTo(uuid)
      .jsonPath("$.prisonerId")
      .isEqualTo(createdCase.prisonerId)
      .jsonPath("$.active")
      .isEqualTo(true)
      .jsonPath("$.appearances[*].charges[*].sentence.sentenceCalcType")
      .isEqualTo("LRSEC250_ORA")
  }

  @Test
  fun `no court case exist for uuid results in not found`() {
    webTestClient
      .get()
      .uri("/legacy/court-case/${UUID.randomUUID()}/reconciliation")
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
      .uri("/legacy/court-case/${createdCase.first}/reconciliation")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `token with incorrect role is forbidden`() {
    val createdCase = createCourtCase()
    webTestClient
      .get()
      .uri("/legacy/court-case/${createdCase.first}/reconciliation")
      .headers {
        it.authToken(roles = listOf("ROLE_OTHER_FUNCTION"))
      }
      .exchange()
      .expectStatus()
      .isForbidden
  }
}
