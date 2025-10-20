package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.SentenceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.CourtAppearanceEntityStatus
import java.util.UUID

data class LegacySentenceDeletedResponse(
  val prisonerId: String,
  val lifetimeUuid: UUID,
  val chargeLifetimeUuid: UUID,
  val appearanceUuid: UUID,
  val courtCaseId: String,
) {
  companion object {
    fun from(sentenceEntity: SentenceEntity): LegacySentenceDeletedResponse? = sentenceEntity.charge.appearanceCharges
      .map { it.appearance!! }
      .filter { it.statusId == CourtAppearanceEntityStatus.ACTIVE }
      .maxByOrNull { it.appearanceDate }?.let { courtAppearance ->
        LegacySentenceDeletedResponse(
          courtAppearance.courtCase.prisonerId,
          sentenceEntity.sentenceUuid,
          sentenceEntity.charge.chargeUuid,
          courtAppearance.appearanceUuid,
          courtAppearance.courtCase.caseUniqueIdentifier,
        )
      }
  }
}
