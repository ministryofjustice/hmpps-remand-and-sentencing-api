package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.CourtCaseEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityStatus

data class CourtCase(
  val prisonerId: String,
  val courtCaseUuid: String,
  val latestAppearance: CourtAppearance,
  val appearances: List<CourtAppearance>,
) {
  companion object {
    fun from(courtCaseEntity: CourtCaseEntity): CourtCase {
      return CourtCase(
        courtCaseEntity.prisonerId,
        courtCaseEntity.caseUniqueIdentifier,
        CourtAppearance.from(courtCaseEntity.latestCourtAppearance!!),
        courtCaseEntity.appearances.filter { it.statusId == EntityStatus.ACTIVE }.map { CourtAppearance.from(it) },
      )
    }
  }
}
