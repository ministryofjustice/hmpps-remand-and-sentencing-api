package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.RecallEntity
import java.util.UUID

data class SaveRecallResponse(
  val recallUuid: UUID,
) {
  companion object {
    fun from(recall: RecallEntity): SaveRecallResponse =
      SaveRecallResponse(recallUuid = recall.recallUniqueIdentifier)
  }
}
