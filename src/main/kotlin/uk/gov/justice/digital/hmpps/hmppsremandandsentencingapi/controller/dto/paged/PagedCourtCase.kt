package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.paged

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.projection.CourtCaseRow
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.CourtCaseLegacyData
import java.time.LocalDate

data class PagedCourtCase(
  val prisonerId: String,
  val courtCaseUuid: String,
  val courtCaseStatus: EntityStatus,
  val legacyData: CourtCaseLegacyData?,
  val appearanceCount: Long,
  val caseReferences: Set<String>,
  val firstDayInCustody: LocalDate,
  val overallSentenceLength: PagedAppearancePeriodLength?,
  val latestCourtAppearance: PagedLatestCourtAppearance,
  val mergedFromCases: List<PagedMergedFromCase>,
  val allAppearancesHaveRecall: Boolean,
) {
  companion object {
    fun from(courtCaseRows: List<CourtCaseRow>): PagedCourtCase {
      val firstCourtCase = courtCaseRows.first()
      val legacyReferences = firstCourtCase.courtCaseLegacyData?.caseReferences?.map { it.offenderCaseReference } ?: emptyList()
      val latestAppearanceCharges = courtCaseRows.filter { it.chargeId != null && it.chargeStatus != EntityStatus.DELETED }.groupBy { it.chargeId!! }
      val mergedFromCases = courtCaseRows.filter { it.mergedFromCaseId != null && it.mergedFromAppearanceId != null }.groupBy { it.mergedFromCaseId!! }
      return PagedCourtCase(
        firstCourtCase.prisonerId,
        firstCourtCase.courtCaseUuid,
        firstCourtCase.courtCaseStatus,
        firstCourtCase.courtCaseLegacyData,
        firstCourtCase.appearanceCount,
        ((firstCourtCase.caseReferences?.split(",") ?: emptyList()) + legacyReferences).toSet(),
        firstCourtCase.firstDayInCustody,
        firstCourtCase.takeIf { it.appearancePeriodLengthType != null && it.appearancePeriodLengthOrder != null }?.let {
          PagedAppearancePeriodLength
            .from(it)
        },
        PagedLatestCourtAppearance.from(firstCourtCase, latestAppearanceCharges),
        mergedFromCases.values.map { PagedMergedFromCase.from(it) },
        courtCaseRows.filter { it.courtAppearanceId != null }.all { it.recallInAppearanceId != null },
      )
    }
  }
}
