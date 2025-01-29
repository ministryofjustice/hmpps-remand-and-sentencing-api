package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.CourtAppearanceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.CourtCaseEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityStatus
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.UUID

class LatestCourtAppearanceTests {

  @Test
  fun `must fall back to created at when appearance dates are the same`() {
    val courtCase = CourtCaseEntity(
      prisonerId = "12345",
      caseUniqueIdentifier = UUID.randomUUID().toString(),
      createdByUsername = "user",
      statusId = EntityStatus.ACTIVE,
    )
    val appearanceDate = LocalDate.now()
    val courtAppearance = courtAppearanceEntity(appearanceDate, courtCase, ZonedDateTime.now().minusDays(10))
    val newerCourtAppearance = courtAppearanceEntity(appearanceDate, courtCase, ZonedDateTime.now())
    val latestCourtAppearance = CourtAppearanceEntity.getLatestCourtAppearance(listOf(courtAppearance, newerCourtAppearance))
    Assertions.assertThat(latestCourtAppearance).isEqualTo(newerCourtAppearance)
  }

  fun courtAppearanceEntity(appearanceDate: LocalDate, courtCase: CourtCaseEntity, createdAt: ZonedDateTime): CourtAppearanceEntity = CourtAppearanceEntity(
    appearanceUuid = UUID.randomUUID(),
    lifetimeUuid = UUID.randomUUID(),
    appearanceOutcome = null,
    courtCase = courtCase,
    courtCode = "CODE1",
    courtCaseReference = "REF1",
    statusId = EntityStatus.ACTIVE,
    previousAppearance = null,
    warrantId = "1",
    createdByUsername = "user",
    createdPrison = "PR1",
    warrantType = "TYPE",
    taggedBail = null,
    charges = mutableSetOf(),
    nextCourtAppearance = null,
    overallConvictionDate = LocalDate.now(),
    appearanceDate = appearanceDate,
    createdAt = createdAt,
  )
}
