package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.CourtCaseEntity
import java.time.LocalDate

data class MergedFromCaseDetails(
  val latestAppearanceCaseReference: String?,
  val latestAppearanceDate: LocalDate?,
  val latestAppearanceCourtCode: String?,
) {
  companion object {
    fun from(courtCaseEntity: CourtCaseEntity): MergedFromCaseDetails = MergedFromCaseDetails(
      courtCaseEntity.latestCourtAppearance?.courtCaseReference,
      courtCaseEntity.latestCourtAppearance?.appearanceDate,
      courtCaseEntity.latestCourtAppearance?.courtCode,
    )
  }
}
