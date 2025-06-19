package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto

data class CourtCaseCountNumbers(
  val countNumbers: Set<CourtCaseCountNumber>,
) {
  companion object {
    fun from(countNumbers: List<String?>): CourtCaseCountNumbers = CourtCaseCountNumbers(
      countNumbers.filter { it != null }.map {
        CourtCaseCountNumber(it!!)
      }.toSet(),
    )
  }
}
