package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.unconfirmedsentencefix

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.projection.UnconfirmedSentenceFixCourtCaseRow

data class UnconfirmedCourtCases(
  val courtCases: List<UnconfirmedCourtCase>,
) {
  companion object {
    fun from(unconfirmedSentenceFixCourtCaseRow: List<UnconfirmedSentenceFixCourtCaseRow>): UnconfirmedCourtCases = UnconfirmedCourtCases(
      unconfirmedSentenceFixCourtCaseRow.groupBy { it.courtCaseUuid }.map { (_, unconfirmedRows) ->
        UnconfirmedCourtCase.from(unconfirmedRows)
      },
    )
  }
}
