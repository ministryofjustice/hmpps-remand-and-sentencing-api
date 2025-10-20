package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.reconciliation

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.SentenceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.PeriodLengthEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.SentenceEntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.LegacySentence.Companion.getSentenceCalcTypeAndCategory
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.SentenceLegacyData
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.util.SentenceUtils
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

data class ReconciliationSentence(
  val sentenceUuid: UUID,
  val fineAmount: BigDecimal?,
  val sentenceCalcType: String?,
  val sentenceCategory: String?,
  val active: Boolean,
  val sentenceStartDate: LocalDate,
  val legacyData: SentenceLegacyData?,
  val consecutiveToSentenceUuid: UUID?,
  val periodLengths: List<ReconciliationPeriodLength>,
) {
  companion object {
    fun from(sentenceEntity: SentenceEntity): ReconciliationSentence {
      val latestRecall = sentenceEntity.latestRecall()
      val sentenceTypeAndCategory = getSentenceCalcTypeAndCategory(sentenceEntity, latestRecall)
      val sentenceStartDate = SentenceUtils.calculateSentenceStartDate(sentenceEntity)
      return ReconciliationSentence(
        sentenceEntity.sentenceUuid,
        sentenceEntity.fineAmount,
        sentenceTypeAndCategory.first,
        sentenceTypeAndCategory.second,
        if (sentenceEntity.legacyData?.active != null) sentenceEntity.legacyData!!.active!! else sentenceEntity.statusId == SentenceEntityStatus.ACTIVE,
        sentenceStartDate,
        sentenceEntity.legacyData,
        sentenceEntity.consecutiveTo?.sentenceUuid,
        sentenceEntity.periodLengths.filter { it.statusId != PeriodLengthEntityStatus.DELETED }.map { ReconciliationPeriodLength.from(it) },
      )
    }
  }
}
