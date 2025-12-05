package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.ImmigrationDetentionEntity
import java.util.UUID

data class DeleteImmigrationDetentionResponse(
  val immigrationDetentionUuid: UUID,
) {
  companion object {
    fun from(
      immigrationDetentionEntity: ImmigrationDetentionEntity,
    ): DeleteImmigrationDetentionResponse = DeleteImmigrationDetentionResponse(immigrationDetentionUuid = immigrationDetentionEntity.immigrationDetentionUuid)
  }
}
