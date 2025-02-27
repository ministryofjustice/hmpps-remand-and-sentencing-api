package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.ChargeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.ChargeLegacyData
import java.time.LocalDate
import java.util.UUID

data class Charge(
  val chargeUuid: UUID,
  val offenceCode: String,
  val offenceStartDate: LocalDate?,
  val offenceEndDate: LocalDate?,
  val outcome: ChargeOutcome?,
  val terrorRelated: Boolean?,
  val sentence: Sentence?,
  val legacyData: ChargeLegacyData?,
) {
  companion object {
    fun from(chargeEntity: ChargeEntity): Charge = Charge(
      chargeEntity.chargeUuid,
      chargeEntity.offenceCode,
      chargeEntity.offenceStartDate,
      chargeEntity.offenceEndDate,
      chargeEntity.chargeOutcome?.let { ChargeOutcome.from(it) },
      chargeEntity.terrorRelated,
      chargeEntity.getActiveSentence()?.let { Sentence.from(it) },
      chargeEntity.legacyData,
    )
  }
}
