package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.projection.ConsecutiveToSentenceRow

data class SentenceConsecutiveToDetailsResponse(
  val sentences: List<SentenceConsecutiveToDetails>,
) {
  companion object {
    fun from(consecutiveToSentenceRows: List<ConsecutiveToSentenceRow>): SentenceConsecutiveToDetailsResponse = SentenceConsecutiveToDetailsResponse(
      consecutiveToSentenceRows.groupBy { it.sentenceUuid }.map { (_, records) ->
        SentenceConsecutiveToDetails.from(records.minBy { it.appearanceDate })
      },
    )
  }
}
