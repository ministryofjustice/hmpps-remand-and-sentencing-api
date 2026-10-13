package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.unconfirmedsentencefix

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.projection.UnconfirmedSentenceFixCourtCaseRow
import java.time.LocalDate

data class UnconfirmedCourtCase(
  val courtCaseUuid: String,
  val caseReference: String?,
  val courtCode: String,
  val sentencingHearingDate: LocalDate,
  val sentences: List<UnconfirmedSentence>,
) {
  companion object {
    fun from(rows: List<UnconfirmedSentenceFixCourtCaseRow>): UnconfirmedCourtCase {
      val firstRow = rows.minBy { it.appearanceDate }
      return UnconfirmedCourtCase(
        firstRow.courtCaseUuid,
        firstRow.caseReference,
        firstRow.courtCode,
        firstRow.appearanceDate,
        rows.groupBy { it.sentenceUuid }.map { UnconfirmedSentence.from(it.value.first()) },
      )
    }
  }
}
