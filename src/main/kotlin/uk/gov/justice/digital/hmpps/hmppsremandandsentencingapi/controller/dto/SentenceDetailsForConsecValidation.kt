package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

@Schema(description = "Shows consec relationship of a sentence")
data class SentenceDetailsForConsecValidation(
  val sentenceUuid: UUID,
  val consecutiveToSentenceUuid: UUID?,
)
