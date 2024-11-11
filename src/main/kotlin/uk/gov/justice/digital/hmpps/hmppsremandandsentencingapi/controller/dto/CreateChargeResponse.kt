package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto

import java.util.UUID

data class CreateChargeResponse(val chargeUuid: UUID, val offenderChargeId: String?) {
  companion object {
    fun from(createCharge: CreateCharge): CreateChargeResponse {
      return CreateChargeResponse(createCharge.chargeUuid, createCharge.legacyData?.offenderChargeId)
    }
  }
}
