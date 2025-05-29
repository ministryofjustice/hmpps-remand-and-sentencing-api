package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.reconciliation

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.ChargeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.ChargeLegacyData
import java.time.LocalDate
import java.util.UUID

data class ReconciliationCharge(
  val chargeUuid: UUID,
  val offenceCode: String,
  val offenceStartDate: LocalDate?,
  val offenceEndDate: LocalDate?,
  val nomisOutcomeCode: String?,
  val legacyData: ChargeLegacyData?,
  val sentence: ReconciliationSentence?,
) {
  companion object {
    fun from(chargeEntity: ChargeEntity): ReconciliationCharge = ReconciliationCharge(
      chargeEntity.chargeUuid,
      chargeEntity.offenceCode,
      chargeEntity.offenceStartDate,
      chargeEntity.offenceEndDate,
      chargeEntity.legacyData?.nomisOutcomeCode ?: chargeEntity.chargeOutcome?.nomisCode,
      chargeEntity.legacyData,
      chargeEntity.sentences.firstOrNull { it.statusId != EntityStatus.DELETED }?.let { ReconciliationSentence.from(it) },
    )
  }
}
