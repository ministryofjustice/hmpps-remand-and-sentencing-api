package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.reconciliation

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.SentenceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityStatus
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.SentenceLegacyData
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
      val firstSentenceAppearanceDate = sentenceEntity.charge.appearanceCharges
        .map { it.appearance!! }
        .filter { it.statusId == EntityStatus.ACTIVE && it.warrantType == "SENTENCING" }
        .minOf { it.appearanceDate }
      return ReconciliationSentence(
        sentenceEntity.sentenceUuid,
        sentenceEntity.fineAmount,
        sentenceEntity.legacyData?.sentenceCalcType ?: sentenceEntity.sentenceType?.nomisSentenceCalcType,
        sentenceEntity.legacyData?.sentenceCategory ?: sentenceEntity.sentenceType?.nomisCjaCode,
        if (sentenceEntity.legacyData?.active != null) sentenceEntity.legacyData!!.active!! else sentenceEntity.statusId == EntityStatus.ACTIVE,
        firstSentenceAppearanceDate,
        sentenceEntity.legacyData,
        sentenceEntity.consecutiveTo?.sentenceUuid,
        sentenceEntity.periodLengths.filter { it.statusId != EntityStatus.DELETED }.map { ReconciliationPeriodLength.from(it) },
      )
    }
  }
}
