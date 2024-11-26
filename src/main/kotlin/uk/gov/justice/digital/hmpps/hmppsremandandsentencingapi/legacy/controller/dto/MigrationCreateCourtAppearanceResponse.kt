package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto

import java.util.UUID

data class MigrationCreateCourtAppearanceResponse(
  val lifetimeUuid: UUID,
  val eventId: String,
)
