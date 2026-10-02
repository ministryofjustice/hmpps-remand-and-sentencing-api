package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

data class HmctsDurationParts(
  val years: Int? = null,
  val months: Int? = null,
  val weeks: Int? = null,
  val days: Int? = null,
) {
  companion object {

    fun parseDuration(input: String): HmctsDurationParts {
      val regex = Regex("""(\d+)\s+(Years?|Months?|Weeks?|Days?)""")

      var years: Int? = null
      var months: Int? = null
      var weeks: Int? = null
      var days: Int? = null

      regex.findAll(input).forEach { match ->
        val value = match.groupValues[1].toInt()
        val unit = match.groupValues[2].lowercase()

        when {
          unit.startsWith("year") -> years = value
          unit.startsWith("month") -> months = value
          unit.startsWith("week") -> weeks = value
          unit.startsWith("day") -> days = value
        }
      }

      return HmctsDurationParts(years, months, weeks, days)
    }
  }
}
