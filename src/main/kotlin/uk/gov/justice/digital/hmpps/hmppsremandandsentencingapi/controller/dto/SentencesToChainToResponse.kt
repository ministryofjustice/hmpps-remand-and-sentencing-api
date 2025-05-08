package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.projection.ConsecutiveToSentenceRow

data class SentencesToChainToResponse(
  val appearances: List<AppearanceToChainTo>,
) {
  companion object {
    fun from(consecutiveToSentenceRows: List<ConsecutiveToSentenceRow>): SentencesToChainToResponse = SentencesToChainToResponse(
      consecutiveToSentenceRows.groupBy { it.toConsecutiveToSentenceAppearance() }.map { (appearance, records) ->
        AppearanceToChainTo.from(appearance, records)
      },
    )
  }
}
