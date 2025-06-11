package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.domain

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.EventMetadata

data class UnlinkEventsToEmit(
  val courtCaseEventMetadata: EventMetadata?,
  val chargesEventMetadata: List<EventMetadata>,
)
