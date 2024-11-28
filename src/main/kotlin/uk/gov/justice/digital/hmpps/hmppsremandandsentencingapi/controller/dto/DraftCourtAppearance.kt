package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto

import com.fasterxml.jackson.databind.JsonNode
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.DraftAppearanceEntity
import java.util.UUID

data class DraftCourtAppearance(
  val draftUuid: UUID,
  val sessionBlob: JsonNode,
) {
  companion object {
    fun from(draftAppearanceEntity: DraftAppearanceEntity): DraftCourtAppearance {
      return DraftCourtAppearance(draftAppearanceEntity.draftUuid, draftAppearanceEntity.sessionBlob)
    }
  }
}
