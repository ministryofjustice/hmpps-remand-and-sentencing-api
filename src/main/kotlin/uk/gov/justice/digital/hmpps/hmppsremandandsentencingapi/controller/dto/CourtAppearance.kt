package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.CourtAppearanceEntity
import java.time.LocalDate
import java.util.UUID

data class CourtAppearance(
  val appearanceUuid: UUID,
  val outcome: String,
  val courtCode: String,
  val courtCaseReference: String,
  val appearanceDate: LocalDate,
  val warrantId: String?,
  val nextCourtAppearance: NextCourtAppearance?,
  val charges: List<Charge>,
) {
  companion object {
    fun from(courtAppearanceEntity: CourtAppearanceEntity): CourtAppearance {
      return CourtAppearance(
        courtAppearanceEntity.appearanceUuid,
        courtAppearanceEntity.appearanceOutcome.outcomeName,
        courtAppearanceEntity.courtCode,
        courtAppearanceEntity.courtCaseReference,
        courtAppearanceEntity.appearanceDate,
        courtAppearanceEntity.warrantId,
        courtAppearanceEntity.nextCourtAppearance?.let { NextCourtAppearance.from(it) },
        courtAppearanceEntity.charges.map { Charge.from(it) },
      )
    }
  }
}
