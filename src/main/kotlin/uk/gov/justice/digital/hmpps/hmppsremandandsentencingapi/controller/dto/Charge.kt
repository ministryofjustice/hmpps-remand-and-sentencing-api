package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.ChargeEntity
import java.time.LocalDate
import java.util.UUID

data class Charge(
  val chargeUuid: UUID,
  val offenceCode: String,
  val offenceStartDate: LocalDate,
  val offenceEndDate: LocalDate?,
  val outcome: String,
) {
  companion object {
    fun from(chargeEntity: ChargeEntity): Charge {
      return Charge(
        chargeEntity.chargeUuid,
        chargeEntity.offenceCode,
        chargeEntity.offenceStartDate,
        chargeEntity.offenceEndDate,
        chargeEntity.chargeOutcome.outcomeName,
      )
    }
  }
}
