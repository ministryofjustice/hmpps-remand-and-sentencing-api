package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.util.DataCreator
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.AppearanceTypeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.CourtAppearanceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.CourtCaseEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.NextCourtAppearanceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ReferenceEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.DpsDataCreator
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZonedDateTime
import java.util.UUID

class NextCourtAppearanceEntityTests {

  @Test
  fun `do not set time if midnight in migration`() {
    val nextEventDateTimeAtMidnight = LocalDateTime.of(LocalDate.now(), LocalTime.MIDNIGHT)
    val legacyData = DataCreator.courtAppearanceLegacyData(nextEventDateTime = nextEventDateTimeAtMidnight)
    val nomisAppearance = DataCreator.migrationCreateCourtAppearance(legacyData = legacyData)
    val futureMigrationCourtAppearance = DataCreator.migrationCreateCourtAppearance()
    val result = NextCourtAppearanceEntity.from(nomisAppearance, futureMigrationCourtAppearance, futureAppearance, appearanceTypeEntity)
    Assertions.assertNull(result.appearanceTime)
  }

  @Test
  fun `do not set time if midnight in sync`() {
    val legacyData = DataCreator.courtAppearanceLegacyData(appearanceTime = LocalTime.MIDNIGHT)
    val nomisAppearance = DataCreator.legacyCreateCourtAppearance(legacyData = legacyData)
    val result = NextCourtAppearanceEntity.from(nomisAppearance, futureAppearance, appearanceTypeEntity)
    Assertions.assertNull(result.appearanceTime)
  }

  companion object {
    val appearanceTypeEntity = AppearanceTypeEntity(
      0,
      UUID.randomUUID(),
      "appearance type",
      0,
      ReferenceEntityStatus.ACTIVE,
    )
    val courtCase = CourtCaseEntity.from(DpsDataCreator.dpsCreateCourtCase(), "user")
    val futureAppearance = CourtAppearanceEntity(
      0, UUID.randomUUID(), null, courtCase, "COURT", "CASE1", LocalDate.now(),
      EntityStatus.ACTIVE, null, null, ZonedDateTime.now(), "user", null, null, null, null, "REMAND", mutableSetOf(), null, null, null,
    )
  }
}
