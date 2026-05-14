package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain

import java.util.UUID


data class DocumentMetadataUpdate(
  val prisonerId: String,
  val documentId: UUID,
  val status: DocumentMetadataStatus
)

enum class DocumentMetadataStatus(val value: String) {
  AWAITING("Awaiting"),
  ACTIVE("Active"),
  DELETED("Deleted")
}
