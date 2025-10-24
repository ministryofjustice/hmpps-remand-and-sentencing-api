package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.sentence.details

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.ChargeOutcome
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.MergedFromCase
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.ChargeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.ChargeLegacyData
import java.time.LocalDate
import java.util.*

data class SentenceDetailsCharge(
  val chargeUuid: UUID,
  val offenceCode: String,
  val offenceStartDate: LocalDate?,
  val offenceEndDate: LocalDate?,
  val outcome: ChargeOutcome?,
  val legacyData: ChargeLegacyData?,
  val mergedFromCase: MergedFromCase?,
) {
  companion object {
    fun from(chargeEntity: ChargeEntity): SentenceDetailsCharge = SentenceDetailsCharge(
      chargeEntity.chargeUuid,
      chargeEntity.offenceCode,
      chargeEntity.offenceStartDate,
      chargeEntity.offenceEndDate,
      chargeEntity.chargeOutcome?.let { ChargeOutcome.from(it) },
      chargeEntity.legacyData,
      chargeEntity.mergedFromCourtCase?.latestCourtAppearance?.let {
        MergedFromCase.from(
          it,
          chargeEntity.mergedFromDate,
        )
      },
    )
  }
}
