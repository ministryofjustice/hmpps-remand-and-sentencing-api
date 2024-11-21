package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto

import java.util.UUID

data class CreateChargeResponse(val chargeUuid: UUID) {
  companion object {
    fun from(createCharge: CreateCharge): CreateChargeResponse {
      return CreateChargeResponse(createCharge.chargeUuid)
    }
  }
}
