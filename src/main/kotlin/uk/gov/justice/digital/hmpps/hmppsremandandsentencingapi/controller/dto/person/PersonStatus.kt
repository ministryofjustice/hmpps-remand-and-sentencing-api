package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.person

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Status indicators for a person")
data class PersonStatus(
  val isOnRemand: Boolean,
)
