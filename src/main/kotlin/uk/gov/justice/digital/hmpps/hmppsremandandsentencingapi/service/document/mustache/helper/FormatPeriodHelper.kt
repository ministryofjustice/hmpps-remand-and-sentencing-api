package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service.document.mustache.helper

import com.github.jknack.handlebars.Helper;
import com.github.jknack.handlebars.Options
import java.time.Period

class FormatPeriodHelper : Helper<Period> {

  fun labelPeriod(length: Int, phraseSingular: String, phrasePlural: String, hideIfZero: Boolean = false): String {
    if (hideIfZero && length == 0) {
      return ""
    } else if (length == 1) {
      return "$length $phraseSingular"
    }
    return " $length $phrasePlural"
  }

  override fun apply(value: Period?, options: Options?): String? {
    return value?.let {
      val days = value.days % 7
      val weeks = value.days / 7
      val months = value.months
      val years = value.years

      buildString {
        append(labelPeriod(years, "Year", "Years"))
        append(" ")
        append(labelPeriod(months, "Month", "Months"))
        append(" ")
        append(labelPeriod(weeks, "Week", "Weeks", true))
        append(" ")
        append(labelPeriod(days, "Day", "Days"))
      }.trim()
    }
  }

}