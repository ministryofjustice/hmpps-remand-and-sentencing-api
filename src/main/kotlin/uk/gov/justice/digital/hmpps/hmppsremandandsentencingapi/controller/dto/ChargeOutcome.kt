package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.ChargeOutcomeEntity
import java.util.UUID

data class ChargeOutcome(
  val outcomeUuid: UUID,
  val outcomeName: String,
  val nomisCode: String,
  val outcomeType: String,
  val displayOrder: Int,
  val isSubList: Boolean,
) {
  companion object {
    fun from(chargeOutcomeEntity: ChargeOutcomeEntity): ChargeOutcome {
      return ChargeOutcome(
        chargeOutcomeEntity.outcomeUuid,
        chargeOutcomeEntity.outcomeName,
        chargeOutcomeEntity.nomisCode,
        chargeOutcomeEntity.outcomeType,
        chargeOutcomeEntity.displayOrder,
        chargeOutcomeEntity.isSubList,
      )
    }
  }
}
