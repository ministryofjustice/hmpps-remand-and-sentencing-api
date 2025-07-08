package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.util

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.PeriodLengthType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.SentenceTypeClassification
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.PeriodLengthLegacyData

class PeriodLengthTypeMapper {
  companion object {

    val civilSentenceCalcTypes: Set<String> = setOf("CIVIL", "CIVILLT")

    val extendedSentenceCalcTypes: Set<String> = setOf("EDS18", "EDS21", "EDSU18", "EPP", "LASPO_AR", "LASPO_DR", "STS18", "STS21")
    val recallExtendedSentenceCalcTypes: Set<String> = setOf("LR_EDS18", "LR_EDS21", "LR_EDSU18", "LR_LASPO_AR", "LR_LASPO_DR")

    const val NOMIS_DETENTION_TERM_CODE: String = "DET"
    const val NOMIS_IMPRISONMENT_TERM_CODE: String = "IMP"
    const val NOMIS_LICENCE_TERM_CODE: String = "LIC"
    const val NOMIS_SUPERVISION_TERM_CODE: String = "SUP"
    val supportedNomisTermCodes: Set<String> = setOf(NOMIS_DETENTION_TERM_CODE, NOMIS_IMPRISONMENT_TERM_CODE, NOMIS_SUPERVISION_TERM_CODE)

    fun convertNomisToDps(periodLengthLegacyData: PeriodLengthLegacyData, sentenceCalcType: String): PeriodLengthType {
      val periodLengthType = when {
        periodLengthLegacyData.lifeSentence == true -> PeriodLengthType.TARIFF_LENGTH
        periodLengthLegacyData.sentenceTermCode == NOMIS_DETENTION_TERM_CODE && civilSentenceCalcTypes.contains(sentenceCalcType) -> PeriodLengthType.TERM_LENGTH
        periodLengthLegacyData.sentenceTermCode == NOMIS_IMPRISONMENT_TERM_CODE && (extendedSentenceCalcTypes + recallExtendedSentenceCalcTypes).contains(sentenceCalcType) -> PeriodLengthType.CUSTODIAL_TERM
        periodLengthLegacyData.sentenceTermCode == NOMIS_LICENCE_TERM_CODE -> PeriodLengthType.LICENCE_PERIOD
        supportedNomisTermCodes.contains(periodLengthLegacyData.sentenceTermCode) -> PeriodLengthType.SENTENCE_LENGTH
        else -> PeriodLengthType.UNSUPPORTED
      }

      return periodLengthType
    }

    fun convertDpsToNomis(periodLengthType: PeriodLengthType, sentenceTypeClassification: SentenceTypeClassification?, periodLengthLegacyData: PeriodLengthLegacyData?, sentenceCalcType: String?): Pair<Boolean, String> {
      var lifeSentenceSentenceTermCode = when {
        periodLengthType == PeriodLengthType.TARIFF_LENGTH -> true to NOMIS_IMPRISONMENT_TERM_CODE
        periodLengthType == PeriodLengthType.CUSTODIAL_TERM || periodLengthType == PeriodLengthType.SENTENCE_LENGTH -> false to NOMIS_IMPRISONMENT_TERM_CODE
        periodLengthType == PeriodLengthType.LICENCE_PERIOD -> false to NOMIS_LICENCE_TERM_CODE
        periodLengthType == PeriodLengthType.TERM_LENGTH && (sentenceTypeClassification == SentenceTypeClassification.CIVIL || civilSentenceCalcTypes.contains(sentenceCalcType)) -> false to NOMIS_DETENTION_TERM_CODE
        periodLengthType == PeriodLengthType.TERM_LENGTH -> false to NOMIS_IMPRISONMENT_TERM_CODE
        else -> periodLengthLegacyData!!.lifeSentence!! to periodLengthLegacyData.sentenceTermCode!!
      }
      return lifeSentenceSentenceTermCode
    }
  }
}
