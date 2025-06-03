package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.paged

import java.util.UUID

data class PagedChargeOutcome(
  val outcomeUuid: UUID,
  val outcomeName: String?,
)
