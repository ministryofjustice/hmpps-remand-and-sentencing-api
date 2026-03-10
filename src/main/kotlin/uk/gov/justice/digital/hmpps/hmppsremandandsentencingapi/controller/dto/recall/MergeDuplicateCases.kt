package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.recall

import java.time.LocalDate

data class DuplicateSentenceKey(
  val courtCode: String,
  val offenceCode: String?,
  val offenceStartDate: LocalDate?,
  val sentenceDate: LocalDate?, // The Sentence Date is otherwise known as the Appearance Date or the Warrant Date
)

data class SentenceWithCaseUuid(
  val caseUuid: String,
  val sentence: RecallableCourtCaseSentence,
)

// Groups court cases that should be merged
class CourtCaseMergedGroups(caseUuids: Collection<String>) {
  private val parentByCaseUuid = caseUuids.associateWith { it }.toMutableMap()

  // Finds the representative case for a given case
  fun findRepresentativeCase(caseUuid: String): String {
    var rootCaseUuid = caseUuid
    while (parentByCaseUuid[rootCaseUuid] != rootCaseUuid) {
      rootCaseUuid = parentByCaseUuid[rootCaseUuid]!!
    }

    var currentCase = caseUuid
    while (parentByCaseUuid[currentCase] != currentCase) {
      val parent = parentByCaseUuid[currentCase]!!
      parentByCaseUuid[currentCase] = rootCaseUuid
      currentCase = parent
    }

    return rootCaseUuid
  }

  // Merge two court cases into the same group
  fun mergeCasesIntoGroup(caseA: String, caseB: String) {
    val representativeA = findRepresentativeCase(caseA)
    val representativeB = findRepresentativeCase(caseB)

    if (representativeA != representativeB) {
      parentByCaseUuid[representativeB] = representativeA
    }
  }
}
