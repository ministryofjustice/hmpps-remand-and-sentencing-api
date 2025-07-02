package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.audit

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.RecallEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.RecallTypeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.RecallType
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.*

class RecallHistoryEntityTest {

  @Test
  fun `can create recall history with relevant status`() {
    val recallUuid = UUID.randomUUID()
    val original = RecallEntity(
      id = 987654,
      recallUuid = recallUuid,
      prisonerId = "A1234BC",
      revocationDate = LocalDate.of(2021, 1, 1),
      returnToCustodyDate = LocalDate.of(2022, 2, 2),
      inPrisonOnRevocationDate = true,
      recallType = RecallTypeEntity(0, RecallType.LR, "LR"),
      statusId = EntityStatus.ACTIVE,
      createdAt = ZonedDateTime.of(2023, 3, 3, 3, 3, 3, 3, ZoneId.systemDefault()),
      createdByUsername = "CREATOR",
      createdPrison = "FOO",
      updatedAt = ZonedDateTime.of(2024, 4, 4, 4, 4, 4, 4, ZoneId.systemDefault()),
      updatedBy = "UPDATER",
      updatedPrison = "BAR",
    )

    assertThat(RecallHistoryEntity.from(original, EntityStatus.DELETED))
      .usingRecursiveComparison()
      .ignoringFields("historyCreatedAt")
      .isEqualTo(
        RecallHistoryEntity(
          id = 0,
          recallId = 987654,
          recallUuid = recallUuid,
          prisonerId = "A1234BC",
          revocationDate = LocalDate.of(2021, 1, 1),
          returnToCustodyDate = LocalDate.of(2022, 2, 2),
          inPrisonOnRevocationDate = true,
          recallType = RecallTypeEntity(0, RecallType.LR, "LR"),
          statusId = EntityStatus.ACTIVE,
          createdAt = ZonedDateTime.of(2023, 3, 3, 3, 3, 3, 3, ZoneId.systemDefault()),
          createdByUsername = "CREATOR",
          createdPrison = "FOO",
          updatedAt = ZonedDateTime.of(2024, 4, 4, 4, 4, 4, 4, ZoneId.systemDefault()),
          updatedBy = "UPDATER",
          updatedPrison = "BAR",
          historyStatusId = EntityStatus.DELETED,
          historyCreatedAt = ZonedDateTime.now(), // ignored in assertion
        ),
      )
  }
}
