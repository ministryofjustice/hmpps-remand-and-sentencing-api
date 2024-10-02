package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto

import com.fasterxml.jackson.annotation.JsonAnyGetter
import com.fasterxml.jackson.annotation.JsonAnySetter

data class CourtAppearanceLegacyData(
  val outcomeReason: String?,
  @JsonAnySetter
  @get:JsonAnyGetter
  val otherFields: Map<String, Any> = hashMapOf(),
)
