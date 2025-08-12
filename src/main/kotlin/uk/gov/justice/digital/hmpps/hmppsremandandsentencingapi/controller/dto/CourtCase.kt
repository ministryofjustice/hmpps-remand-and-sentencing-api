package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.CourtCaseEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.CourtCaseLegacyData

data class CourtCase(
  val prisonerId: String,
  val courtCaseUuid: String,
  val status: EntityStatus,
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
      courtCaseEntity.appearances.filter { it.statusId == EntityStatus.ACTIVE }.map { CourtAppearance.from(it) },
      courtCaseEntity.legacyData,
      mergedToCaseDetails = courtCaseEntity.mergedToCase
        ?.takeIf { it.statusId == EntityStatus.ACTIVE && it.latestCourtAppearance?.statusId == EntityStatus.ACTIVE }
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
