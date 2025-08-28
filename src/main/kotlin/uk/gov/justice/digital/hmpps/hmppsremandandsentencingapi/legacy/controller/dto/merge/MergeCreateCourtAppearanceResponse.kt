package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.merge

import java.util.UUID

data class MergeCreateCourtAppearanceResponse(
  val appearanceUuid: UUID,
  val eventId: Long,
)
