package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto

import java.util.UUID

data class UpdateSentenceTypeResponse(
  val updatedSentenceUuids: List<UUID>,
)
