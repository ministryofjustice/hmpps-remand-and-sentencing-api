package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.AppearanceOutcomeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ReferenceEntityStatus
import java.util.UUID

data class CourtAppearanceOutcome(
  val outcomeUuid: UUID,
  val outcomeName: String,
  val nomisCode: String,
  val outcomeType: String,
  val displayOrder: Int,
  val relatedChargeOutcomeUuid: UUID,
  val isSubList: Boolean,
  val dispositionCode: String,
  val status: ReferenceEntityStatus,
) {
  companion object {
    fun from(appearanceOutcomeEntity: AppearanceOutcomeEntity): CourtAppearanceOutcome = CourtAppearanceOutcome(
      appearanceOutcomeEntity.outcomeUuid,
      appearanceOutcomeEntity.outcomeName,
      appearanceOutcomeEntity.nomisCode,
      appearanceOutcomeEntity.outcomeType,
      appearanceOutcomeEntity.displayOrder,
      appearanceOutcomeEntity.relatedChargeOutcomeUuid,
      appearanceOutcomeEntity.isSubList,
      appearanceOutcomeEntity.dispositionCode,
      appearanceOutcomeEntity.status,
    )
  }
}
