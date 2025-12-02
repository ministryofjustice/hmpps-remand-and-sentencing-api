package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto

import java.util.UUID

data class IsRecallPossibleResponse(
  val isRecallPossible: IsRecallPossible,
  val sentenceIds: List<UUID>,
)

enum class IsRecallPossible {
  YES,
  UNKNOWN_PRE_RECALL_MAPPING,
  RECALL_TYPE_AND_SENTENCE_MAPPING_NOT_POSSIBLE,
}
