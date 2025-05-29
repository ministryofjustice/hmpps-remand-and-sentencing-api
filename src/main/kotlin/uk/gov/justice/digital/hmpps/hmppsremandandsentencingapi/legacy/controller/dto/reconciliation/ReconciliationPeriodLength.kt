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
      val (isLifeSentence, sentenceTermCode) = if (sentenceTypeClassification != null || periodLengthEntity.periodLengthType !== PeriodLengthType.UNSUPPORTED) {
        PeriodLengthTypeMapper.convertDpsToNomis(periodLengthEntity.periodLengthType, sentenceTypeClassification, periodLengthEntity.legacyData)
      } else {
        periodLengthEntity.legacyData?.lifeSentence to periodLengthEntity.legacyData!!.sentenceTermCode!!
      }
      return ReconciliationPeriodLength(
        periodLengthEntity.periodLengthUuid,
        periodLengthEntity.years,
        periodLengthEntity.months,
        periodLengthEntity.weeks,
        periodLengthEntity.days,
        isLifeSentence,
        sentenceTermCode,
        periodLengthEntity.legacyData,
      )
    }
  }
}
