package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.hmctsresultcode

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.ChargeOutcome
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.HmctsResultCodeEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.ReferenceEntityStatus
import java.util.UUID

data class HmctsResultCode(
  val resultUuid: UUID,
  val resultName: String,
  val resultCode: String,
  val outcome: ChargeOutcome,
  val status: ReferenceEntityStatus,
) {
  companion object {
    fun from(hmctsResultCodeEntity: HmctsResultCodeEntity): HmctsResultCode = HmctsResultCode(
      hmctsResultCodeEntity.resultUuid,
      hmctsResultCodeEntity.resultName,
      hmctsResultCodeEntity.resultCode,
      ChargeOutcome.from(hmctsResultCodeEntity.chargeOutcome),
      hmctsResultCodeEntity.status,
    )
  }
}
