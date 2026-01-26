package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.immigrationdetention

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.ImmigrationDetention
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.event.EventSource.DPS
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.CourtAppearanceEntityStatus.IMMIGRATION_APPEARANCE
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ImmigrationDetentionNoLongerOfInterestType.OTHER_REASON
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ImmigrationDetentionRecordType.DEPORTATION_ORDER
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ImmigrationDetentionRecordType.NO_LONGER_OF_INTEREST
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.DpsDataCreator
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.*

class UpdateImmigrationDetentionTests : IntegrationTestBase() {

  @Test
  fun `Update an Immigration Detention record and fetch it based on returned UUID`() {
    val immigrationDetention = DpsDataCreator.dpsCreateImmigrationDetention(
      prisonerId = "B12345B",
      immigrationDetentionRecordType = DEPORTATION_ORDER,
      recordDate = LocalDate.of(2021, 1, 1),
      createdByUsername = "aUser",
      createdByPrison = "PRI",
      appearanceOutcomeUuid = IMMIGRATION_DECISION_TO_DEPORT_UUID,
    )

    val uuid = UUID.randomUUID()
    val createResponse = updateImmigrationDetention(immigrationDetention, uuid)
    val actualImmigrationDetention =
      getImmigrationDetentionByUUID(createResponse.immigrationDetentionUuid)

    var messages = getMessages(3)

    assertThat(messages).hasSize(3).extracting<String> { it.eventType }
      .contains("court-appearance.inserted", "charge.inserted", "court-case.inserted")

    purgeQueues()

    assertThat(actualImmigrationDetention).usingRecursiveComparison()
      .ignoringFields("createdAt")
      .isEqualTo(
        ImmigrationDetention(
          immigrationDetentionUuid = createResponse.immigrationDetentionUuid,
          courtAppearanceUuid = createResponse.courtAppearanceUuid!!,
          prisonerId = "B12345B",
          immigrationDetentionRecordType = DEPORTATION_ORDER,
          recordDate = LocalDate.of(2021, 1, 1),
          createdAt = ZonedDateTime.now(),
        ),
      )

    immigrationDetention.immigrationDetentionRecordType = NO_LONGER_OF_INTEREST
    immigrationDetention.recordDate = LocalDate.of(2021, 2, 1)
    val updateResponse = updateImmigrationDetention(immigrationDetention, uuid)

    val historicImmigrationDetention =
      immigrationDetentionHistoryRepository.findByImmigrationDetentionUuid(updateResponse.immigrationDetentionUuid)
    assertThat(historicImmigrationDetention).hasSize(1)
    assertThat(historicImmigrationDetention[0].immigrationDetentionRecordType).isEqualTo(DEPORTATION_ORDER)
    assertThat(historicImmigrationDetention[0].historyCreatedAt).isNotNull()

    val courtCase = courtCaseRepository.findAllByPrisonerId("B12345B").firstOrNull()

    val appearances = courtAppearanceRepository.findAllByCourtCaseCaseUniqueIdentifierAndStatusId(
      courtCase?.caseUniqueIdentifier.toString(),
      IMMIGRATION_APPEARANCE,
    )

    assertThat(appearances[0].appearanceDate).isEqualTo(LocalDate.of(2021, 2, 1))

    messages = getMessages(2)

    assertThat(messages).hasSize(2).extracting<String> { it.eventType }
      .contains("court-appearance.updated", "charge.updated")

    purgeQueues()
  }

  @Test
  fun `Update an Immigration Detention record to test other fields`() {
    val immigrationDetention = DpsDataCreator.dpsCreateImmigrationDetention(
      prisonerId = "B12345B",
      immigrationDetentionRecordType = NO_LONGER_OF_INTEREST,
      noLongerOfInterestReason = OTHER_REASON,
      noLongerOfInterestComment = "A Comment",
      recordDate = LocalDate.of(2021, 1, 1),
      createdByUsername = "aUser",
      createdByPrison = "PRI",
      appearanceOutcomeUuid = IMMIGRATION_NO_LONGER_OF_INTEREST_UUID,
    )

    val uuid = UUID.randomUUID()
    val immigrationDetentionResponse = updateImmigrationDetention(immigrationDetention, uuid)
    val actualImmigrationDetention =
      getImmigrationDetentionByUUID(immigrationDetentionResponse.immigrationDetentionUuid)

    assertThat(actualImmigrationDetention).usingRecursiveComparison()
      .ignoringFields("createdAt")
      .isEqualTo(
        ImmigrationDetention(
          immigrationDetentionUuid = immigrationDetentionResponse.immigrationDetentionUuid,
          courtAppearanceUuid = immigrationDetentionResponse.courtAppearanceUuid!!,
          prisonerId = "B12345B",
          immigrationDetentionRecordType = NO_LONGER_OF_INTEREST,
          noLongerOfInterestReason = OTHER_REASON,
          noLongerOfInterestComment = "A Comment",
          recordDate = LocalDate.of(2021, 1, 1),
          source = DPS,
          createdAt = ZonedDateTime.now(),
        ),
      )
  }
}
