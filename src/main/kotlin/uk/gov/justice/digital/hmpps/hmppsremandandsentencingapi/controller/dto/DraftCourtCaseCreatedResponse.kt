package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.CourtCaseEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.DraftAppearanceEntity

data class DraftCourtCaseCreatedResponse(
  val courtCaseUuid: String,
  val draftAppearances: List<DraftCourtAppearanceCreatedResponse>,
) {
  companion object {
    fun from(courtCaseEntity: CourtCaseEntity, draftAppearances: List<DraftAppearanceEntity>): DraftCourtCaseCreatedResponse {
      return DraftCourtCaseCreatedResponse(courtCaseEntity.caseUniqueIdentifier, draftAppearances.map { draftAppearanceEntity -> DraftCourtAppearanceCreatedResponse.from(draftAppearanceEntity) })
    }
  }
}
