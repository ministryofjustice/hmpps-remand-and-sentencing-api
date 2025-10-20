package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.CourtCaseEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.CourtCaseEntityStatus

data class CourtCases(
  val courtCases: List<CourtCase>,
) {
  companion object {
    fun from(courtCaseEntities: List<CourtCaseEntity>): CourtCases = CourtCases(courtCaseEntities.filter { it.statusId == CourtCaseEntityStatus.ACTIVE }.map { CourtCase.from(it) })
  }
}
