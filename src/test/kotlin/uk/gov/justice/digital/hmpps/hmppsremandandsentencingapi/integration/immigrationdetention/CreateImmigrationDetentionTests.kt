package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.immigrationdetention

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.ImmigrationDetention
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.CourtAppearanceEntityStatus.IMMIGRATION_APPEARANCE
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ImmigrationDetentionRecordType.IS91
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ImmigrationDetentionRecordType.NO_LONGER_OF_INTEREST
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.DpsDataCreator
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.*

class CreateImmigrationDetentionTests : IntegrationTestBase() {

  @Test
  fun `Create an Immigration Detention record and fetch it based on returned UUID also check the events are emitted`() {
    val immigrationDetention = DpsDataCreator.dpsCreateImmigrationDetention(
      prisonerId = "A12345B",
      immigrationDetentionRecordType = IS91,
      recordDate = LocalDate.of(2021, 1, 1),
      createdByUsername = "aUser",
      createdByPrison = "PRI",
      appearanceOutcomeUuid = IMMIGRATION_IS91_UUID,
    )

    val immigrationDetentionResponse = createImmigrationDetention(immigrationDetention)
    val actualImmigrationDetention =
      getImmigrationDetentionByUUID(immigrationDetentionResponse.immigrationDetentionUuid)

    assertThat(actualImmigrationDetention).usingRecursiveComparison()
      .ignoringFields("createdAt", "courtAppearanceUuid")
      .isEqualTo(
        ImmigrationDetention(
          immigrationDetentionUuid = immigrationDetentionResponse.immigrationDetentionUuid,
          courtAppearanceUuid = UUID.randomUUID(),
          prisonerId = "A12345B",
          immigrationDetentionRecordType = IS91,
          recordDate = LocalDate.of(2021, 1, 1),
          createdAt = ZonedDateTime.now(),
        ),
      )

    var messages = getMessages(3)

    assertThat(messages).hasSize(3).extracting<String> { it.eventType }
      .contains("court-appearance.inserted", "charge.inserted", "court-case.inserted")

    purgeQueues()

    val immigrationDetention2 = DpsDataCreator.dpsCreateImmigrationDetention(
      prisonerId = "A12345B",
      immigrationDetentionRecordType = NO_LONGER_OF_INTEREST,
      recordDate = LocalDate.of(2021, 1, 1),
      createdByUsername = "aUser",
      createdByPrison = "PRI",
      appearanceOutcomeUuid = IMMIGRATION_NO_LONGER_OF_INTEREST_UUID,
    )

    createImmigrationDetention(immigrationDetention2)

    messages = getMessages(3)

    assertThat(messages).hasSize(3).extracting<String> { it.eventType }
      .contains("court-appearance.inserted", "charge.inserted", "court-case.inserted")

    val courtAppearances = courtAppearanceRepository.findAllByCourtCasePrisonerIdAndStatusId("A12345B", IMMIGRATION_APPEARANCE)

    courtAppearances
      .forEach { appearance ->
        assertThat(appearance.courtCode).isEqualTo("IMM")
      }
  }

  @Test
  fun `create immigration detention record based on NOMIS court appearance`() {
    val courtAppearanceUuid = createNomisImmigrationDetentionCourtCase(prisonerId = "B12345B", "5500")
    val immigrationDetention = DpsDataCreator.dpsCreateImmigrationDetention(
      prisonerId = "B12345B",
      immigrationDetentionRecordType = IS91,
      recordDate = LocalDate.of(2021, 1, 1),
      createdByUsername = "aUser",
      createdByPrison = "PRI",
      appearanceOutcomeUuid = IMMIGRATION_IS91_UUID,
      courtAppearanceUuid = courtAppearanceUuid,
    )

    val immigrationDetentionResponse = createImmigrationDetention(immigrationDetention)
    assertThat(immigrationDetentionResponse.courtAppearanceUuid!!).isEqualTo(courtAppearanceUuid)

    val actualImmigrationDetention =
      getImmigrationDetentionByUUID(immigrationDetentionResponse.immigrationDetentionUuid)

    assertThat(actualImmigrationDetention).usingRecursiveComparison()
      .ignoringFields("createdAt")
      .isEqualTo(
        ImmigrationDetention(
          immigrationDetentionUuid = immigrationDetentionResponse.immigrationDetentionUuid,
          courtAppearanceUuid = courtAppearanceUuid,
          prisonerId = "B12345B",
          immigrationDetentionRecordType = IS91,
          recordDate = LocalDate.of(2021, 1, 1),
          createdAt = ZonedDateTime.now(),
        ),
      )

    val messages = getMessages(2)

    assertThat(messages).hasSize(2).extracting<String> { it.eventType }
      .contains("court-appearance.updated", "charge.updated")
  }
}
