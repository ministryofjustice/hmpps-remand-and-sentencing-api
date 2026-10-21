package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.immigrationdetention

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.ImmigrationDetention
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.event.EventSource
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ImmigrationDetentionRecordType.DEPORTATION_ORDER
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ImmigrationDetentionRecordType.NO_LONGER_OF_INTEREST
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.DpsDataCreator
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.*

class GetImmigrationByPrisonerTests : IntegrationTestBase() {

  @Test
  fun `Get all immigration detention records for a prisoner without NOMIS records`() {
    val id1 = DpsDataCreator.dpsCreateImmigrationDetention(
      prisonerId = "B12345B",
      immigrationDetentionRecordType = DEPORTATION_ORDER,
      createdByUsername = "aUser",
      recordDate = LocalDate.of(2021, 1, 1),
      createdByPrison = "PRI",
      appearanceOutcomeUuid = IMMIGRATION_DECISION_TO_DEPORT_UUID,
    )
    val firstImmigrationDetention = createImmigrationDetention(id1)

    val id2 = DpsDataCreator.dpsCreateImmigrationDetention(
      prisonerId = "B12345B",
      immigrationDetentionRecordType = NO_LONGER_OF_INTEREST,
      createdByUsername = "aUser",
      recordDate = LocalDate.of(2021, 1, 2),
      createdByPrison = "PRI",
      appearanceOutcomeUuid = IMMIGRATION_NO_LONGER_OF_INTEREST_UUID,
    )
    val secondImmigrationDetention = createImmigrationDetention(id2)

    val immigrationDetentionRecords = getImmigrationDetentionsByPrisonerId("B12345B")

    assertThat(immigrationDetentionRecords)
      .usingRecursiveComparison()
      .ignoringFields("createdAt")
      .ignoringCollectionOrder()
      .isEqualTo(
        listOf(
          ImmigrationDetention(
            immigrationDetentionUuid = firstImmigrationDetention.immigrationDetentionUuid,
            courtAppearanceUuid = firstImmigrationDetention.courtAppearanceUuid!!,
            prisonerId = "B12345B",
            immigrationDetentionRecordType = DEPORTATION_ORDER,
            recordDate = LocalDate.of(2021, 1, 1),
            createdAt = ZonedDateTime.now(),
          ),
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
  fun `Get all immigration detention records for a prisoner with NOMIS records`() {
    val (nomisCourtAppearanceUuid, legacyCourtAppearance) = createNomisImmigrationDetentionCourtCase(prisonerId = "B12345B", "5502")
    val (inactiveCourtCaseCourtAppearanceUuid, inactiveCourtAppearance) = createNomisImmigrationDetentionCourtCase(prisonerId = "B12345B", "5502", false)
    val id1 = DpsDataCreator.dpsCreateImmigrationDetention(
      prisonerId = "B12345B",
      immigrationDetentionRecordType = DEPORTATION_ORDER,
      createdByUsername = "aUser",
      recordDate = LocalDate.of(2021, 1, 1),
      createdByPrison = "PRI",
      appearanceOutcomeUuid = IMMIGRATION_DECISION_TO_DEPORT_UUID,
    )
    val firstImmigrationDetention = createImmigrationDetention(id1)

    val id2 = DpsDataCreator.dpsCreateImmigrationDetention(
      prisonerId = "B12345B",
      immigrationDetentionRecordType = NO_LONGER_OF_INTEREST,
      createdByUsername = "aUser",
      recordDate = LocalDate.of(2021, 1, 2),
      createdByPrison = "PRI",
      appearanceOutcomeUuid = IMMIGRATION_NO_LONGER_OF_INTEREST_UUID,
    )
    val secondImmigrationDetention = createImmigrationDetention(id2)

    val immigrationDetentionRecords = getImmigrationDetentionsByPrisonerId("B12345B")

    assertThat(immigrationDetentionRecords)
      .usingRecursiveComparison()
      .ignoringFields("createdAt", "immigrationDetentionUuid")
      .ignoringCollectionOrder()
      .isEqualTo(
        listOf(
          ImmigrationDetention(
            immigrationDetentionUuid = firstImmigrationDetention.immigrationDetentionUuid,
            courtAppearanceUuid = firstImmigrationDetention.courtAppearanceUuid!!,
            prisonerId = "B12345B",
            immigrationDetentionRecordType = DEPORTATION_ORDER,
            recordDate = LocalDate.of(2021, 1, 1),
            createdAt = ZonedDateTime.now(),
          ),
          ImmigrationDetention(
            immigrationDetentionUuid = secondImmigrationDetention.immigrationDetentionUuid,
            courtAppearanceUuid = secondImmigrationDetention.courtAppearanceUuid!!,
            prisonerId = "B12345B",
            immigrationDetentionRecordType = NO_LONGER_OF_INTEREST,
            recordDate = LocalDate.of(2021, 1, 2),
            createdAt = ZonedDateTime.now(),
          ),
          ImmigrationDetention(
            immigrationDetentionUuid = UUID.randomUUID(),
            courtAppearanceUuid = nomisCourtAppearanceUuid,
            prisonerId = "B12345B",
            immigrationDetentionRecordType = DEPORTATION_ORDER,
            recordDate = legacyCourtAppearance.appearanceDate,
            createdAt = ZonedDateTime.now(),
            source = EventSource.NOMIS,
          ),
          ImmigrationDetention(
            immigrationDetentionUuid = UUID.randomUUID(),
            courtAppearanceUuid = inactiveCourtCaseCourtAppearanceUuid,
            prisonerId = "B12345B",
            immigrationDetentionRecordType = DEPORTATION_ORDER,
            recordDate = inactiveCourtAppearance.appearanceDate,
            createdAt = ZonedDateTime.now(),
            source = EventSource.NOMIS,
          ),
        ),
      )
  }

  @Test
  fun `filter out court appearances when there is a corresponding immigration detention record`() {
    val (nomisCourtAppearanceUuid) = createNomisImmigrationDetentionCourtCase(prisonerId = "B12345B", "5502")
    val immigrationDetention = DpsDataCreator.dpsCreateImmigrationDetention(
      prisonerId = "B12345B",
      immigrationDetentionRecordType = DEPORTATION_ORDER,
      recordDate = LocalDate.of(2021, 1, 1),
      createdByUsername = "aUser",
      createdByPrison = "PRI",
      appearanceOutcomeUuid = IMMIGRATION_DECISION_TO_DEPORT_UUID,
      courtAppearanceUuid = nomisCourtAppearanceUuid,
    )
    val uuid = UUID.randomUUID()
    updateImmigrationDetention(immigrationDetention, uuid)

    val immigrationDetentionRecords = getImmigrationDetentionsByPrisonerId("B12345B")
    assertThat(immigrationDetentionRecords)
      .usingRecursiveComparison()
      .ignoringFields("createdAt")
      .ignoringCollectionOrder()
      .isEqualTo(
        listOf(
          ImmigrationDetention(
            immigrationDetentionUuid = uuid,
            courtAppearanceUuid = nomisCourtAppearanceUuid,
            prisonerId = "B12345B",
            immigrationDetentionRecordType = DEPORTATION_ORDER,
            recordDate = LocalDate.of(2021, 1, 1),
            createdAt = ZonedDateTime.now(),
          ),
        ),
      )
  }

  @Test
  fun `filter by record date (inclusive of the boundary dates)`() {
    val startDate = LocalDate.of(2021, 6, 1)
    val endDate = LocalDate.of(2021, 12, 31)
    val (nomisOnStartDate) = createNomisImmigrationDetentionCourtCase(prisonerId = "B12345B", "5502", appearanceDate = startDate)
    val (nomisBeforeStartDate) = createNomisImmigrationDetentionCourtCase(prisonerId = "B12345B", "5500", appearanceDate = startDate.minusDays(1))
    val dpsOnEndDate = createImmigrationDetention(
      DpsDataCreator.dpsCreateImmigrationDetention(
        prisonerId = "B12345B",
        immigrationDetentionRecordType = DEPORTATION_ORDER,
        createdByUsername = "aUser",
        recordDate = endDate,
        createdByPrison = "PRI",
        appearanceOutcomeUuid = IMMIGRATION_DECISION_TO_DEPORT_UUID,
      ),
    )
    val dpsAfterEndDate = createImmigrationDetention(
      DpsDataCreator.dpsCreateImmigrationDetention(
        prisonerId = "B12345B",
        immigrationDetentionRecordType = NO_LONGER_OF_INTEREST,
        createdByUsername = "aUser",
        recordDate = endDate.plusDays(1),
        createdByPrison = "PRI",
        appearanceOutcomeUuid = IMMIGRATION_NO_LONGER_OF_INTEREST_UUID,
      ),
    )

    val immigrationDetentionRecords = getImmigrationDetentionsByPrisonerId("B12345B", startDate, endDate)

    assertThat(immigrationDetentionRecords).extracting<UUID> { it.courtAppearanceUuid }
      .containsExactlyInAnyOrder(nomisOnStartDate, dpsOnEndDate.courtAppearanceUuid!!)
    assertThat(immigrationDetentionRecords).extracting<UUID> { it.courtAppearanceUuid }
      .doesNotContain(nomisBeforeStartDate, dpsAfterEndDate.courtAppearanceUuid)
  }

  @Test
  fun `no start and end date includes all records (past and future)`() {
    val (earlierNomisAppearance) = createNomisImmigrationDetentionCourtCase(
      prisonerId = "B12345B",
      nomisOutcomeCode = "5502",
      appearanceDate = LocalDate.now().minusYears(2),
    )
    val dpsToday = createImmigrationDetention(
      DpsDataCreator.dpsCreateImmigrationDetention(
        prisonerId = "B12345B",
        immigrationDetentionRecordType = DEPORTATION_ORDER,
        createdByUsername = "aUser",
        recordDate = LocalDate.now(),
        createdByPrison = "PRI",
        appearanceOutcomeUuid = IMMIGRATION_DECISION_TO_DEPORT_UUID,
      ),
    )
    val dpsAfterToday = createImmigrationDetention(
      DpsDataCreator.dpsCreateImmigrationDetention(
        prisonerId = "B12345B",
        immigrationDetentionRecordType = NO_LONGER_OF_INTEREST,
        createdByUsername = "aUser",
        recordDate = LocalDate.now().plusDays(1),
        createdByPrison = "PRI",
        appearanceOutcomeUuid = IMMIGRATION_NO_LONGER_OF_INTEREST_UUID,
      ),
    )

    val immigrationDetentionRecords = getImmigrationDetentionsByPrisonerId("B12345B")

    assertThat(immigrationDetentionRecords).extracting<UUID> { it.courtAppearanceUuid }
      .contains(earlierNomisAppearance, dpsToday.courtAppearanceUuid!!, dpsAfterToday.courtAppearanceUuid)
  }
}
