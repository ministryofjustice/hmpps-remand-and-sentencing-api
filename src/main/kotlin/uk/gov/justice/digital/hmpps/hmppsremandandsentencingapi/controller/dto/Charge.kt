package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto

import com.fasterxml.jackson.databind.JsonNode
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.ChargeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityStatus
import java.time.LocalDate
import java.util.UUID

data class Charge(
  val chargeUuid: UUID,
  val lifetimeUuid: UUID,
  val offenceCode: String,
  val offenceStartDate: LocalDate?,
  val offenceEndDate: LocalDate?,
  val outcome: ChargeOutcome?,
  val terrorRelated: Boolean?,
  val sentence: Sentence?,
  val legacyData: JsonNode?,
  val status: EntityStatus,
) {
  companion object {
    fun from(chargeEntity: ChargeEntity): Charge {
      return Charge(
        chargeEntity.chargeUuid,
        chargeEntity.lifetimeChargeUuid,
        chargeEntity.offenceCode,
        chargeEntity.offenceStartDate,
        chargeEntity.offenceEndDate,
        chargeEntity.chargeOutcome?.let { ChargeOutcome.from(it) },
        chargeEntity.terrorRelated,
        chargeEntity.getActiveSentence()?.let { Sentence.from(it) },
        chargeEntity.legacyData,
        chargeEntity.statusId,
      )
    }
  }
}
