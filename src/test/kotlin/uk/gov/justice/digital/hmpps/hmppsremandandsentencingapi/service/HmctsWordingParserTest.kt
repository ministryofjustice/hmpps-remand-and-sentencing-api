package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

class HmctsWordingParserTest {

  @ParameterizedTest
  @MethodSource("offenceCodeCases")
  fun `should extract offence code from wording`(
    wording: String,
    expectedOffenceCode: String,
  ) {
    assertThat(
      HmctsWordingParser(wording).parseSuspendedSentenceOffenceCode(),
    ).isEqualTo(expectedOffenceCode)
  }

  @ParameterizedTest
  @MethodSource("invalidCases")
  fun `should return null when offence code cannot be found`(wording: String) {
    assertThat(
      HmctsWordingParser(wording).parseSuspendedSentenceOffenceCode(),
    ).isNull()
  }

  companion object {
    @JvmStatic
    fun offenceCodeCases(): Stream<Arguments> = Stream.of(
      Arguments.of(
        "Activation of a suspended sentence order. Original CaseURN: 46XY1096323, Original code : MD71530, Original details: On 02/02/2023",
        "MD71530",
      ),
      Arguments.of(
        "Activation of a suspended sentence order. Original CaseURN: 47CC0742326, Original code : TH68010,",
        "TH68010",
      ),
      Arguments.of(
        "Activation of a suspended sentence order. Original code : OF61102,",
        "OF61102",
      ),
    )

    @JvmStatic
    fun invalidCases(): Stream<String> = Stream.of(
      "",
      "Activation of a suspended sentence order.",
      "Original code : INVALID",
      "Original Code : MD71530",
    )
  }
}
