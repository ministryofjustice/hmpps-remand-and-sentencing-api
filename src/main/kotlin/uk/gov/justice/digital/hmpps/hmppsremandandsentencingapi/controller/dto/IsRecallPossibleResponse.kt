package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto

import java.util.UUID

data class IsRecallPossibleResponse(
  val isRecallPossible: IsRecallPossible,
  val sentenceIds: List<UUID>,
)

enum class IsRecallPossible(val priority: Int) {
  YES(3),
  UNKNOWN_PRE_RECALL_MAPPING(2),
  RECALL_TYPE_AND_SENTENCE_MAPPING_NOT_POSSIBLE(1),
}
