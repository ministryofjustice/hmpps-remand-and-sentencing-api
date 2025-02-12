package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.SentenceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.PeriodLengthType
import java.math.BigDecimal
import java.util.UUID

data class LegacySentence(
  val prisonerId: String,
  val courtCaseId: String,
  val chargeLifetimeUuid: UUID,
  val lifetimeUuid: UUID,
  val active: Boolean,
  val sentenceCalcType: String,
  val sentenceCategory: String,
  val consecutiveToLifetimeUuid: UUID?,
  val chargeNumber: String?,
  val fineAmount: BigDecimal?,
  val periodLengths: List<LegacyPeriodLength>,
) {
  companion object {
    fun from(sentenceEntity: SentenceEntity): LegacySentence {
      val courtCase = sentenceEntity.charge.courtAppearances.filter { it.statusId == EntityStatus.ACTIVE }.maxBy { it.appearanceDate }.courtCase
      return LegacySentence(
        courtCase.prisonerId,
        courtCase.caseUniqueIdentifier,
        sentenceEntity.charge.lifetimeChargeUuid,
        sentenceEntity.lifetimeSentenceUuid,
        sentenceEntity.statusId == EntityStatus.ACTIVE,
        sentenceEntity.sentenceType?.nomisSentenceCalcType ?: sentenceEntity.legacyData!!.sentenceCalcType!!,
        sentenceEntity.sentenceType?.nomisCjaCode ?: sentenceEntity.legacyData!!.sentenceCategory!!,
        sentenceEntity.consecutiveTo?.lifetimeSentenceUuid,
        sentenceEntity.chargeNumber,
        sentenceEntity.fineAmountEntity?.fineAmount,
        sentenceEntity.periodLengths.filter { it.periodLengthType != PeriodLengthType.OVERALL_SENTENCE_LENGTH }.map { LegacyPeriodLength.from(it, sentenceEntity.sentenceType?.classification) },
      )
    }
  }
}
