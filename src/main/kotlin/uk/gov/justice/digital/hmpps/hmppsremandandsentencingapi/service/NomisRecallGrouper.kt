package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.service

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto.recall.Recall
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.domain.event.EventSource

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

  private fun groupingKey(recall: Recall): String? {
    if (recall.returnToCustodyDate != null) {
      return "${recall.recallType}|arrest:${recall.returnToCustodyDate}"
    }
    if (recall.postedDate.isNullOrBlank()) return null
    // postedDate is a datetime string; first 10 chars are yyyy-MM-dd
    return "${recall.recallType}|posted:${recall.postedDate.take(10)}"
  }

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
