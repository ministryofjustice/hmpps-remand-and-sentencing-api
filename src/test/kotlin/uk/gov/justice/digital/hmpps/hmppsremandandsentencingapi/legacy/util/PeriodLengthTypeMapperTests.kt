package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.util

import org.assertj.core.api.Assertions
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.util.DataCreator
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.PeriodLengthType
import java.util.stream.Stream

class PeriodLengthTypeMapperTests {

  @ParameterizedTest(name = "when life sentence is {0}, sentence term code is {1} and sentence calc is {2} then type is {3}")
  @MethodSource("periodLengthTypeParameters")
  fun `period length type mapper tests`(lifeSentence: Boolean, sentenceTermCode: String, sentenceCalc: String, expectedType: PeriodLengthType) {
    val periodLengthLegacyData = DataCreator.periodLengthLegacyData(lifeSentence, sentenceTermCode)
    val result = PeriodLengthTypeMapper.convert(periodLengthLegacyData, sentenceCalc)
    Assertions.assertThat(result).isEqualTo(expectedType)
  }

  companion object {
    @JvmStatic
    fun periodLengthTypeParameters(): Stream<Arguments> {
      return Stream.of(
        Arguments.of(true, "IMP", "ALP", PeriodLengthType.TARIFF_LENGTH),
        Arguments.of(false, "DET", "CIVIL", PeriodLengthType.TERM_LENGTH),
        Arguments.of(false, "DET", "DPP", PeriodLengthType.SENTENCE_LENGTH),
        Arguments.of(false, "SUP", "VOO", PeriodLengthType.SENTENCE_LENGTH),
        Arguments.of(false, "IMP", "EDS18", PeriodLengthType.CUSTODIAL_TERM),
        Arguments.of(false, "IMP", "FTR_ORA", PeriodLengthType.SENTENCE_LENGTH),
        Arguments.of(false, "LIC", "EDS18", PeriodLengthType.LICENCE_PERIOD),
      )
    }
  }
}
