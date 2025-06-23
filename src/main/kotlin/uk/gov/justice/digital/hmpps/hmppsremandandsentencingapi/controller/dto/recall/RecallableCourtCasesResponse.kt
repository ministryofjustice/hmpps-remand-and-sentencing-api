package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.recall

data class RecallableCourtCasesResponse(
  val totalCases: Int,
  val cases: List<RecallableCourtCase>,
)
