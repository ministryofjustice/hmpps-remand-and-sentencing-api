package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.courtcase

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.DpsDataCreator
import java.time.LocalDate
import java.util.UUID

class HasActiveCaseWithLatestRemandOutcomeTests : IntegrationTestBase() {

  @Test
  fun `should return false when the prisoner has no court cases`() {
    assertThat(courtCaseRepository.hasActiveCaseWithLatestRemandOutcome(setOf(DpsDataCreator.DEFAULT_PRISONER_ID))).isFalse()
  }

  @Test
  fun `should return true when the latest appearance outcome is REMAND`() {
    createCourtCase(DpsDataCreator.dpsCreateNonSentencedCourtCase(prisonerId = "PRI124"))

    assertThat(courtCaseRepository.hasActiveCaseWithLatestRemandOutcome(setOf("PRI124"))).isTrue()
  }

  @Test
  fun `should return false when the latest appearance outcome is not REMAND`() {
    createCourtCase(
      DpsDataCreator.dpsCreateNonSentencedCourtCase(
        prisonerId = DpsDataCreator.DEFAULT_PRISONER_ID,
        appearances = listOf(
          DpsDataCreator.dpsCreateNonSentencedCourtAppearance(
            outcomeUuid = UUID.fromString("1709089f-0cf8-4544-8229-a2251e671241"), // Fine outcome
          ),
        ),
      ),
    )

    assertThat(courtCaseRepository.hasActiveCaseWithLatestRemandOutcome(setOf(DpsDataCreator.DEFAULT_PRISONER_ID))).isFalse()
  }

  @Test
  fun `should return false when an older appearance is REMAND but the latest is sentencing`() {
    createCourtCase(
      DpsDataCreator.dpsCreateCourtCase(
        prisonerId = "PRI124",
        appearances = listOf(
          DpsDataCreator.dpsCreateNonSentencedCourtAppearance(appearanceDate = LocalDate.now().minusDays(1)),
          DpsDataCreator.dpsCreateCourtAppearance(),
        ),
      ),
    )

    assertThat(courtCaseRepository.hasActiveCaseWithLatestRemandOutcome(setOf("PRI124"))).isFalse()
  }

  @Test
  fun `should return true when only one of several prisoners is on remand`() {
    createCourtCase(DpsDataCreator.dpsCreateNonSentencedCourtCase(prisonerId = "PRI124"))
    createCourtCase(
      DpsDataCreator.dpsCreateNonSentencedCourtCase(
        prisonerId = "PRI125",
        appearances = listOf(
          DpsDataCreator.dpsCreateNonSentencedCourtAppearance(
            outcomeUuid = UUID.fromString("1709089f-0cf8-4544-8229-a2251e671241"), // Fine outcome
          ),
        ),
      ),
    )

    assertThat(courtCaseRepository.hasActiveCaseWithLatestRemandOutcome(setOf("PRI125"))).isFalse()
    assertThat(courtCaseRepository.hasActiveCaseWithLatestRemandOutcome(setOf("PRI125", "PRI124"))).isTrue()
  }

  @Test
  fun `should return false when none of several prisoners is on remand`() {
    assertThat(courtCaseRepository.hasActiveCaseWithLatestRemandOutcome(setOf("PRI125", "PRI126"))).isFalse()
  }
}
