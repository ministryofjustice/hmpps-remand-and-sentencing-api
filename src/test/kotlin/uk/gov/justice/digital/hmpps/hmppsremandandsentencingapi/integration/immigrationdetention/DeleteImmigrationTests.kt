package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.immigrationdetention

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ImmigrationDetentionRecordType.DEPORTATION_ORDER
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ImmigrationDetentionRecordType.NO_LONGER_OF_INTEREST
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.DpsDataCreator
import java.time.LocalDate

class DeleteImmigrationTests : IntegrationTestBase() {

  @Test
  fun `Delete an Immigration Detention record`() {
    val id1 = DpsDataCreator.dpsCreateImmigrationDetention(
      prisonerId = "B12345B",
      immigrationDetentionRecordType = DEPORTATION_ORDER,
      createdByUsername = "aUser",
      recordDate = LocalDate.of(2021, 1, 1),
      createdByPrison = "PRI",
      appearanceOutcomeUuid = IMMIGRATION_DECISION_TO_DEPORT_UUID,
    )
    val id2 = DpsDataCreator.dpsCreateImmigrationDetention(
      prisonerId = "B12345B",
      immigrationDetentionRecordType = NO_LONGER_OF_INTEREST,
      recordDate = LocalDate.of(2021, 1, 1),
      createdByUsername = "aUser",
      createdByPrison = "PRI",
      appearanceOutcomeUuid = IMMIGRATION_NO_LONGER_OF_INTEREST_UUID,
    )

    val id1Response = createImmigrationDetention(id1)
    val id2Response = createImmigrationDetention(id2)

    var messages = getMessages(6)

    assertThat(messages).hasSize(6).extracting<String> { it.eventType }
      .contains("court-case.inserted", "court-case.inserted", "court-appearance.inserted", "charge.inserted", "court-appearance.inserted", "charge.inserted")

    purgeQueues()

    deleteImmigrationDetention(id1Response.immigrationDetentionUuid)

    messages = getMessages(3)

    assertThat(messages).hasSize(3).extracting<String> { it.eventType }
      .contains("court-appearance.deleted", "charge.deleted", "court-case.deleted")

    purgeQueues()

    deleteImmigrationDetention(id2Response.immigrationDetentionUuid)

    messages = getMessages(3)

    assertThat(messages).hasSize(3).extracting<String> { it.eventType }
      .contains("court-case.deleted", "court-appearance.deleted", "charge.deleted")

    purgeQueues()

    val historicImmigrationDetention =
      immigrationDetentionHistoryRepository.findByImmigrationDetentionUuid(id1Response.immigrationDetentionUuid)
    assertThat(historicImmigrationDetention).hasSize(1)
    assertThat(historicImmigrationDetention[0].historyCreatedAt).isNotNull()

    assertThat(getImmigrationDetentionsByPrisonerId("B12345B")).isEmpty()
  }
}
