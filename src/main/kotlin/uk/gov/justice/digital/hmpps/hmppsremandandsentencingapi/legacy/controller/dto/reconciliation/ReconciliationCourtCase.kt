package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.reconciliation

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.AppearanceTypeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.CourtCaseEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.CourtAppearanceEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.CourtCaseEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.CourtCaseLegacyData

data class ReconciliationCourtCase(
  val courtCaseUuid: String,
  val prisonerId: String,
  val active: Boolean,
  val merged: Boolean,
  val status: CourtCaseEntityStatus,
  val courtCaseLegacyData: CourtCaseLegacyData?,
  val appearances: List<ReconciliationCourtAppearance>,
) {
  companion object {
    fun from(courtCaseEntity: CourtCaseEntity, defaultAppearanceType: AppearanceTypeEntity): ReconciliationCourtCase {
      val courtAppearances = courtCaseEntity.appearances.filter { it.statusId != CourtAppearanceEntityStatus.DELETED }
      val courtAppearanceTypes = courtAppearances.filter { it.nextCourtAppearance != null }.map { it.nextCourtAppearance!! }.groupBy { it.futureSkeletonAppearance.id }.mapValues { it.value.maxBy { it.futureSkeletonAppearance.updatedAt ?: it.futureSkeletonAppearance.createdAt }.appearanceType }
      return ReconciliationCourtCase(
        courtCaseEntity.caseUniqueIdentifier,
        courtCaseEntity.prisonerId,
        courtCaseEntity.statusId == CourtCaseEntityStatus.ACTIVE,
        courtCaseEntity.statusId == CourtCaseEntityStatus.MERGED,
        courtCaseEntity.statusId,
        courtCaseEntity.legacyData,
        courtAppearances.map { ReconciliationCourtAppearance.from(it, courtAppearanceTypes.getOrDefault(it.id, defaultAppearanceType)) },
      )
    }
  }
}
