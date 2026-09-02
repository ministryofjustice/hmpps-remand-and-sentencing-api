package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto

import java.util.UUID

data class UpdateSentenceStatusResponse(
  val updatedSentenceUuids: List<UUID>,
)
