package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.enum.RecallType
import java.util.UUID

data class IsRecallPossibleRequest(
  val sentenceIds: List<UUID>,
  val recallType: RecallType,
)
