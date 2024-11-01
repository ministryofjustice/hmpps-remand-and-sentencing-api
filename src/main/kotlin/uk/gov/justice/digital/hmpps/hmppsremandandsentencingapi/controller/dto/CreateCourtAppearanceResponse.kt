package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto

import java.util.UUID

data class CreateCourtAppearanceResponse(val appearanceUuid: UUID, val eventId: String?)
