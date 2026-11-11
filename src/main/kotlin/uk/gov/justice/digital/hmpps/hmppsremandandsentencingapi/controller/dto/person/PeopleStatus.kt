package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.person

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Status indicators for a map of people and their status")
data class PeopleStatus(
  val peopleStatus: Map<String, PersonStatus>,
)
