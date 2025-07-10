package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.projection.SentenceAfterOnAnotherCourtAppearanceRow

data class SentencesAfterOnOtherCourtAppearanceDetailsResponse(
  val appearances: List<CourtAppearanceAfterSentence>,
) {
  companion object {
    fun from(sentenceAfterOnAnotherCourtAppearanceRows: List<SentenceAfterOnAnotherCourtAppearanceRow>): SentencesAfterOnOtherCourtAppearanceDetailsResponse = SentencesAfterOnOtherCourtAppearanceDetailsResponse(
      sentenceAfterOnAnotherCourtAppearanceRows.groupBy { it.appearanceUuid }.values.map {
        CourtAppearanceAfterSentence.from(it.first())
      },
    )
  }
}
