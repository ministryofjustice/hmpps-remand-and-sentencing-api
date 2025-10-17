package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.reconciliation

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.CourtCaseEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.CourtCaseLegacyData
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.service.LegacyCourtAppearanceService.Companion.DEFAULT_APPEARANCE_TYPE_UUD

data class ReconciliationCourtCase(
  val courtCaseUuid: String,
  val prisonerId: String,
  val active: Boolean,
  val merged: Boolean,
  val courtCaseLegacyData: CourtCaseLegacyData?,
  val appearances: List<ReconciliationCourtAppearance>,
) {
  companion object {
    fun from(courtCaseEntity: CourtCaseEntity): ReconciliationCourtCase {
      val courtAppearances = courtCaseEntity.appearances.filter { it.statusId != EntityStatus.DELETED }
      val courtAppearanceTypes = courtAppearances.filter { it.nextCourtAppearance != null }.map { it.nextCourtAppearance!! }.groupBy { it.futureSkeletonAppearance.id }.mapValues { it.value.maxBy { it.futureSkeletonAppearance.updatedAt ?: it.futureSkeletonAppearance.createdAt }.appearanceType.appearanceTypeUuid }
      return ReconciliationCourtCase(
        courtCaseEntity.caseUniqueIdentifier,
        courtCaseEntity.prisonerId,
        courtCaseEntity.statusId == EntityStatus.ACTIVE,
        courtCaseEntity.statusId == EntityStatus.MERGED,
        courtCaseEntity.legacyData,
        courtAppearances.map { ReconciliationCourtAppearance.from(it, courtAppearanceTypes.getOrDefault(it.id, DEFAULT_APPEARANCE_TYPE_UUD)) },
      )
    }
  }
}
