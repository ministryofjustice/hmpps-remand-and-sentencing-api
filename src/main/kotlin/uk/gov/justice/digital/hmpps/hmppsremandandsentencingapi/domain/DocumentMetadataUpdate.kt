package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain

import com.fasterxml.jackson.annotation.JsonValue
import java.util.UUID

data class DocumentMetadataUpdate(
  val prisonerId: String,
  val documentId: UUID,
  val status: DocumentMetadataStatus,
)

enum class DocumentMetadataStatus(val value: String) {
  AWAITING("Awaiting"),
  ACTIVE("Active"),
  DELETED("Deleted"),
  ;

  @JsonValue
  fun toValue(): String = value
}
