package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.recall

data class PrisonerRecallsResponse(
  val recalls: List<Recall>,
  val prisonerRecallTotal: Long,
)
