package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto

import com.fasterxml.jackson.databind.ObjectMapper
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.SentenceEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.EntityStatus
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
    fun from(sentenceEntity: SentenceEntity, objectMapper: ObjectMapper): LegacySentence {
      val legacyData = sentenceEntity.legacyData?.let {
        objectMapper.treeToValue<SentenceLegacyData>(
          it,
          SentenceLegacyData::class.java,
        )
      }
      return LegacySentence(
        sentenceEntity.charge.courtAppearances.filter { it.statusId == EntityStatus.ACTIVE }.maxBy { it.appearanceDate }.courtCase.prisonerId,
        sentenceEntity.charge.lifetimeChargeUuid,
        sentenceEntity.lifetimeSentenceUuid!!,
        legacyData?.sentenceCalcType ?: sentenceEntity.sentenceType?.nomisSentenceCalcType,
        legacyData?.sentenceCategory ?: sentenceEntity.sentenceType?.nomisCjaCode,
        sentenceEntity.consecutiveTo?.lifetimeSentenceUuid,
        sentenceEntity.chargeNumber,
        sentenceEntity.fineAmountEntity?.fineAmount,
        legacyData,
      )
    }
  }
}
