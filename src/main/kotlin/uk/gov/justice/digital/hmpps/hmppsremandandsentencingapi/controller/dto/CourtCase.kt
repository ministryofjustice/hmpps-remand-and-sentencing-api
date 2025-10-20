package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.CourtCaseEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.CourtAppearanceEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.CourtCaseEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.CourtCaseLegacyData

data class CourtCase(
  val prisonerId: String,
  val courtCaseUuid: String,
  val status: CourtCaseEntityStatus,
  val latestAppearance: CourtAppearance?,
  val appearances: List<CourtAppearance>,
  val legacyData: CourtCaseLegacyData?,
  val mergedToCaseDetails: MergedToCaseDetails?,
) {
  companion object {
    fun from(courtCaseEntity: CourtCaseEntity): CourtCase = CourtCase(
      courtCaseEntity.prisonerId,
      courtCaseEntity.caseUniqueIdentifier,
      courtCaseEntity.statusId,
      courtCaseEntity.latestCourtAppearance?.let { CourtAppearance.from(it) },
      courtCaseEntity.appearances.filter { it.statusId == CourtAppearanceEntityStatus.ACTIVE }.map { CourtAppearance.from(it) },
      courtCaseEntity.legacyData,
      mergedToCaseDetails = courtCaseEntity.mergedToCase
        ?.takeIf { it.statusId == CourtCaseEntityStatus.ACTIVE && it.latestCourtAppearance?.statusId == CourtAppearanceEntityStatus.ACTIVE }
        ?.let { mergedTo ->
          val latestAppearance = mergedTo.latestCourtAppearance
          MergedToCaseDetails(
            mergedToDate = courtCaseEntity.mergedToDate,
            caseReference = mergedTo.latestCourtAppearance?.courtCaseReference,
            courtCode = latestAppearance?.courtCode,
            warrantDate = latestAppearance?.appearanceDate,
          )
        },
    )
  }
}
