package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.CourtCaseEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityStatus
import java.time.LocalDate

data class LegacyCourtCase(
  val courtCaseUuid: String,
  val prisonerId: String,
  val active: Boolean,
  val startDate: LocalDate?,
  val courtId: String?,
  val caseReference: String?,
  val caseReferences: List<CaseReferenceLegacyData>,
) {
  companion object {
    fun from(courtCaseEntity: CourtCaseEntity): LegacyCourtCase {
      val firstAppearance = courtCaseEntity.appearances.firstOrNull { entity -> entity.statusId == EntityStatus.ACTIVE }
      return LegacyCourtCase(courtCaseEntity.caseUniqueIdentifier, courtCaseEntity.prisonerId, courtCaseEntity.statusId == EntityStatus.ACTIVE, firstAppearance?.appearanceDate, firstAppearance?.courtCode, firstAppearance?.courtCaseReference, courtCaseEntity.legacyData?.caseReferences ?: emptyList())
    }
  }
}
