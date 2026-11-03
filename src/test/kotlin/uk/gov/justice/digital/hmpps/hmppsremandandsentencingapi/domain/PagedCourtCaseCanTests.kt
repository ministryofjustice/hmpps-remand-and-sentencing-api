package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.PagedCourtCaseCan.Companion.canAppeal
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ChargeEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.CourtAppearanceEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.CourtCaseEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.SentenceEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util.CourtCaseRowCreator

class PagedCourtCaseCanTests {

  @Test
  fun `can appeal when no recalls and there is an active sentence`() {
    val courtCaseRow = CourtCaseRowCreator.courtCaseRow(
      courtCaseStatus = CourtCaseEntityStatus.ACTIVE,
      courtAppearanceStatus = CourtAppearanceEntityStatus.ACTIVE,
      appearanceChargeStatus = ChargeEntityStatus.ACTIVE,
      appearanceSentenceStatus = SentenceEntityStatus.ACTIVE,
    )
    val canAppeal = canAppeal(listOf(courtCaseRow))
    Assertions.assertThat(canAppeal).isTrue()
  }

  @Test
  fun `cannot appeal when the sentence is inactive`() {
    val courtCaseRow = CourtCaseRowCreator.courtCaseRow(
      courtCaseStatus = CourtCaseEntityStatus.ACTIVE,
      courtAppearanceStatus = CourtAppearanceEntityStatus.ACTIVE,
      appearanceChargeStatus = ChargeEntityStatus.ACTIVE,
      appearanceSentenceStatus = SentenceEntityStatus.INACTIVE,
    )
    val canAppeal = canAppeal(listOf(courtCaseRow))
    Assertions.assertThat(canAppeal).isFalse()
  }

  @Test
  fun `cannot add hearing when there is a sentencing warrant appearance`() {
    val courtCaseRow = CourtCaseRowCreator.courtCaseRow(
      courtCaseStatus = CourtCaseEntityStatus.ACTIVE,
      courtAppearanceStatus = CourtAppearanceEntityStatus.ACTIVE,
      appearanceChargeStatus = ChargeEntityStatus.ACTIVE,
      appearanceSentenceStatus = SentenceEntityStatus.INACTIVE,
      courtAppearanceWarrantType = "SENTENCING",
    )
    val courtCaseCan = PagedCourtCaseCan.from(listOf(courtCaseRow))
    Assertions.assertThat(courtCaseCan.canAddHearing).isFalse()
  }
}
