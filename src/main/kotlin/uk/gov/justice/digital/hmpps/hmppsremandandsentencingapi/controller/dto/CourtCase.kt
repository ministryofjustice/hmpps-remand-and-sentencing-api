package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto

import com.fasterxml.jackson.databind.JsonNode
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.CourtCaseEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityStatus

data class CourtCase(
  val prisonerId: String,
  val courtCaseUuid: String,
  val status: EntityStatus,
  val latestAppearance: CourtAppearance?,
  val appearances: List<CourtAppearance>,
  val legacyData: JsonNode?,
) {
  companion object {
    fun from(courtCaseEntity: CourtCaseEntity): CourtCase {
      return CourtCase(
        courtCaseEntity.prisonerId,
        courtCaseEntity.caseUniqueIdentifier,
        courtCaseEntity.statusId,
        courtCaseEntity.latestCourtAppearance?.let { CourtAppearance.from(it) },
        courtCaseEntity.appearances.filter { it.statusId == EntityStatus.ACTIVE }.map { CourtAppearance.from(it) },
        courtCaseEntity.legacyData,
      )
    }
  }
}
