package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.util

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.PeriodLengthType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.PeriodLengthLegacyData

class PeriodLengthTypeMapper {
  companion object {

    val civilSentenceCalcTypes: Set<String> = setOf("CIVIL", "CIVILLT")

    val extendedSentenceCalcTypes: Set<String> = setOf("EDS18", "EDS21", "EDSU18", "EPP", "LASPO_AR", "LASPO_DR", "STS18", "STS21")

    fun convert(periodLengthLegacyData: PeriodLengthLegacyData, sentenceCalcType: String): PeriodLengthType {
      var periodLengthType: PeriodLengthType = PeriodLengthType.SENTENCE_LENGTH
      if (periodLengthLegacyData.lifeSentence == true) {
        periodLengthType = PeriodLengthType.TARIFF_LENGTH
      } else if (periodLengthLegacyData.sentenceTermCode == "DET" && civilSentenceCalcTypes.contains(sentenceCalcType)) {
        periodLengthType = PeriodLengthType.TERM_LENGTH
      } else if (periodLengthLegacyData.sentenceTermCode == "IMP" && extendedSentenceCalcTypes.contains(sentenceCalcType)) {
        periodLengthType = PeriodLengthType.CUSTODIAL_TERM
      } else if (periodLengthLegacyData.sentenceTermCode == "LIC") {
        periodLengthType = PeriodLengthType.LICENCE_PERIOD
      }

      return periodLengthType
    }
  }
}
