package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.recall.Recall
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.event.EventSource
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.RecallType
import java.time.LocalDate

/**
 * NOMIS creates one recall per sentence. Group NOMIS recalls that share the same recall type
 * and arrest date, or if there is no arrest date, the same posted-date day.
 */
object NomisRecallGrouper {

  fun group(recalls: List<Recall>): List<Recall> {
    val (nomisRecalls, otherRecalls) = recalls.partition { it.source == EventSource.NOMIS }

    val groupedNomisRecalls = nomisRecalls
      .groupBy { groupingKey(it) }
      .flatMap { (key, recallsWithSameKey) ->
        if (key == null || recallsWithSameKey.size == 1) {
          recallsWithSameKey
        } else {
          listOf(merge(recallsWithSameKey))
        }
      }

    return otherRecalls + groupedNomisRecalls
  }

  fun groupingKey(recallType: RecallType, returnToCustodyDate: LocalDate?, postedDate: String?): String? {
    if (returnToCustodyDate != null) {
      return "$recallType|arrest:$returnToCustodyDate"
    }
    if (postedDate.isNullOrBlank()) return null
    // postedDate is a datetime string; first 10 chars are yyyy-MM-dd
    return "$recallType|posted:${postedDate.take(10)}"
  }

  private fun groupingKey(recall: Recall): String? = groupingKey(recall.recallType, recall.returnToCustodyDate, recall.postedDate)

  private fun merge(recallsWithSameKey: List<Recall>): Recall {
    val primary = recallsWithSameKey.minBy { it.createdAt }
    val courtCases = recallsWithSameKey
      .flatMap { it.courtCases }
      .groupBy { it.courtCaseUuid }
      .map { (_, cases) ->
        cases.first().copy(
          sentences = cases.flatMap { it.sentences }.distinctBy { it.sentenceUuid },
        )
      }
    return primary.copy(courtCases = courtCases)
  }
}
