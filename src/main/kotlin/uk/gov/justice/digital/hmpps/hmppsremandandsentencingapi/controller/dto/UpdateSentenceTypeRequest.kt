package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto

import jakarta.validation.Valid
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import java.util.UUID

data class UpdateSentenceTypeRequest(
  @field:Valid
  @field:NotEmpty(message = "Updates list cannot be empty")
  val updates: List<SentenceTypeUpdate>,
)

data class SentenceTypeUpdate(
  @field:NotNull(message = "Sentence UUID is required")
  val sentenceUuid: UUID,

  @field:NotNull(message = "Sentence type is required")
  val sentenceType: String,
)
