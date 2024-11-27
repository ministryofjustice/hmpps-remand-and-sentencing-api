package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto

import com.fasterxml.jackson.databind.JsonNode

data class DraftCreateCourtAppearance(
  val sessionBlob: JsonNode,
)
