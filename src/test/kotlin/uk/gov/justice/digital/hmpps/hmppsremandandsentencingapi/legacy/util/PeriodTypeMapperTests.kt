package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.util

import org.assertj.core.api.Assertions
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.util.DataCreator
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.PeriodLengthType
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.SentenceTypeClassification
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.controller.dto.PeriodLengthLegacyData
import java.util.stream.Stream

class PeriodTypeMapperTests {

  @ParameterizedTest(name = "NOMIS to DPS when life sentence is {0}, sentence term code is {1} and sentence calc is {2} then type is {3}")
  @MethodSource("nomisToDpsPeriodLengthTypeParameters")
  fun `period length type mapper tests`(lifeSentence: Boolean, sentenceTermCode: String, sentenceCalc: String, expectedType: PeriodLengthType) {
    val periodLengthLegacyData = DataCreator.periodLengthLegacyData(lifeSentence, sentenceTermCode)
    val result = PeriodLengthTypeMapper.convertNomisToDps(periodLengthLegacyData, sentenceCalc)
    Assertions.assertThat(result).isEqualTo(expectedType)
  }

  @ParameterizedTest(name = "DPS to NOMIS when period length type is {0} and sentence type classification is {1} then life sentence is {2} and sentence term code is {3}")
  @MethodSource("dpsToNomisPeriodLengthTypeParameters")
  fun `DPS to NOMIS period length type mapper tests`(periodLengthType: PeriodLengthType, sentenceTypeClassification: SentenceTypeClassification?, lifeSentence: Boolean, sentenceTermCode: String, periodLengthLegacyData: PeriodLengthLegacyData?) {
    val (lifeSentenceResult, sentenceTermCodeResult) = PeriodLengthTypeMapper.convertDpsToNomis(periodLengthType, sentenceTypeClassification, periodLengthLegacyData)
    Assertions.assertThat(lifeSentenceResult).isEqualTo(lifeSentence)
    Assertions.assertThat(sentenceTermCodeResult).isEqualTo(sentenceTermCode)
  }

  companion object {
    @JvmStatic
    fun nomisToDpsPeriodLengthTypeParameters(): Stream<Arguments> = Stream.of(
      Arguments.of(true, "IMP", "ALP", PeriodLengthType.TARIFF_LENGTH),
      Arguments.of(false, "DET", "CIVIL", PeriodLengthType.TERM_LENGTH),
      Arguments.of(false, "DET", "DPP", PeriodLengthType.SENTENCE_LENGTH),
      Arguments.of(false, "SUP", "VOO", PeriodLengthType.SENTENCE_LENGTH),
      Arguments.of(false, "IMP", "EDS18", PeriodLengthType.CUSTODIAL_TERM),
      Arguments.of(false, "IMP", "FTR_ORA", PeriodLengthType.SENTENCE_LENGTH),
      Arguments.of(false, "LIC", "EDS18", PeriodLengthType.LICENCE_PERIOD),
      Arguments.of(false, "SEC105", "SEC105", PeriodLengthType.UNSUPPORTED),
    )

    @JvmStatic
    fun dpsToNomisPeriodLengthTypeParameters(): Stream<Arguments> = Stream.of(
      Arguments.of(PeriodLengthType.TARIFF_LENGTH, SentenceTypeClassification.INDETERMINATE, true, "IMP", null),
      Arguments.of(PeriodLengthType.CUSTODIAL_TERM, SentenceTypeClassification.EXTENDED, false, "IMP", null),
      Arguments.of(PeriodLengthType.LICENCE_PERIOD, SentenceTypeClassification.EXTENDED, false, "LIC", null),
      Arguments.of(PeriodLengthType.SENTENCE_LENGTH, SentenceTypeClassification.STANDARD, false, "IMP", null),
      Arguments.of(PeriodLengthType.TERM_LENGTH, SentenceTypeClassification.DTO, false, "IMP", null),
      Arguments.of(PeriodLengthType.TERM_LENGTH, SentenceTypeClassification.CIVIL, false, "DET", null),
      Arguments.of(PeriodLengthType.UNSUPPORTED, null, false, "SEC105", DataCreator.periodLengthLegacyData(lifeSentence = false, sentenceTermCode = "SEC105")),
    )
  }
}
