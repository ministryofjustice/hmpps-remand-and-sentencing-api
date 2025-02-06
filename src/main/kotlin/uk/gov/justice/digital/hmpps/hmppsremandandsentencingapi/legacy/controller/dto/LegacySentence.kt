package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.SentenceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.PeriodLengthType
import java.math.BigDecimal
import java.util.UUID

data class LegacySentence(
  val prisonerId: String,
  val chargeLifetimeUuid: UUID,
  val lifetimeUuid: UUID,
  val sentenceCalcType: String?,
  val sentenceCategory: String?,
  val consecutiveToLifetimeUuid: UUID?,
  val chargeNumber: String?,
  val fineAmount: BigDecimal?,
  val legacyData: SentenceLegacyData?,
  val periodLengths: List<LegacyPeriodLength>,
) {
  companion object {
    fun from(sentenceEntity: SentenceEntity): LegacySentence = LegacySentence(
      sentenceEntity.charge.courtAppearances.filter { it.statusId == EntityStatus.ACTIVE }.maxBy { it.appearanceDate }.courtCase.prisonerId,
      sentenceEntity.charge.lifetimeChargeUuid,
      sentenceEntity.lifetimeSentenceUuid!!,
      sentenceEntity.legacyData?.sentenceCalcType ?: sentenceEntity.sentenceType?.nomisSentenceCalcType,
      sentenceEntity.legacyData?.sentenceCategory ?: sentenceEntity.sentenceType?.nomisCjaCode,
      sentenceEntity.consecutiveTo?.lifetimeSentenceUuid,
      sentenceEntity.chargeNumber,
      sentenceEntity.fineAmountEntity?.fineAmount,
      sentenceEntity.legacyData,
      sentenceEntity.periodLengths.filter { it.periodLengthType != PeriodLengthType.OVERALL_SENTENCE_LENGTH }.map { LegacyPeriodLength.from(it, sentenceEntity.sentenceType?.classification) },
    )
  }
}
