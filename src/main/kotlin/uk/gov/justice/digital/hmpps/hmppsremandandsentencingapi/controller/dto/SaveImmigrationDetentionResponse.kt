package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.ImmigrationDetentionEntity
import java.util.UUID

data class SaveImmigrationDetentionResponse(
  val immigrationDetentionUuid: UUID,
) {
  companion object {
    fun from(
      immigrationDetentionEntity: ImmigrationDetentionEntity,
    ): SaveImmigrationDetentionResponse = SaveImmigrationDetentionResponse(immigrationDetentionUuid = immigrationDetentionEntity.immigrationDetentionUuid)
  }
}
