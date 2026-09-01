package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.recall

import java.time.LocalDate

object SentenceOrdering {
  fun compare(
    aCountNumber: String?,
    bCountNumber: String?,
    aLineNumber: String?,
    bLineNumber: String?,
    aOffenceStartDate: LocalDate?,
    bOffenceStartDate: LocalDate?,
  ): Int {
    val aCount = aCountNumber?.takeIf { it != "-1" }?.toIntOrNull()
    val bCount = bCountNumber?.takeIf { it != "-1" }?.toIntOrNull()

    if (aCount != null && bCount != null) {
      return aCount.compareTo(bCount)
    }

    if (aCount != null) return -1
    if (bCount != null) return 1

    val aLine = aLineNumber?.toIntOrNull()
    val bLine = bLineNumber?.toIntOrNull()

    if (aLine != null && bLine != null) {
      return aLine.compareTo(bLine)
    }

    if (aLine != null) return -1
    if (bLine != null) return 1

    val aDate = aOffenceStartDate ?: LocalDate.MAX
    val bDate = bOffenceStartDate ?: LocalDate.MAX

    return aDate.compareTo(bDate)
  }
}
