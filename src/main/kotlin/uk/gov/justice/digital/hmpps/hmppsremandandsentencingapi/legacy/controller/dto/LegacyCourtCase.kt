package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.CourtCaseEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityStatus

data class LegacyCourtCase(
  val courtCaseUuid: String,
  val prisonerId: String,
  val active: Boolean,
  val legacyData: CourtCaseLegacyData?,
) {
  companion object {
    fun from(courtCaseEntity: CourtCaseEntity, legacyData: CourtCaseLegacyData?): LegacyCourtCase {
      return LegacyCourtCase(courtCaseEntity.caseUniqueIdentifier, courtCaseEntity.prisonerId, courtCaseEntity.statusId == EntityStatus.ACTIVE, legacyData)
    }
  }
}
