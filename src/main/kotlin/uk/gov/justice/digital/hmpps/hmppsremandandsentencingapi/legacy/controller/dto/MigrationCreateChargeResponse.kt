package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto

import java.util.UUID

data class MigrationCreateChargeResponse(
  val chargeUuid: UUID,
  val chargeNOMISId: Long,
)
