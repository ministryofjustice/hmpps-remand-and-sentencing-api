package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.legacy.util

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.integration.legacy.util.DataCreator
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.PeriodLengthType

class PeriodLengthTypeMapperTests {

  @Test
  fun `when life sentence flag is true period length type is TARIFF_LENGTH`() {
    val periodLengthLegacyData = DataCreator.periodLengthLegacyData(lifeSentence = true)
    val result = PeriodLengthTypeMapper.convert(periodLengthLegacyData, "ALP")
    Assertions.assertThat(result).isEqualTo(PeriodLengthType.TARIFF_LENGTH)
  }

  @Test
  fun `when life sentence is false, sentence term code is DET and sentence cal is civil then type is TERM LENGTH`() {
    val periodLengthLegacyData = DataCreator.periodLengthLegacyData(lifeSentence = false, sentenceTermCode = "DET")
    val result = PeriodLengthTypeMapper.convert(periodLengthLegacyData, "CIVIL")
    Assertions.assertThat(result).isEqualTo(PeriodLengthType.TERM_LENGTH)
  }

  @Test
  fun `when life sentence is false, sentence term code is DET and sentence cal is not civil then type is SENTENCE LENGTH`() {
    val periodLengthLegacyData = DataCreator.periodLengthLegacyData(lifeSentence = false, sentenceTermCode = "DET")
    val result = PeriodLengthTypeMapper.convert(periodLengthLegacyData, "DPP")
    Assertions.assertThat(result).isEqualTo(PeriodLengthType.SENTENCE_LENGTH)
  }
}
