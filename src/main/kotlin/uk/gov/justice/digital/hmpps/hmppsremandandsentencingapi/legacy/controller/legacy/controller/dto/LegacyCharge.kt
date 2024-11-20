package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.legacy.controller.dto

import com.fasterxml.jackson.databind.ObjectMapper
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.ChargeEntity
import java.time.LocalDate
import java.util.UUID

data class LegacyCharge(
  val lifetimeUuid: UUID,
  val nomisOutcomeCode: String?,
  val offenceCode: String,
  val offenceStartDate: LocalDate,
  val offenceEndDate: LocalDate?,
  val legacyData: ChargeLegacyData?,
) {
  companion object {
    fun from(chargeEntity: ChargeEntity, objectMapper: ObjectMapper): LegacyCharge {
      val legacyData = chargeEntity.legacyData?.let { objectMapper.treeToValue<ChargeLegacyData>(it, ChargeLegacyData::class.java) }
      return LegacyCharge(
        chargeEntity.lifetimeChargeUuid,
        legacyData?.nomisOutcomeCode ?: chargeEntity.chargeOutcome?.nomisCode,
        chargeEntity.offenceCode,
        chargeEntity.offenceStartDate,
        chargeEntity.offenceEndDate,
        legacyData,
      )
    }
  }
}
