package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.recall

import io.swagger.v3.oas.annotations.media.Schema

data class PrisonerRecallsResponse(
  val recalls: List<Recall>,
  @get:Schema(description = "Total number of recalls for the prisoner after NOMIS grouping")
  val prisonerRecallTotal: Int,
)
