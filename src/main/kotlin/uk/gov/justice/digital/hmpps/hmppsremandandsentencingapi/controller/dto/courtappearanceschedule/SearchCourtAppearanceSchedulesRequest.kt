package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.courtappearanceschedule

import java.util.UUID

data class SearchCourtAppearanceSchedulesRequest(
  val uuids: List<UUID>,
)
