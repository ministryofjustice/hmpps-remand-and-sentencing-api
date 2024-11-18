package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.legacy.controller.dto

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.CourtCaseEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityStatus

data class LegacyCourtCase(
  val courtCaseUuid: String,
  val prisonerId: String,
  val active: Boolean,
) {
  companion object {
    fun from(courtCaseEntity: CourtCaseEntity): LegacyCourtCase {
      return LegacyCourtCase(courtCaseEntity.caseUniqueIdentifier, courtCaseEntity.prisonerId, courtCaseEntity.statusId == EntityStatus.ACTIVE)
    }
  }
}
