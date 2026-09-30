package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.document.mustache.helper

import com.github.jknack.handlebars.Helper
import com.github.jknack.handlebars.Options
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.document.dto.PeriodLength

class FormatPeriodHelper : Helper<PeriodLength> {

  fun labelPeriod(length: Int, phraseSingular: String, phrasePlural: String, hideIfZero: Boolean = false): String {
    if (hideIfZero && length == 0) {
      return ""
    } else if (length == 1) {
      return "$length $phraseSingular"
    }
    return "$length $phrasePlural"
  }

  override fun apply(value: PeriodLength?, options: Options?): String? = value?.let {
    val labelsMap = mapOf(
      "years" to labelPeriod(value.years, "Year", "Years"),
      "months" to labelPeriod(value.months, "Month", "Months"),
      "weeks" to labelPeriod(value.weeks, "Week", "Weeks", true),
      "days" to labelPeriod(value.days, "Day", "Days"),
    )

    value.periodOrder
      .split(",")
      .map { p -> labelsMap[p.trim()].orEmpty() }
      .filter { it.isNotBlank() }
      .joinToString(" ")
  }
}
