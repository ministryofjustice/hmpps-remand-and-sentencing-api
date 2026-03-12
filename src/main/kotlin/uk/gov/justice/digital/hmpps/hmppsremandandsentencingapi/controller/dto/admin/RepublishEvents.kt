package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.admin

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.EventMetadata

data class RepublishEvents(
  val eventsMetadata: List<EventMetadata>,
)
