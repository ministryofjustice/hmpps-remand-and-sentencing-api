package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ChargeEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.CourtAppearanceEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.CourtCaseEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.SentenceEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.SentenceTypeClassification
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.projection.CourtCaseRow

data class PagedCourtCaseCan(
  val hasNoRecalls: Boolean,
  val canAppeal: Boolean,
  val canBreach: Boolean,
  val canAddHearing: Boolean,
) {
  companion object {
    fun from(courtCaseRows: List<CourtCaseRow>): PagedCourtCaseCan {
      val courtCaseIsNotMerged = courtCaseIsNotMerged(courtCaseRows)
      val hasNoRecalls = hasNoRecalls(courtCaseRows)
      val canAppeal = canAppeal(courtCaseRows)
      val canBreach = canBreach(courtCaseRows)
      val hasNoSentencingWarrantTypes = hasNoSentencingWarrantTypes(courtCaseRows)
      val canAddHearing = hasNoRecalls && !canAppeal && !canBreach && courtCaseIsNotMerged && hasNoSentencingWarrantTypes
      return PagedCourtCaseCan(
        hasNoRecalls,
        canAppeal,
        canBreach,
        canAddHearing,
      )
    }

    fun courtCaseIsNotMerged(courtCaseRows: List<CourtCaseRow>): Boolean = courtCaseRows.none { it.courtCaseStatus == CourtCaseEntityStatus.MERGED }

    fun hasNoRecalls(courtCaseRows: List<CourtCaseRow>): Boolean = courtCaseRows.all { it.recallInAppearanceId == null }

    fun canAppeal(courtCaseRows: List<CourtCaseRow>): Boolean = hasNoRecalls(courtCaseRows) &&
      courtCaseRows.any {
        it.courtCaseStatus == CourtCaseEntityStatus.ACTIVE &&
          it.courtAppearanceStatus == CourtAppearanceEntityStatus.ACTIVE &&
          it.appearanceChargeStatus == ChargeEntityStatus.ACTIVE &&
          it.appearanceSentenceStatus == SentenceEntityStatus.ACTIVE
      }

    fun canBreach(courtCaseRows: List<CourtCaseRow>): Boolean = hasNoRecalls(courtCaseRows) &&
      courtCaseRows.any {
        it.courtAppearanceStatus == CourtAppearanceEntityStatus.ACTIVE &&
          it.appearanceChargeStatus == ChargeEntityStatus.ACTIVE &&
          it.appearanceSentenceTypeClassification == SentenceTypeClassification.DTO
      }

    fun hasNoSentencingWarrantTypes(courtCaseRows: List<CourtCaseRow>): Boolean = courtCaseRows.none { it.courtAppearanceWarrantType == "SENTENCING" }
  }
}
