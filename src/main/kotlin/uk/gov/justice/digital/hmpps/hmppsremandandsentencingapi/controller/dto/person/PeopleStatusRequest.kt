package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.person

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotEmpty

@Schema(description = "Status indicators for a list of people")
data class PeopleStatusRequest(
  @field:NotEmpty
  val personIds: Set<String>,
)
