package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.projection.ConsecutiveToSentenceAppearance
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.projection.ConsecutiveToSentenceRow
import java.time.LocalDate

data class AppearanceToChainTo(
  val courtCode: String,
  val courtCaseReference: String?,
  val appearanceDate: LocalDate,
  val sentences: List<SentenceToChainTo>,
) {
  companion object {
    fun from(appearance: ConsecutiveToSentenceAppearance, consecutiveToSentenceRows: List<ConsecutiveToSentenceRow>): AppearanceToChainTo = AppearanceToChainTo(
      appearance.courtCode,
      appearance.courtCaseReference,
      appearance.appearanceDate,
      consecutiveToSentenceRows.map { SentenceToChainTo.from(it.charge, it.sentence) },
    )
  }
}
