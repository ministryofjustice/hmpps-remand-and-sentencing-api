package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto

import java.util.UUID

data class UpdateSentenceTypeResponse(
  val updatedCount: Int,
  val updates: List<SentenceTypeUpdateResult>,
)

data class SentenceTypeUpdateResult(
  val sentenceUuid: UUID,
  val sentenceType: String,
)
