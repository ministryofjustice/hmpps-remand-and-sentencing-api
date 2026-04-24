package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.reconciliation

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.PeriodLengthEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.PeriodLengthType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.PeriodLengthLegacyData
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.util.PeriodLengthTypeMapper
import java.util.UUID

data class ReconciliationPeriodLength(
  val periodLengthUuid: UUID,
  val periodYears: Int?,
  val periodMonths: Int?,
  val periodWeeks: Int?,
  val periodDays: Int?,
  val lifeSentence: Boolean?,
  val sentenceTermCode: String,
  val legacyData: PeriodLengthLegacyData?,
) {
  companion object {
    fun from(periodLengthEntity: PeriodLengthEntity): ReconciliationPeriodLength {
      val sentenceTypeClassification = periodLengthEntity.sentenceEntity!!.sentenceType?.classification
      val sentenceCalcType = periodLengthEntity.sentenceEntity!!.sentenceType?.nomisSentenceCalcType ?: periodLengthEntity.sentenceEntity!!.legacyData?.sentenceCalcType
      val (isLifeSentence, sentenceTermCode) = if (sentenceTypeClassification != null || periodLengthEntity.periodLengthType !== PeriodLengthType.UNSUPPORTED) {
        PeriodLengthTypeMapper.convertDpsToNomis(periodLengthEntity.periodLengthType, sentenceTypeClassification, periodLengthEntity.legacyData, sentenceCalcType)
      } else {
        periodLengthEntity.legacyData?.lifeSentence to periodLengthEntity.legacyData!!.sentenceTermCode!!
      }
      return ReconciliationPeriodLength(
        periodLengthEntity.periodLengthUuid,
        if (isLifeSentence != true) periodLengthEntity.years else null,
        if (isLifeSentence != true) periodLengthEntity.months else null,
        if (isLifeSentence != true) periodLengthEntity.weeks else null,
        if (isLifeSentence != true) periodLengthEntity.days else null,
        isLifeSentence,
        sentenceTermCode,
        periodLengthEntity.legacyData,
      )
    }
  }
}
