package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.CourtAppearanceEntity
import java.time.LocalDate

data class MergedFromCase(
  val caseReference: String?,
  val courtCode: String,
  val warrantDate: LocalDate,
  val mergedFromDate: LocalDate?,
) {
  companion object {
    fun from(courtAppearanceEntity: CourtAppearanceEntity, mergedFromDate: LocalDate?): MergedFromCase = MergedFromCase(
      courtAppearanceEntity.courtCaseReference,
      courtAppearanceEntity.courtCode,
      courtAppearanceEntity.appearanceDate,
      mergedFromDate,
    )
  }
}
