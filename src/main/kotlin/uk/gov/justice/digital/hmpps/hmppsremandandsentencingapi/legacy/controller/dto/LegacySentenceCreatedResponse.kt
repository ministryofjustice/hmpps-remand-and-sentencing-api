package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.SentenceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.CourtAppearanceEntityStatus
import java.util.UUID

data class LegacySentenceCreatedResponse(
  val prisonerId: String,
  val lifetimeUuid: UUID,
  val chargeLifetimeUuid: UUID,
  val appearanceUuid: UUID,
  val courtCaseId: String,
) {
  companion object {
    fun from(sentenceEntity: SentenceEntity): LegacySentenceCreatedResponse {
      val courtAppearance = sentenceEntity.charge.appearanceCharges
        .map { it.appearance!! }
        .filter { it.statusId != CourtAppearanceEntityStatus.DELETED }
        .maxByOrNull { it.appearanceDate }
        ?: throw IllegalStateException("No active court appearance found for charge ${sentenceEntity.charge.chargeUuid}")
      return LegacySentenceCreatedResponse(
        courtAppearance.courtCase.prisonerId,
        sentenceEntity.sentenceUuid,
        sentenceEntity.charge.chargeUuid,
        courtAppearance.appearanceUuid,
        courtAppearance.courtCase.caseUniqueIdentifier,
      )
    }
  }
}
