package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.util.DataCreator
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.CourtAppearanceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.CourtCaseEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityStatus
import java.time.LocalDateTime
import java.util.UUID

class CourtAppearanceTests {

  @Test
  fun `same date future time results in future status`() {
    val twoHoursInFuture = LocalDateTime.now().plusHours(2)
    val legacyCourtAppearance = DataCreator.legacyCreateCourtAppearance(appearanceDate = twoHoursInFuture.toLocalDate(), legacyData = DataCreator.courtAppearanceLegacyData(appearanceTime = twoHoursInFuture.toLocalTime()))
    val courtCase = CourtCaseEntity.placeholderEntity("P123", UUID.randomUUID().toString(), "user", null)
    val result = CourtAppearanceEntity.from(legacyCourtAppearance, null, courtCase, "user")
    Assertions.assertThat(result.statusId).isEqualTo(EntityStatus.FUTURE)
  }

  @Test
  fun `same date past time results in active status`() {
    val twoHoursInPast = LocalDateTime.now().minusHours(2)
    val legacyCourtAppearance = DataCreator.legacyCreateCourtAppearance(appearanceDate = twoHoursInPast.toLocalDate(), legacyData = DataCreator.courtAppearanceLegacyData(appearanceTime = twoHoursInPast.toLocalTime()))
    val courtCase = CourtCaseEntity.placeholderEntity("P123", UUID.randomUUID().toString(), "user", null)
    val result = CourtAppearanceEntity.from(legacyCourtAppearance, null, courtCase, "user")
    Assertions.assertThat(result.statusId).isEqualTo(EntityStatus.ACTIVE)
  }
}
