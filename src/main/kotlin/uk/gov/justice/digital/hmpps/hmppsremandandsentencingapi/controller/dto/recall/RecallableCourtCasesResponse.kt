package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.recall

data class RecallableCourtCasesResponse(
  val cases: List<RecallableCourtCase>,
)
