package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.projection.ConsecutiveToSentenceAppearance
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.projection.ConsecutiveToSentenceRow
import java.time.LocalDate
import java.util.UUID

data class AppearanceToChainTo(
  val courtCode: String,
  val courtCaseReference: String?,
  val appearanceDate: LocalDate,
  val appearanceUuid: UUID,
  val sentences: List<SentenceToChainTo>,
) {
  companion object {
    fun from(appearance: ConsecutiveToSentenceAppearance, consecutiveToSentenceRows: List<ConsecutiveToSentenceRow>): AppearanceToChainTo = AppearanceToChainTo(
      appearance.courtCode,
      appearance.courtCaseReference,
      appearance.appearanceDate,
      appearance.appearanceUuid,
      consecutiveToSentenceRows.map { SentenceToChainTo.from(it) },
    )
  }
}
