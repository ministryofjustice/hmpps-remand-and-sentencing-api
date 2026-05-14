package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain

data class RecordResponseWithDocumentUpdates<T>(
  val recordResponse: RecordResponse<T>,
  val documentUpdates: List<DocumentMetadataUpdate>
)

