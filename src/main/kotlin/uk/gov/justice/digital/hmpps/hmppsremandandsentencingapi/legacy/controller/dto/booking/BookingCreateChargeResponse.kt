package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.booking

import java.util.UUID

data class BookingCreateChargeResponse(
  val chargeUuid: UUID,
  val chargeNOMISId: Long,
)
