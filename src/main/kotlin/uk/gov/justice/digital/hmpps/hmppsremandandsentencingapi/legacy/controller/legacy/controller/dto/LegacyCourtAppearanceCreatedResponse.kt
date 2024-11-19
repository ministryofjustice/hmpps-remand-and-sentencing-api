package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.legacy.controller.dto

import java.util.UUID

data class LegacyCourtAppearanceCreatedResponse(
  val lifetimeUuid: UUID,
  val courtCaseUuid: String,
  val prisonerId: String,
)
