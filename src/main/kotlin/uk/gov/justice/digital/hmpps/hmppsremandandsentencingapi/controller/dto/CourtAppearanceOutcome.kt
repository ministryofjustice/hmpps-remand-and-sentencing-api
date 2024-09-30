package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.AppearanceOutcomeEntity
import java.util.UUID

data class CourtAppearanceOutcome(
  val outcomeUuid: UUID,
  val outcomeName: String,
) {
  companion object {
    fun from(appearanceOutcomeEntity: AppearanceOutcomeEntity): CourtAppearanceOutcome {
      return CourtAppearanceOutcome(appearanceOutcomeEntity.outcomeUuid, appearanceOutcomeEntity.outcomeName)
    }
  }
}
