package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.reconciliation

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.CourtCaseEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.CourtCaseLegacyData

data class ReconciliationCourtCase(
  val courtCaseUuid: String,
  val prisonerId: String,
  val active: Boolean,
  val merged: Boolean,
  val courtCaseLegacyData: CourtCaseLegacyData?,
  val appearances: List<ReconciliationCourtAppearance>,
) {
  companion object {
    fun from(courtCaseEntity: CourtCaseEntity): ReconciliationCourtCase = ReconciliationCourtCase(
      courtCaseEntity.caseUniqueIdentifier,
      courtCaseEntity.prisonerId,
      courtCaseEntity.statusId == EntityStatus.ACTIVE,
      courtCaseEntity.statusId == EntityStatus.MERGED,
      courtCaseEntity.legacyData,
      courtCaseEntity.appearances.filter { it.statusId != EntityStatus.DELETED }.map { ReconciliationCourtAppearance.from(it) },
    )
  }
}
