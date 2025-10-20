package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.util.DataCreator
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.CourtAppearanceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.CourtCaseEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.CourtAppearanceEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.DpsDataCreator
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.stream.Stream

class CourtAppearanceTests {

  @Test
  fun `same date future time results in future status`() {
    val twoHoursInFuture = LocalDateTime.now().plusHours(2)
    val legacyCourtAppearance = DataCreator.legacyCreateCourtAppearance(appearanceDate = twoHoursInFuture.toLocalDate(), legacyData = DataCreator.courtAppearanceLegacyData(appearanceTime = twoHoursInFuture.toLocalTime()))
    val courtCase = CourtCaseEntity.from(DpsDataCreator.dpsCreateCourtCase(), "user")
    val result = CourtAppearanceEntity.from(legacyCourtAppearance, null, courtCase, "user")
    Assertions.assertThat(result.statusId).isEqualTo(CourtAppearanceEntityStatus.FUTURE)
  }

  @Test
  fun `same date past time results in active status`() {
    val twoHoursInPast = LocalDateTime.now().minusHours(2)
    val legacyCourtAppearance = DataCreator.legacyCreateCourtAppearance(appearanceDate = twoHoursInPast.toLocalDate(), legacyData = DataCreator.courtAppearanceLegacyData(appearanceTime = twoHoursInPast.toLocalTime()))
    val courtCase = CourtCaseEntity.from(DpsDataCreator.dpsCreateCourtCase(), "user")
    val result = CourtAppearanceEntity.from(legacyCourtAppearance, null, courtCase, "user")
    Assertions.assertThat(result.statusId).isEqualTo(CourtAppearanceEntityStatus.ACTIVE)
  }

  @Test
  fun `appearance without outcome and null legacy data and charge has sentence results in sentencing warrant type`() {
    val sentencedCharge = DataCreator.migrationCreateCharge(sentence = DataCreator.migrationCreateSentence())
    val migrationCourtAppearance = DataCreator.migrationCreateCourtAppearance(legacyData = DataCreator.courtAppearanceLegacyData(outcomeDispositionCode = null, outcomeConvictionFlag = null, outcomeDescription = null, nomisOutcomeCode = null), charges = listOf(sentencedCharge))
    val courtCase = CourtCaseEntity.from(DataCreator.migrationCreateCourtCase(), "user", "PRI1")
    val result = CourtAppearanceEntity.from(migrationCourtAppearance, null, courtCase, "user", null)
    Assertions.assertThat(result.warrantType).isEqualTo("SENTENCING")
  }

  @ParameterizedTest(name = "appearance with legacy data outcome code {0} results in status of {1}")
  @MethodSource("legacyDataOutcomeStatusParameters")
  fun `legacy outcome code to entity status`(nomisOutcomeCode: String, expectedStatus: CourtAppearanceEntityStatus) {
    val legacyCourtAppearance = DataCreator.legacyCreateCourtAppearance(appearanceDate = LocalDate.now().minusDays(5), legacyData = DataCreator.courtAppearanceLegacyData(nomisOutcomeCode = nomisOutcomeCode))
    val courtCase = CourtCaseEntity.from(DpsDataCreator.dpsCreateCourtCase(), "user")
    val result = CourtAppearanceEntity.from(legacyCourtAppearance, null, courtCase, "user")
    Assertions.assertThat(result.statusId).isEqualTo(expectedStatus)
  }

  companion object {
    @JvmStatic
    fun legacyDataOutcomeStatusParameters(): Stream<Arguments> = Stream.of(
      Arguments.of("1501", CourtAppearanceEntityStatus.RECALL_APPEARANCE),
      Arguments.of("5501", CourtAppearanceEntityStatus.IMMIGRATION_APPEARANCE),
      Arguments.of("5502", CourtAppearanceEntityStatus.IMMIGRATION_APPEARANCE),
    )
  }
}
