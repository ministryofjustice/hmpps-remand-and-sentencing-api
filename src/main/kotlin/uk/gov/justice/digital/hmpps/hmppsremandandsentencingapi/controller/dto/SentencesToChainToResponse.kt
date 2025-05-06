package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.projection.CourtCaseAppearanceChargeSentence

data class SentencesToChainToResponse(
  val appearances: List<AppearanceToChainTo>,
) {
  companion object {
    fun from(courtCaseAppearanceChargeSentences: List<CourtCaseAppearanceChargeSentence>): SentencesToChainToResponse = SentencesToChainToResponse(
      courtCaseAppearanceChargeSentences.groupBy { it.appearance }.map { (appearance, records) ->
        AppearanceToChainTo.from(appearance, records)
      },
    )
  }
}
