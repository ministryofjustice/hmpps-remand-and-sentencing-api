package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.projection.SentenceAfterOnAnotherCourtAppearanceRow
import java.time.LocalDate

data class CourtAppearanceAfterSentence(
  val caseReference: String?,
  val appearanceDate: LocalDate,
  val courtCode: String,
) {
  companion object {
    fun from(sentenceAfterOnAnotherCourtAppearanceRow: SentenceAfterOnAnotherCourtAppearanceRow): CourtAppearanceAfterSentence = CourtAppearanceAfterSentence(
      sentenceAfterOnAnotherCourtAppearanceRow.caseReference,
      sentenceAfterOnAnotherCourtAppearanceRow.appearanceDate,
      sentenceAfterOnAnotherCourtAppearanceRow.courtCode,
    )
  }
}
