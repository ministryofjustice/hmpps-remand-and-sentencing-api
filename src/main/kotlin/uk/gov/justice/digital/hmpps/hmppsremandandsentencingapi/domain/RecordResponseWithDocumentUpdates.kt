package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain

data class RecordResponseWithDocumentUpdates<T>(
  val record: T,
  val eventsToEmit: MutableSet<EventMetadata>,
  val documentUpdates: List<DocumentMetadataUpdate>,
)
