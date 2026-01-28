package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.immigrationdetention

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.ImmigrationDetention
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.event.EventSource
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ImmigrationDetentionRecordType.DEPORTATION_ORDER
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ImmigrationDetentionRecordType.IS91
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ImmigrationDetentionRecordType.NO_LONGER_OF_INTEREST
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.DpsDataCreator
import java.lang.Thread.sleep
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.*

class GetLatestImmigrationByPrisonerTests : IntegrationTestBase() {

  @Test
  fun `Get latest immigration detention record for a prisoner without NOMIS records`() {
    val id1 = DpsDataCreator.dpsCreateImmigrationDetention(
      prisonerId = "B12345B",
      immigrationDetentionRecordType = DEPORTATION_ORDER,
      createdByUsername = "aUser",
      recordDate = LocalDate.of(2021, 1, 1),
      createdByPrison = "PRI",
      appearanceOutcomeUuid = IMMIGRATION_DECISION_TO_DEPORT_UUID,
    )
    val id1Uuid = createImmigrationDetention(id1).immigrationDetentionUuid

    sleep(1000)

    val id2 = DpsDataCreator.dpsCreateImmigrationDetention(
      prisonerId = "B12345B",
      immigrationDetentionRecordType = NO_LONGER_OF_INTEREST,
      createdByUsername = "aUser",
      recordDate = LocalDate.of(2021, 1, 2),
      createdByPrison = "PRI",
      appearanceOutcomeUuid = IMMIGRATION_NO_LONGER_OF_INTEREST_UUID,
    )
    val secondImmigrationDetention = createImmigrationDetention(id2)

    val immigrationDetentionRecords = getLatestImmigrationDetentionRecordByPrisonerId("B12345B")

    assertThat(immigrationDetentionRecords)
      .usingRecursiveComparison()
      .ignoringFields("createdAt")
      .ignoringCollectionOrder()
      .isEqualTo(
        listOf(
          ImmigrationDetention(
            immigrationDetentionUuid = secondImmigrationDetention.immigrationDetentionUuid,
            courtAppearanceUuid = secondImmigrationDetention.courtAppearanceUuid!!,
            prisonerId = "B12345B",
            immigrationDetentionRecordType = NO_LONGER_OF_INTEREST,
            recordDate = LocalDate.of(2021, 1, 2),
            createdAt = ZonedDateTime.now(),
          ),
        ),
      )
  }

  @Test
  fun `Get latest immigration detention record for a prisoner with NOMIS records`() {
    val id1 = DpsDataCreator.dpsCreateImmigrationDetention(
      prisonerId = "B12345B",
      immigrationDetentionRecordType = DEPORTATION_ORDER,
      createdByUsername = "aUser",
      recordDate = LocalDate.of(2021, 1, 1),
      createdByPrison = "PRI",
      appearanceOutcomeUuid = IMMIGRATION_DECISION_TO_DEPORT_UUID,
    )
    val id1Uuid = createImmigrationDetention(id1).immigrationDetentionUuid

    sleep(1000)

    val courtAppearanceUuid = createNomisImmigrationDetentionCourtCase(prisonerId = "B12345B", "5500")

    val immigrationDetentionRecords = getLatestImmigrationDetentionRecordByPrisonerId("B12345B")

    assertThat(immigrationDetentionRecords)
      .usingRecursiveComparison()
      .ignoringFields("createdAt", "immigrationDetentionUuid")
      .ignoringCollectionOrder()
      .isEqualTo(
        listOf(
          ImmigrationDetention(
            immigrationDetentionUuid = UUID.randomUUID(),
            courtAppearanceUuid = courtAppearanceUuid,
            prisonerId = "B12345B",
            immigrationDetentionRecordType = IS91,
            recordDate = LocalDate.now(),
            createdAt = ZonedDateTime.now(),
            source = EventSource.NOMIS,
          ),
        ),
      )
  }

  @Test
  fun `Get the latest Immigration Detention record`() {
    createImmigrationDetention(
      DpsDataCreator.dpsCreateImmigrationDetention(
        prisonerId = "B12345B",
        immigrationDetentionRecordType = IS91,
        recordDate = LocalDate.of(2021, 2, 1),
        createdByUsername = "aUser",
        createdByPrison = "PRI",
        appearanceOutcomeUuid = IMMIGRATION_IS91_UUID,
      ),
    )
    assertThat(getLatestImmigrationDetentionByPrisonerId("B12345B").immigrationDetentionRecordType).isEqualTo(IS91)

    sleep(1000) // Ensure createdAt timestamps differ

    createImmigrationDetention(
      DpsDataCreator.dpsCreateImmigrationDetention(
        prisonerId = "B12345B",
        immigrationDetentionRecordType = NO_LONGER_OF_INTEREST,
        recordDate = LocalDate.of(2021, 1, 1),
        createdByUsername = "aUser",
        createdByPrison = "PRI",
        appearanceOutcomeUuid = IMMIGRATION_NO_LONGER_OF_INTEREST_UUID,
      ),
    )
    assertThat(getLatestImmigrationDetentionByPrisonerId("B12345B").immigrationDetentionRecordType).isEqualTo(
      IS91,
    )
  }

  @Test
  fun `Check behaviour when there are no records`() {
    webTestClient
      .get()
      .uri("/immigration-detention/person/NOTFOUND/latest")
      .headers {
        it.authToken(roles = listOf("ROLE_REMAND_SENTENCING__IMMIGRATION_DETENTION_RW"))
      }
      .exchange()
      .expectStatus()
      .isNotFound
  }
}
