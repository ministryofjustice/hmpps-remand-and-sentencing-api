package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.util

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.PeriodLengthType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.PeriodLengthLegacyData

class PeriodLengthTypeMapper {
  companion object {

    val civilSentenceCalcTypes: Set<String> = setOf("CIVIL", "CIVILLT")

    fun convert(periodLengthLegacyData: PeriodLengthLegacyData, sentenceCalcType: String): PeriodLengthType {
      if (periodLengthLegacyData.lifeSentence == true) {
        return PeriodLengthType.TARIFF_LENGTH
      }
      if (periodLengthLegacyData.sentenceTermCode == "DET") {
        if (civilSentenceCalcTypes.contains(sentenceCalcType)) {
          return PeriodLengthType.TERM_LENGTH
        } else {
          return PeriodLengthType.SENTENCE_LENGTH
        }
      }

      return PeriodLengthType.CUSTODIAL_TERM
    }
  }
}
