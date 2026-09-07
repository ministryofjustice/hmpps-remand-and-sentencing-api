package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.recall

import java.time.LocalDate

object SentenceOrdering {
  fun compare(
    a: SentenceOrderingKey,
    b: SentenceOrderingKey,
  ): Int {
    val aCount = a.countNumber?.takeIf { it != "-1" }?.toIntOrNull()
    val bCount = b.countNumber?.takeIf { it != "-1" }?.toIntOrNull()

    if (aCount != null && bCount != null) {
      return aCount.compareTo(bCount)
    }

    if (aCount != null) return -1
    if (bCount != null) return 1

    val aLine = a.lineNumber?.toIntOrNull()
    val bLine = b.lineNumber?.toIntOrNull()

    if (aLine != null && bLine != null) {
      return aLine.compareTo(bLine)
    }

    if (aLine != null) return -1
    if (bLine != null) return 1

    val aDate = a.offenceStartDate ?: LocalDate.MAX
    val bDate = b.offenceStartDate ?: LocalDate.MAX

    return aDate.compareTo(bDate)
  }
}
