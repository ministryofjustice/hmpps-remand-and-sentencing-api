package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.CourtAppearanceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.projection.CourtCaseAppearanceChargeSentence
import java.time.LocalDate

data class AppearanceToChainTo(
  val courtCode: String,
  val courtCaseReference: String?,
  val appearanceDate: LocalDate,
  val sentences: List<SentenceToChainTo>,
) {
  companion object {
    fun from(appearance: CourtAppearanceEntity, courtCaseAppearanceChargeSentences: List<CourtCaseAppearanceChargeSentence>): AppearanceToChainTo = AppearanceToChainTo(
      appearance.courtCode,
      appearance.courtCaseReference,
      appearance.appearanceDate,
      courtCaseAppearanceChargeSentences.map { SentenceToChainTo.from(it.charge, it.sentence) },
    )
  }
}
