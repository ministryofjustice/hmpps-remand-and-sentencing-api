package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.PeriodLengthEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.SentenceTypeClassification
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.util.PeriodLengthTypeMapper

data class LegacyPeriodLength(
  val periodYears: Int?,
  val periodMonths: Int?,
  val periodWeeks: Int?,
  val periodDays: Int?,
  val isLifeSentence: Boolean?,
  val sentenceTermCode: String,
) {
  companion object {
    fun from(periodLengthEntity: PeriodLengthEntity, sentenceTypeClassification: SentenceTypeClassification?): LegacyPeriodLength {
      val (isLifeSentence, sentenceTermCode) = if (sentenceTypeClassification != null) {
        PeriodLengthTypeMapper.convertDpsToNomis(periodLengthEntity.periodLengthType, sentenceTypeClassification, periodLengthEntity.legacyData)
      } else {
        periodLengthEntity.legacyData?.lifeSentence to periodLengthEntity.legacyData!!.sentenceTermCode!!
      }
      return LegacyPeriodLength(
        periodLengthEntity.years,
        periodLengthEntity.months,
        periodLengthEntity.weeks,
        periodLengthEntity.days,
        isLifeSentence,
        sentenceTermCode,
      )
    }
  }
}
