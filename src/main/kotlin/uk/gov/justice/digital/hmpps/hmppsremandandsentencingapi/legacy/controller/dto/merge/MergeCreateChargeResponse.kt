package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.merge

import java.util.UUID

data class MergeCreateChargeResponse(
  val chargeUuid: UUID,
  val chargeNOMISId: Long,
)
