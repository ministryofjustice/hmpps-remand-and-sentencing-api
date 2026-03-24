package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.controller.dto

import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.jpa.entity.CourtAppearanceSubtypeEntity
import java.util.UUID

data class CourtAppearanceSubtype(
  val appearanceSubtypeUuid: UUID,
  val description: String,
  val displayOrder: Int,
  val appearanceTypeUuid: UUID,
) {
  companion object {
    fun from(courtAppearanceSubtypeEntity: CourtAppearanceSubtypeEntity): CourtAppearanceSubtype = CourtAppearanceSubtype(
      courtAppearanceSubtypeEntity.appearanceSubtypeUuid,
      courtAppearanceSubtypeEntity.description,
      courtAppearanceSubtypeEntity.displayOrder,
      courtAppearanceSubtypeEntity.appearanceType.appearanceTypeUuid,
    )
  }
}
