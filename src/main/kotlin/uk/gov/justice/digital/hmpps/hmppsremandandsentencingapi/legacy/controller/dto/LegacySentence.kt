package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.SentenceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.PeriodLengthType
import java.math.BigDecimal
import java.time.LocalDate
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
  val sentenceStartDate: LocalDate,
) {
  companion object {
    fun from(sentenceEntity: SentenceEntity): LegacySentence {
      val activeAppearances = sentenceEntity.charge.courtAppearances.filter { it.statusId == EntityStatus.ACTIVE }
      val courtCase = activeAppearances.maxBy { it.appearanceDate }.courtCase
      val firstSentenceAppearanceDate = activeAppearances.filter { it.warrantType == "SENTENCING" }.minOf { it.appearanceDate }
      return LegacySentence(
        courtCase.prisonerId,
        courtCase.caseUniqueIdentifier,
        sentenceEntity.charge.chargeUuid,
        sentenceEntity.sentenceUuid,
        sentenceEntity.statusId == EntityStatus.ACTIVE,
        sentenceEntity.sentenceType?.nomisSentenceCalcType ?: sentenceEntity.legacyData!!.sentenceCalcType!!,
        sentenceEntity.sentenceType?.nomisCjaCode ?: sentenceEntity.legacyData!!.sentenceCategory!!,
        sentenceEntity.consecutiveTo?.sentenceUuid,
        sentenceEntity.chargeNumber,
        sentenceEntity.fineAmount,
        sentenceEntity.periodLengths.filter { it.periodLengthType != PeriodLengthType.OVERALL_SENTENCE_LENGTH }.map { LegacyPeriodLength.from(it, sentenceEntity.sentenceType?.classification) },
        firstSentenceAppearanceDate,
      )
    }
  }
}
