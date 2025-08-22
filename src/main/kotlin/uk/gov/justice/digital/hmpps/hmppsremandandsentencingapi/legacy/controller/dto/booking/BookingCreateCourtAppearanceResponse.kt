package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.booking

import java.util.UUID

data class BookingCreateCourtAppearanceResponse(
  val appearanceUuid: UUID,
  val eventId: Long,
)
