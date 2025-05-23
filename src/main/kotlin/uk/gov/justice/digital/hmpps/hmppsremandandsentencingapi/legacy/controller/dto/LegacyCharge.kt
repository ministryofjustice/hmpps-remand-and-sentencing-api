package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.ChargeEntity
import java.time.LocalDate
import java.util.UUID

data class LegacyCharge(
  val prisonerId: String,
  val courtCaseUuid: String,
  val lifetimeUuid: UUID,
  val nomisOutcomeCode: String?,
  val offenceCode: String,
  val offenceStartDate: LocalDate?,
  val offenceEndDate: LocalDate?,
  val legacyData: ChargeLegacyData?,
) {
  companion object {
    fun from(chargeEntity: ChargeEntity): LegacyCharge = LegacyCharge(
      chargeEntity.appearanceCharges.first().appearance!!.courtCase.prisonerId,
      chargeEntity.appearanceCharges.first().appearance!!.courtCase.caseUniqueIdentifier,
      chargeEntity.chargeUuid,
      chargeEntity.legacyData?.nomisOutcomeCode ?: chargeEntity.chargeOutcome?.nomisCode,
      chargeEntity.offenceCode,
      chargeEntity.offenceStartDate,
      chargeEntity.offenceEndDate,
      chargeEntity.legacyData,
    )
  }
}
