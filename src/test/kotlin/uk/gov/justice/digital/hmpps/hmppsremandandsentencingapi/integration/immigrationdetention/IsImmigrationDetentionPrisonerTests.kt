package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.immigrationdetention

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ImmigrationDetentionRecordType.DEPORTATION_ORDER
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.DpsDataCreator
import java.time.LocalDate

class IsImmigrationDetentionPrisonerTests : IntegrationTestBase() {

  @ParameterizedTest
  @ValueSource(
    strings = [
      "ROLE_REMAND_SENTENCING__IMMIGRATION_DETENTION_RW",
      "ROLE_REMAND_AND_SENTENCING__CCRD__RO",
      "ROLE_REMAND_SENTENCING__IMMIGRATION_DETENTION_RW,ROLE_REMAND_AND_SENTENCING__CCRD__RO",
    ],
  )
  fun `Should return true when the prisoner has a DPS immigration detention record`(roleCsv: String) {
    val roles = roleCsv.split(",")
    createImmigrationDetention(
      DpsDataCreator.dpsCreateImmigrationDetention(
        prisonerId = "B12345B",
        immigrationDetentionRecordType = DEPORTATION_ORDER,
        createdByUsername = "aUser",
        recordDate = LocalDate.of(2021, 1, 1),
        createdByPrison = "PRI",
        appearanceOutcomeUuid = IMMIGRATION_DECISION_TO_DEPORT_UUID,
      ),
    )

    assertThat(isImmigrationDetentionPrisoner("B12345B", roles)).isTrue()
  }

  @ParameterizedTest
  @ValueSource(
    strings = [
      "ROLE_REMAND_SENTENCING__IMMIGRATION_DETENTION_RW",
      "ROLE_REMAND_AND_SENTENCING__CCRD__RO",
    ],
  )
  fun `Should return true when the prisoner only has a NOMIS immigration detention record`(roleCsv: String) {
    val roles = roleCsv.split(",")
    createNomisImmigrationDetentionCourtCase(prisonerId = "B12345B", nomisOutcomeCode = "5500")

    assertThat(isImmigrationDetentionPrisoner("B12345B", roles)).isTrue()
  }

  @Test
  fun `Should return false when the prisoner has no immigration detention records`() {
    assertThat(isImmigrationDetentionPrisoner("599540")).isFalse()
  }

  @Test
  fun `Should return 401 when unauthorised`() {
    webTestClient
      .get()
      .uri("/immigration-detention/person/B12345B/exists")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `Should return 403 when missing valid role`() {
    webTestClient
      .get()
      .uri("/immigration-detention/person/B12345B/exists")
      .headers {
        it.authToken(roles = listOf("ROLE_FOOBAR"))
      }
      .exchange()
      .expectStatus()
      .isForbidden
  }
}
