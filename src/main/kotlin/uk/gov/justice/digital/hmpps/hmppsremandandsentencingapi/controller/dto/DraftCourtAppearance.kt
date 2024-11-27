package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto

import com.fasterxml.jackson.databind.JsonNode
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.DraftAppearanceEntity

data class DraftCourtAppearance(
  val sessionBlob: JsonNode,
) {
  companion object {
    fun from(draftAppearanceEntity: DraftAppearanceEntity): DraftCourtAppearance {
      return DraftCourtAppearance(draftAppearanceEntity.sessionBlob)
    }
  }
}
