package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.DraftAppearanceEntity
import java.util.UUID

data class DraftCourtAppearanceCreatedResponse(
  val draftUuid: UUID,
) {
  companion object {
    fun from(draftAppearanceEntity: DraftAppearanceEntity): DraftCourtAppearanceCreatedResponse = DraftCourtAppearanceCreatedResponse(draftAppearanceEntity.draftUuid)
  }
}
