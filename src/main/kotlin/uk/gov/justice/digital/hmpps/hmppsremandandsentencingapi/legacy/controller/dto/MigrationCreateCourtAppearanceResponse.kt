package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto

import java.util.UUID

data class MigrationCreateCourtAppearanceResponse(
  val appearanceUuid: UUID,
  val eventId: Long,
)
