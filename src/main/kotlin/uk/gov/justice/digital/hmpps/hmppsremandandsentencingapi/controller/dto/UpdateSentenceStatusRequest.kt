package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto

import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.SentenceEntityStatus
import java.util.UUID

data class UpdateSentenceStatusRequest(
  @field:NotNull(message = "Appearance UUID is required")
  val appearanceUuid: UUID,

  @field:NotEmpty(message = "Sentence UUIDs list cannot be empty")
  val sentenceUuids: List<UUID>,

  @field:NotNull(message = "Status is required")
  val status: SentenceEntityStatus,

  val reason: String?,
)
