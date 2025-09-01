package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

@Schema(description = "This contains the details used to validate loops in consecutive chains")
data class ConsecutiveChainValidationRequest(
  val prisonerId: String,
  val appearanceUuid: UUID,
  val sourceSentenceUuid: UUID,
  val targetSentenceUuid: UUID,
  @field:Schema(description = "The UI sentences for the appearance in the UI session")
  val sentences: List<SentenceDetailsForConsecValidation>,
)
