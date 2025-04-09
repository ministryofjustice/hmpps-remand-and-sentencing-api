package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.PeriodLengthEntity
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.SentenceTypeClassification
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.util.PeriodLengthTypeMapper
import java.util.UUID

data class LegacyPeriodLength(
  val periodYears: Int?,
  val periodMonths: Int?,
  val periodWeeks: Int?,
  val periodDays: Int?,
  val isLifeSentence: Boolean?,
  val sentenceTermCode: String,
  val periodLengthUuid: UUID,
) {
  companion object {
    fun from(periodLengthEntity: PeriodLengthEntity, sentenceTypeClassification: SentenceTypeClassification?): LegacyPeriodLength {
      val (isLifeSentence, sentenceTermCode) = if (sentenceTypeClassification != null) {
        PeriodLengthTypeMapper.convertDpsToNomis(periodLengthEntity.periodLengthType, sentenceTypeClassification, periodLengthEntity.legacyData)
      } else {
        periodLengthEntity.legacyData?.lifeSentence to periodLengthEntity.legacyData!!.sentenceTermCode!!
      }
      return LegacyPeriodLength(
        if (isLifeSentence != true) periodLengthEntity.years else null,
        if (isLifeSentence != true) periodLengthEntity.months else null,
        if (isLifeSentence != true) periodLengthEntity.weeks else null,
        if (isLifeSentence != true) periodLengthEntity.days else null,
        isLifeSentence,
        sentenceTermCode,
        periodLengthEntity.periodLengthUuid,
      )
    }
  }
}
