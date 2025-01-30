package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.util

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.PeriodLengthType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.SentenceTypeClassification
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.PeriodLengthLegacyData

class PeriodLengthTypeMapper {
  companion object {

    val civilSentenceCalcTypes: Set<String> = setOf("CIVIL", "CIVILLT")

    val extendedSentenceCalcTypes: Set<String> = setOf("EDS18", "EDS21", "EDSU18", "EPP", "LASPO_AR", "LASPO_DR", "STS18", "STS21")

    fun convertNomisToDps(periodLengthLegacyData: PeriodLengthLegacyData, sentenceCalcType: String): PeriodLengthType {
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

    fun convertDpsToNomis(periodLengthType: PeriodLengthType, sentenceTypeClassification: SentenceTypeClassification): Pair<Boolean, String> {
      var lifeSentenceSentenceTermCode = when {
        periodLengthType == PeriodLengthType.TARIFF_LENGTH -> true to "IMP"
        periodLengthType == PeriodLengthType.CUSTODIAL_TERM || periodLengthType == PeriodLengthType.SENTENCE_LENGTH -> false to "IMP"
        periodLengthType == PeriodLengthType.LICENCE_PERIOD -> false to "LIC"
        periodLengthType == PeriodLengthType.TERM_LENGTH && sentenceTypeClassification == SentenceTypeClassification.DTO -> false to "IMP"
        periodLengthType == PeriodLengthType.TERM_LENGTH && sentenceTypeClassification == SentenceTypeClassification.CIVIL -> false to "DET"
        else -> false to ""
      }
      return lifeSentenceSentenceTermCode
    }
  }
}
