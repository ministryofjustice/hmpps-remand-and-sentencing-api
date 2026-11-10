package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.person

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Status indicators for a list of people")
data class PeopleStatusRequest(
  val personIds: Set<String>,
)
