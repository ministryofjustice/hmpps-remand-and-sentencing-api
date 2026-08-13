package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain

import java.util.UUID

data class DocumentStatusUpdates(
  val documentId: UUID,
  val status: DocumentMetadataStatus,
  val caseReference: String? = null,
)

enum class DocumentMetadataStatus {
  AWAITING,
  ACTIVE,
  DELETED,
}
