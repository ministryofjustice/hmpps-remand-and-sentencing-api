package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain

import java.util.UUID

data class DocumentStatusUpdates(
  val documentId: UUID,
  val status: DocumentMetadataStatus,
)

enum class DocumentMetadataStatus {
  AWAITING,
  ACTIVE,
  DELETED,
}
