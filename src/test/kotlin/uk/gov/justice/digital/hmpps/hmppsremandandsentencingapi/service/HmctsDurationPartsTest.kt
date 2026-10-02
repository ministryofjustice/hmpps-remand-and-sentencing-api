package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

import org.assertj.core.api.Assertions
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

class HmctsDurationPartsTest {

  @ParameterizedTest(name = "Tests durations are mapped from hmcts {0} to parts {1}")
  @MethodSource("durationExamples")
  fun `Test get offence outcome mappings`(hmctsDuration: String, parts: HmctsDurationParts) {
    val result = HmctsDurationParts.parseDuration(hmctsDuration)
    Assertions.assertThat(result).isEqualTo(parts)
  }

  companion object {

    @JvmStatic
    fun durationExamples(): Stream<Arguments> = Stream.of(
      Arguments.of(
        "5 Years 219 Days",
        HmctsDurationParts(years = 5, days = 219),
      ),
      Arguments.of(
        "5 Years 7 Months 10 Days",
        HmctsDurationParts(years = 5, months = 7, days = 10),
      ),
      Arguments.of(
        "9 Weeks",
        HmctsDurationParts(weeks = 9),
      ),
      Arguments.of(
        "1 Year",
        HmctsDurationParts(years = 1),
      ),
      Arguments.of(
        "1 Month",
        HmctsDurationParts(months = 1),
      ),
      Arguments.of(
        "1 Week",
        HmctsDurationParts(weeks = 1),
      ),
      Arguments.of(
        "1 Day",
        HmctsDurationParts(days = 1),
      ),
      Arguments.of(
        "2 Years 3 Months 4 Weeks 5 Days",
        HmctsDurationParts(
          years = 2,
          months = 3,
          weeks = 4,
          days = 5,
        ),
      ),
      Arguments.of(
        "3 Years 2 Months",
        HmctsDurationParts(years = 3, months = 2),
      ),
      Arguments.of(
        "6 Months 2 Weeks",
        HmctsDurationParts(months = 6, weeks = 2),
      ),
      Arguments.of(
        "4 Weeks 3 Days",
        HmctsDurationParts(weeks = 4, days = 3),
      ),
    )
  }
}
