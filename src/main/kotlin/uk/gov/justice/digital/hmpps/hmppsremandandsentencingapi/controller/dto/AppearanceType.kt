package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.AppearanceTypeEntity
import java.util.UUID

data class AppearanceType(
  val appearanceTypeUuid: UUID,
  val description: String,
  val displayOrder: Int,
) {
  companion object {
    fun from(appearanceTypeEntity: AppearanceTypeEntity): AppearanceType = AppearanceType(
      appearanceTypeEntity.appearanceTypeUuid,
      appearanceTypeEntity.description,
      appearanceTypeEntity.displayOrder,
    )
  }
}
