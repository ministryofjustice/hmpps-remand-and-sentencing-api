package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain

data class RecordResponse<T>(
  val record: T,
  val eventsToEmit: MutableSet<EventMetadata>,
)
