package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.EventMetadata
import java.util.UUID

data class LegacySentenceCreatedResponse(
  val prisonerId: String,
  val lifetimeUuid: UUID,
  val chargeLifetimeUuid: UUID,
  val appearanceUuid: UUID,
  val courtCaseId: String,
  val eventMetadata: Set<EventMetadata> = emptySet(),
)
